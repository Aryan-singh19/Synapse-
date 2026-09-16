package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class SynthEngine {
    companion object {
        const val SAMPLE_RATE = 44100
        const val MAX_VOICES = 4
        const val BUFFER_CHUNK_SIZE = 512
        const val WAVEFORM_BUFFER_SIZE = 512
    }

    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    // WAV Audio Recorder for live studio capturing
    val recorder = WavAudioRecorder()

    // Real-time Performance Controls
    @Volatile
    var pitchBendSemitones: Float = 0f // -2.0 to +2.0

    @Volatile
    var modWheelAmount: Float = 0f // 0.0 to 1.0

    @Volatile
    var masterTuningHz: Float = 440f // 430Hz - 450Hz concert pitch calibration

    // Current synthesis patch parameters
    @Volatile
    var patch: SynthPatch = SynthPatch()
        set(value) {
            field = value
            updateInternalParams(value)
        }

    // Active voices
    private val voices = Array(MAX_VOICES) { Voice() }

    // Internal modulation & FX state
    private var lfoPhase = 0.0
    private var randomHoldVal = 0.0f
    private var randomHoldCounter = 0

    // Stereo Delay line buffers (ping-pong spatial delay)
    private val delayBufferSize = SAMPLE_RATE * 2 // 2 seconds max delay
    private val delayBufferL = FloatArray(delayBufferSize)
    private val delayBufferR = FloatArray(delayBufferSize)
    private var delayWriteIndex = 0

    // Stereo Chorus / Dimension ensemble buffers
    private val chorusBufferSize = 2048 // ~46ms max chorus delay
    private val chorusBufferL = FloatArray(chorusBufferSize)
    private val chorusBufferR = FloatArray(chorusBufferSize)
    private var chorusWriteIndex = 0
    private var chorusLfoPhase = 0.0

    // Algorithmic Stereo Reverb (Freeverb style comb + allpass matrix)
    private class CombFilter(val size: Int) {
        val buffer = FloatArray(size)
        var bufferIndex = 0
        var filterStore = 0.0f

        fun process(input: Float, damp: Float, feedback: Float): Float {
            val output = buffer[bufferIndex]
            filterStore = output * (1.0f - damp) + filterStore * damp
            buffer[bufferIndex] = input + filterStore * feedback
            bufferIndex = (bufferIndex + 1) % size
            return output
        }

        fun clear() {
            buffer.fill(0f)
            filterStore = 0f
        }
    }

    private class AllPassFilter(val size: Int) {
        val buffer = FloatArray(size)
        var bufferIndex = 0

        fun process(input: Float): Float {
            val bufOut = buffer[bufferIndex]
            val output = -input + bufOut
            buffer[bufferIndex] = input + (bufOut * 0.5f)
            bufferIndex = (bufferIndex + 1) % size
            return output
        }

        fun clear() {
            buffer.fill(0f)
        }
    }

    private val combsL = arrayOf(CombFilter(1116), CombFilter(1188), CombFilter(1277), CombFilter(1356))
    private val combsR = arrayOf(CombFilter(1139), CombFilter(1211), CombFilter(1300), CombFilter(1379))
    private val allpassesL = arrayOf(AllPassFilter(225), AllPassFilter(341))
    private val allpassesR = arrayOf(AllPassFilter(248), AllPassFilter(364))

    private fun processReverb(inL: Float, inR: Float, roomSize: Float, damp: Float): Pair<Float, Float> {
        val feedback = (roomSize * 0.28f + 0.7f).coerceIn(0.6f, 0.96f)
        val effDamp = damp.coerceIn(0.05f, 0.85f)
        val monoInput = (inL + inR) * 0.015f

        var outL = 0f
        var outR = 0f
        for (c in combsL) outL += c.process(monoInput, effDamp, feedback)
        for (c in combsR) outR += c.process(monoInput, effDamp, feedback)

        for (ap in allpassesL) outL = ap.process(outL)
        for (ap in allpassesR) outR = ap.process(outR)

        return Pair(outL * 1.5f, outR * 1.5f)
    }

    // Visualizer buffer (thread-safe copy for UI)
    private val scopeBufferInternal = FloatArray(WAVEFORM_BUFFER_SIZE)
    private val scopeBufferExport = FloatArray(WAVEFORM_BUFFER_SIZE)
    private var scopeWriteIndex = 0
    private val scopeLock = Any()

    @Volatile
    var currentPeakRms: Float = 0f
        private set

    // Cached modulation values calculated per audio block
    private var modLfoOut = 0.0f
    private var modAmpEnvAvg = 0.0f
    private var modFiltEnvAvg = 0.0f

    init {
        updateInternalParams(patch)
    }

    private fun updateInternalParams(p: SynthPatch) {
        // Param validation if needed
    }

    fun start() {
        if (isRunning.get()) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBufferSize * 2, BUFFER_CHUNK_SIZE * 4 * 2)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isRunning.set(true)

            audioThread = Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val stereoShortBuffer = ShortArray(BUFFER_CHUNK_SIZE * 2)
                val floatBufferL = FloatArray(BUFFER_CHUNK_SIZE)
                val floatBufferR = FloatArray(BUFFER_CHUNK_SIZE)

                while (isRunning.get()) {
                    generateStereoBlock(floatBufferL, floatBufferR)

                    var sumSquares = 0f
                    for (i in 0 until BUFFER_CHUNK_SIZE) {
                        val sampleL = floatBufferL[i]
                        val sampleR = floatBufferR[i]
                        sumSquares += (sampleL * sampleL + sampleR * sampleR) * 0.5f

                        val clampL = sampleL.coerceIn(-1.0f, 1.0f)
                        val clampR = sampleR.coerceIn(-1.0f, 1.0f)

                        stereoShortBuffer[i * 2] = (clampL * 32767.0f).toInt().toShort()
                        stereoShortBuffer[i * 2 + 1] = (clampR * 32767.0f).toInt().toShort()
                    }
                    currentPeakRms = sqrt(sumSquares / BUFFER_CHUNK_SIZE)

                    // Write to live WAV recorder if active (Stereo 16-bit 44.1kHz)
                    if (recorder.isRecording) {
                        recorder.writeSamples(stereoShortBuffer, BUFFER_CHUNK_SIZE * 2)
                    }

                    // Write interleaved stereo to AudioTrack
                    audioTrack?.write(stereoShortBuffer, 0, BUFFER_CHUNK_SIZE * 2)
                }
            }, "SynapseAudioEngineThread")

            audioThread?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning.set(false)
        }
    }

    fun stop() {
        if (recorder.isRecording) {
            recorder.stop()
        }
        isRunning.set(false)
        try {
            audioThread?.join(500)
        } catch (e: InterruptedException) {
            // Ignored
        }
        audioThread = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignored
        }
        audioTrack = null
    }

    /**
     * Note On with MIDI note number (0 - 127) and velocity (0.0 - 1.0)
     */
    fun noteOn(midiNote: Int, velocity: Float = 0.8f) {
        val targetFreq = midiToFreq(midiNote)

        // Find existing voice with same note, or oldest voice, or free voice
        var voice = voices.find { it.activeNote == midiNote && it.isActive }
        if (voice == null) {
            voice = voices.find { !it.isActive }
        }
        if (voice == null) {
            // Steal oldest voice
            voice = voices.minByOrNull { it.voiceAge } ?: voices[0]
        }

        voice.trigger(midiNote, targetFreq, velocity, patch)
    }

    /**
     * Note Off with MIDI note number
     */
    fun noteOff(midiNote: Int) {
        for (voice in voices) {
            if (voice.activeNote == midiNote && voice.isActive) {
                voice.release()
            }
        }
    }

    fun allNotesOff() {
        for (voice in voices) {
            voice.kill()
        }
        for (c in combsL) c.clear()
        for (c in combsR) c.clear()
        for (ap in allpassesL) ap.clear()
        for (ap in allpassesR) ap.clear()
    }

    fun getActiveVoiceCount(): Int {
        var count = 0
        for (i in 0 until MAX_VOICES) {
            if (voices[i].isActive) count++
        }
        return count
    }

    fun getVoiceActiveMask(): BooleanArray {
        val mask = BooleanArray(MAX_VOICES)
        for (i in 0 until MAX_VOICES) {
            mask[i] = voices[i].isActive
        }
        return mask
    }

    /**
     * Main DSP stereo block generation function (512 samples per channel)
     */
    private fun generateStereoBlock(outputL: FloatArray, outputR: FloatArray) {
        val p = patch
        val lfoInc = (2.0 * PI * p.lfoRate) / SAMPLE_RATE
        val delaySamples = (p.delayTime * SAMPLE_RATE).toInt().coerceIn(1, delayBufferSize - 1)
        val chorusLfoInc = (2.0 * PI * p.chorusRate) / SAMPLE_RATE

        // Calculate modulation from patch bay
        var pitchModOsc1 = 0f
        var pitchModOsc2 = 0f
        var cutoffMod = 0f
        var resMod = 0f
        var delayMod = 0f
        var driveMod = 0f
        var chorusMod = 0f
        var reverbMod = 0f

        var avgAmp = 0f
        var avgFilt = 0f
        var activeCount = 0

        for (v in voices) {
            if (v.isActive) {
                avgAmp += v.ampEnvLevel
                avgFilt += v.filtEnvLevel
                activeCount++
            }
        }
        if (activeCount > 0) {
            modAmpEnvAvg = avgAmp / activeCount
            modFiltEnvAvg = avgFilt / activeCount
        }

        for (cable in p.cables) {
            val sourceVal = when (cable.source) {
                PatchSource.LFO_OUT -> modLfoOut
                PatchSource.AMP_ENV -> modAmpEnvAvg
                PatchSource.FILTER_ENV -> modFiltEnvAvg
                PatchSource.RANDOM_SH -> randomHoldVal
                PatchSource.VELOCITY -> 0.8f
            }
            val routedVal = sourceVal * cable.amount
            when (cable.destination) {
                PatchDestination.FILTER_CUTOFF -> cutoffMod += routedVal * 4000f
                PatchDestination.FILTER_RES -> resMod += routedVal * 4.0f
                PatchDestination.OSC1_PITCH -> pitchModOsc1 += routedVal * 12f // semitones
                PatchDestination.OSC2_PITCH -> pitchModOsc2 += routedVal * 12f
                PatchDestination.DELAY_TIME -> delayMod += routedVal * 0.3f
                PatchDestination.DRIVE_GAIN -> driveMod += routedVal * 2.5f
                PatchDestination.CHORUS_MIX -> chorusMod += routedVal * 0.5f
                PatchDestination.REVERB_MIX -> reverbMod += routedVal * 0.5f
            }
        }

        val effChorusMix = (p.chorusMix + chorusMod).coerceIn(0f, 1f)
        val effReverbMix = (p.reverbMix + reverbMod).coerceIn(0f, 0.85f)

        for (i in 0 until BUFFER_CHUNK_SIZE) {
            // Update Main LFO
            lfoPhase += lfoInc
            if (lfoPhase >= 2.0 * PI) lfoPhase -= 2.0 * PI

            modLfoOut = when (p.lfoShape) {
                Waveform.SINE -> sin(lfoPhase).toFloat()
                Waveform.TRIANGLE -> (2.0 * abs(lfoPhase / PI - 1.0) - 1.0).toFloat()
                Waveform.SQUARE -> if (lfoPhase < PI) 1.0f else -1.0f
                Waveform.SAWTOOTH -> (lfoPhase / PI - 1.0).toFloat()
                Waveform.NOISE -> (Math.random() * 2.0 - 1.0).toFloat()
            } * p.lfoDepth

            // Random S&H update
            randomHoldCounter++
            if (randomHoldCounter >= (SAMPLE_RATE / (p.lfoRate * 2.0)).toInt().coerceAtLeast(10)) {
                randomHoldCounter = 0
                randomHoldVal = (Math.random() * 2.0 - 1.0).toFloat()
            }

            // Sum voices
            var mix = 0.0f
            for (voice in voices) {
                if (voice.isActive) {
                    mix += voice.renderSample(p, modLfoOut, pitchModOsc1, pitchModOsc2, cutoffMod, resMod)
                }
            }

            // Master drive saturation (analog hyperbolic tangent curve)
            val effectiveDrive = (p.driveGain + driveMod).coerceIn(1.0f, 8.0f)
            var driven = mix * effectiveDrive
            driven = driven / (1.0f + abs(driven))

            // Stereo Chorus / Ensemble effect
            chorusLfoPhase += chorusLfoInc
            if (chorusLfoPhase >= 2.0 * PI) chorusLfoPhase -= 2.0 * PI

            // Quadrature LFO for wide 90-degree stereo field
            val modOffsetL = (sin(chorusLfoPhase) * 350.0 * p.chorusDepth).toFloat()
            val modOffsetR = (cos(chorusLfoPhase) * 350.0 * p.chorusDepth).toFloat()

            // Write to circular chorus buffers
            chorusBufferL[chorusWriteIndex] = driven
            chorusBufferR[chorusWriteIndex] = driven

            val baseChorusDelay = 600 // ~13.6ms center delay
            val readChorusIdxL = (chorusWriteIndex - baseChorusDelay - modOffsetL.toInt()).let {
                val rem = it % chorusBufferSize
                if (rem < 0) rem + chorusBufferSize else rem
            }
            val readChorusIdxR = (chorusWriteIndex - baseChorusDelay - modOffsetR.toInt()).let {
                val rem = it % chorusBufferSize
                if (rem < 0) rem + chorusBufferSize else rem
            }

            val chorusSampleL = chorusBufferL[readChorusIdxL]
            val chorusSampleR = chorusBufferR[readChorusIdxR]
            chorusWriteIndex = (chorusWriteIndex + 1) % chorusBufferSize

            val chorusedL = driven * (1f - effChorusMix * 0.45f) + chorusSampleL * effChorusMix
            val chorusedR = driven * (1f - effChorusMix * 0.45f) + chorusSampleR * effChorusMix

            // Stereo Ping-Pong Delay
            val currentDelaySamples = (delaySamples + (delayMod * SAMPLE_RATE).toInt()).coerceIn(100, delayBufferSize - 1)
            var readIdxL = delayWriteIndex - currentDelaySamples
            if (readIdxL < 0) readIdxL += delayBufferSize

            // Right channel has slight 75% poly-meter delay offset for stereo width
            val rightDelaySamples = (currentDelaySamples * 0.75f).toInt().coerceIn(80, delayBufferSize - 1)
            var readIdxR = delayWriteIndex - rightDelaySamples
            if (readIdxR < 0) readIdxR += delayBufferSize

            val delayedL = delayBufferL[readIdxL]
            val delayedR = delayBufferR[readIdxR]

            // Cross-channel feedback ping-pong
            val feedbackL = (chorusedL + delayedR * p.delayFeedback).coerceIn(-1.5f, 1.5f)
            val feedbackR = (chorusedR + delayedL * p.delayFeedback).coerceIn(-1.5f, 1.5f)

            delayBufferL[delayWriteIndex] = feedbackL
            delayBufferR[delayWriteIndex] = feedbackR
            delayWriteIndex = (delayWriteIndex + 1) % delayBufferSize

            // Final Wet/Dry mix with Delay
            var finalL = chorusedL * (1f - p.delayMix) + delayedL * p.delayMix
            var finalR = chorusedR * (1f - p.delayMix) + delayedR * p.delayMix

            // Algorithmic Space Reverb
            if (effReverbMix > 0.005f) {
                val (revL, revR) = processReverb(finalL, finalR, p.reverbSize, p.reverbDamp)
                finalL = finalL * (1f - effReverbMix * 0.5f) + revL * effReverbMix
                finalR = finalR * (1f - effReverbMix * 0.5f) + revR * effReverbMix
            }

            finalL *= p.masterVolume
            finalR *= p.masterVolume

            // Analog master tape limiter (prevents harsh digital clipping)
            finalL = (finalL / sqrt(1.0f + finalL * finalL)).coerceIn(-1.0f, 1.0f)
            finalR = (finalR / sqrt(1.0f + finalR * finalR)).coerceIn(-1.0f, 1.0f)

            outputL[i] = finalL
            outputR[i] = finalR

            // Write mono sum to oscilloscope / FFT buffer
            val monoSum = (finalL + finalR) * 0.5f
            scopeBufferInternal[scopeWriteIndex] = monoSum
            scopeWriteIndex++
            if (scopeWriteIndex >= WAVEFORM_BUFFER_SIZE) {
                scopeWriteIndex = 0
                synchronized(scopeLock) {
                    System.arraycopy(scopeBufferInternal, 0, scopeBufferExport, 0, WAVEFORM_BUFFER_SIZE)
                }
            }
        }
    }

    /**
     * Fetch the latest waveform snapshot for 60fps Canvas rendering
     */
    fun getWaveformSnapshot(dest: FloatArray) {
        synchronized(scopeLock) {
            System.arraycopy(scopeBufferExport, 0, dest, 0, min(dest.size, WAVEFORM_BUFFER_SIZE))
        }
    }

    private fun midiToFreq(note: Int): Float {
        return (masterTuningHz * 2.0.pow((note - 69.0) / 12.0)).toFloat()
    }

    private enum class EnvStage { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE }

    /**
     * Individual Synthesizer Voice
     */
    private inner class Voice {
        var isActive = false
        var activeNote = -1
        var voiceAge = 0L

        private var currentFreq = 440f
        private var targetFreq = 440f
        private var velocity = 0.8f

        // Oscillators Phase
        private var osc1Phase = 0.0
        private var osc2Phase = 0.0

        // Envelopes
        private var ampStage = EnvStage.IDLE
        var ampEnvLevel = 0.0f
        private var filtStage = EnvStage.IDLE
        var filtEnvLevel = 0.0f

        // Resonant State Variable Filter (Chamberlin SVF)
        private var svfLow = 0.0f
        private var svfBand = 0.0f

        fun trigger(note: Int, freq: Float, vel: Float, p: SynthPatch) {
            isActive = true
            activeNote = note
            voiceAge = System.currentTimeMillis()
            velocity = vel
            targetFreq = freq
            if (p.glideTime <= 0.005f || !isActive) {
                currentFreq = freq
            }
            ampStage = EnvStage.ATTACK
            filtStage = EnvStage.ATTACK
        }

        fun release() {
            if (isActive) {
                ampStage = EnvStage.RELEASE
                filtStage = EnvStage.RELEASE
            }
        }

        fun kill() {
            isActive = false
            activeNote = -1
            ampStage = EnvStage.IDLE
            filtStage = EnvStage.IDLE
            ampEnvLevel = 0f
            filtEnvLevel = 0f
            svfLow = 0f
            svfBand = 0f
        }

        fun renderSample(
            p: SynthPatch,
            lfoOut: Float,
            pitchModOsc1: Float,
            pitchModOsc2: Float,
            cutoffMod: Float,
            resMod: Float
        ): Float {
            // Frequency Glide / Portamento
            if (abs(currentFreq - targetFreq) > 0.1f) {
                val glideFactor = exp(-1.0f / (p.glideTime * SAMPLE_RATE).coerceAtLeast(1.0f))
                currentFreq = targetFreq + (currentFreq - targetFreq) * glideFactor
            } else {
                currentFreq = targetFreq
            }

            // Frequency for Osc 1 (Octave + Semi + Fine + LFO pitch + Mod + PitchBend)
            val pitchBend = this@SynthEngine.pitchBendSemitones
            val osc1MidiShift = (p.osc1Octave * 12) + p.osc1Semi + p.osc1Fine + (lfoOut * 2.0f) + pitchModOsc1 + pitchBend
            val freq1 = currentFreq * 2.0.pow(osc1MidiShift / 12.0).toFloat()

            // Frequency for Osc 2
            val osc2MidiShift = (p.osc2Octave * 12) + p.osc2Semi + p.osc2Fine + pitchModOsc2 + pitchBend
            val freq2 = currentFreq * 2.0.pow(osc2MidiShift / 12.0).toFloat()

            // Phase increments
            val osc1Inc = freq1 / SAMPLE_RATE
            val osc2Inc = freq2 / SAMPLE_RATE

            osc1Phase += osc1Inc
            if (osc1Phase >= 1.0) osc1Phase -= 1.0

            osc2Phase += osc2Inc
            if (osc2Phase >= 1.0) {
                osc2Phase -= 1.0
                if (p.osc2Sync && osc1Phase < osc1Inc) {
                    osc2Phase = 0.0
                }
            }

            // Generate oscillator waveforms
            val s1 = generateWaveform(p.osc1Waveform, osc1Phase) * p.osc1Volume
            val s2 = generateWaveform(p.osc2Waveform, osc2Phase) * p.osc2Volume

            val rawSample = s1 + s2

            // Envelope progression
            updateAmpEnvelope(p)
            updateFiltEnvelope(p)

            if (ampStage == EnvStage.IDLE) {
                isActive = false
                return 0f
            }

            // Resonant Filter computation (SVF)
            val modWheelCutoff = this@SynthEngine.modWheelAmount * 6000f
            val baseCutoff = p.filterCutoff + (filtEnvLevel * p.filterEnvAmount * 8000f) + cutoffMod + (lfoOut * 1200f) + modWheelCutoff
            val clampedCutoff = baseCutoff.coerceIn(20f, 18000f)
            val effectiveRes = (p.filterResonance + resMod).coerceIn(0.2f, 9.5f)

            // Chamberlin SVF coefficients
            val f = 2.0f * sin(PI.toFloat() * (clampedCutoff / SAMPLE_RATE)).coerceIn(0.001f, 0.99f)
            val q = 1.0f / effectiveRes

            svfLow += f * svfBand
            val high = rawSample - svfLow - q * svfBand
            svfBand += f * high
            val notch = high + svfLow

            val filteredSample = when (p.filterType) {
                FilterType.LOW_PASS -> svfLow
                FilterType.BAND_PASS -> svfBand
                FilterType.HIGH_PASS -> high
            }

            // Final VCA output
            return filteredSample * ampEnvLevel * velocity
        }

        private fun generateWaveform(type: Waveform, phase: Double): Float {
            return when (type) {
                Waveform.SINE -> sin(phase * 2.0 * PI).toFloat()
                Waveform.TRIANGLE -> {
                    val p = phase.toFloat()
                    if (p < 0.5f) 4.0f * p - 1.0f else 3.0f - 4.0f * p
                }
                Waveform.SAWTOOTH -> (2.0f * phase.toFloat() - 1.0f)
                Waveform.SQUARE -> if (phase < 0.5) 0.9f else -0.9f
                Waveform.NOISE -> (Math.random() * 2.0 - 1.0).toFloat() * 0.7f
            }
        }

        private fun updateAmpEnvelope(p: SynthPatch) {
            when (ampStage) {
                EnvStage.ATTACK -> {
                    val rate = 1.0f / (p.ampAttack * SAMPLE_RATE).coerceAtLeast(1.0f)
                    ampEnvLevel += rate
                    if (ampEnvLevel >= 1.0f) {
                        ampEnvLevel = 1.0f
                        ampStage = EnvStage.DECAY
                    }
                }
                EnvStage.DECAY -> {
                    val rate = (1.0f - p.ampSustain) / (p.ampDecay * SAMPLE_RATE).coerceAtLeast(1.0f)
                    ampEnvLevel -= rate
                    if (ampEnvLevel <= p.ampSustain) {
                        ampEnvLevel = p.ampSustain
                        ampStage = EnvStage.SUSTAIN
                    }
                }
                EnvStage.SUSTAIN -> {
                    ampEnvLevel = p.ampSustain
                }
                EnvStage.RELEASE -> {
                    val rate = 1.0f / (p.ampRelease * SAMPLE_RATE).coerceAtLeast(1.0f)
                    ampEnvLevel -= rate
                    if (ampEnvLevel <= 0.0001f) {
                        ampEnvLevel = 0.0f
                        ampStage = EnvStage.IDLE
                    }
                }
                EnvStage.IDLE -> {
                    ampEnvLevel = 0.0f
                }
            }
        }

        private fun updateFiltEnvelope(p: SynthPatch) {
            when (filtStage) {
                EnvStage.ATTACK -> {
                    val rate = 1.0f / (p.filtAttack * SAMPLE_RATE).coerceAtLeast(1.0f)
                    filtEnvLevel += rate
                    if (filtEnvLevel >= 1.0f) {
                        filtEnvLevel = 1.0f
                        filtStage = EnvStage.DECAY
                    }
                }
                EnvStage.DECAY -> {
                    val rate = (1.0f - p.filtSustain) / (p.filtDecay * SAMPLE_RATE).coerceAtLeast(1.0f)
                    filtEnvLevel -= rate
                    if (filtEnvLevel <= p.filtSustain) {
                        filtEnvLevel = p.filtSustain
                        filtStage = EnvStage.SUSTAIN
                    }
                }
                EnvStage.SUSTAIN -> {
                    filtEnvLevel = p.filtSustain
                }
                EnvStage.RELEASE -> {
                    val rate = 1.0f / (p.filtRelease * SAMPLE_RATE).coerceAtLeast(1.0f)
                    filtEnvLevel -= rate
                    if (filtEnvLevel <= 0.0001f) {
                        filtEnvLevel = 0.0f
                        filtStage = EnvStage.IDLE
                    }
                }
                EnvStage.IDLE -> {
                    filtEnvLevel = 0.0f
                }
            }
        }
    }
}
