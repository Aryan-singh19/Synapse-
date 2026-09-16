package com.example.sequencer

import com.example.audio.MusicalScale
import com.example.audio.SynthEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class PlaybackDirection(val label: String) {
    FORWARD(">>>"),
    REVERSE("<<<"),
    PING_PONG("<->"),
    RANDOM("RND")
}

data class SequencerStep(
    val index: Int,
    val enabled: Boolean = true,
    val midiNote: Int = 48, // C3
    val velocity: Float = 0.8f,
    val accent: Boolean = false,
    val octaveShift: Int = 0
)

class StepSequencer(private val synthEngine: SynthEngine) {
    companion object {
        const val NUM_STEPS = 16
        val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        fun getNoteLabel(midiNote: Int): String {
            val octave = (midiNote / 12) - 1
            val noteName = NOTE_NAMES[midiNote % 12]
            return "$noteName$octave"
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var sequencerJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentStep = MutableStateFlow(-1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _bpm = MutableStateFlow(128)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _swing = MutableStateFlow(0f) // 0.0 to 0.5
    val swing: StateFlow<Float> = _swing.asStateFlow()

    private val _gateLength = MutableStateFlow(0.80f) // 0.25 to 0.95
    val gateLength: StateFlow<Float> = _gateLength.asStateFlow()

    private val _direction = MutableStateFlow(PlaybackDirection.FORWARD)
    val direction: StateFlow<PlaybackDirection> = _direction.asStateFlow()

    private val _rootNote = MutableStateFlow(48) // C3
    val rootNote: StateFlow<Int> = _rootNote.asStateFlow()

    private val _selectedScale = MutableStateFlow(MusicalScale.MINOR_PENTATONIC)
    val selectedScale: StateFlow<MusicalScale> = _selectedScale.asStateFlow()

    private val _steps = MutableStateFlow(createDefaultPattern())
    val steps: StateFlow<List<SequencerStep>> = _steps.asStateFlow()

    private var previousMidiNote: Int? = null

    private fun createDefaultPattern(): List<SequencerStep> {
        // Default cyberpunk driving bass groove in C Minor Pentatonic
        val scale = MusicalScale.MINOR_PENTATONIC.intervals
        val root = 48
        return List(NUM_STEPS) { i ->
            val isBeat = i % 2 == 0
            val interval = when (i % 8) {
                0 -> scale[0]
                2 -> scale[1 % scale.size]
                4 -> scale[2 % scale.size]
                6 -> scale[0]
                else -> scale[0]
            }
            SequencerStep(
                index = i,
                enabled = isBeat,
                midiNote = root + interval,
                velocity = if (i % 4 == 0) 0.95f else 0.75f,
                accent = i % 4 == 0
            )
        }
    }

    fun setBpm(newBpm: Int) {
        _bpm.value = newBpm.coerceIn(50, 240)
    }

    fun setSwing(newSwing: Float) {
        _swing.value = newSwing.coerceIn(0f, 0.5f)
    }

    fun setScale(scale: MusicalScale) {
        _selectedScale.value = scale
        quantizeAllStepsToScale()
    }

    fun setRootNote(root: Int) {
        _rootNote.value = root
        quantizeAllStepsToScale()
    }

    fun toggleStep(index: Int) {
        val list = _steps.value.toMutableList()
        val step = list[index]
        list[index] = step.copy(enabled = !step.enabled)
        _steps.value = list
    }

    fun setStepNote(index: Int, midiNote: Int) {
        val list = _steps.value.toMutableList()
        list[index] = list[index].copy(midiNote = midiNote)
        _steps.value = list
    }

    fun toggleAccent(index: Int) {
        val list = _steps.value.toMutableList()
        val step = list[index]
        list[index] = step.copy(
            accent = !step.accent,
            velocity = if (!step.accent) 1.0f else 0.75f
        )
        _steps.value = list
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            stop()
        } else {
            play()
        }
    }

    fun setDirection(dir: PlaybackDirection) {
        _direction.value = dir
    }

    fun setGateLength(length: Float) {
        _gateLength.value = length.coerceIn(0.20f, 0.95f)
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        _currentStep.value = -1

        sequencerJob = coroutineScope.launch {
            var stepIndex = 0
            var pingPongForward = true

            while (isActive && _isPlaying.value) {
                _currentStep.value = stepIndex

                val step = _steps.value[stepIndex]
                val currentBpm = _bpm.value
                val stepDurationMs = (60_000L / currentBpm) / 4L // 16th note in ms

                // Swing timing delay
                val swingOffset = if (stepIndex % 2 == 1) {
                    (stepDurationMs * _swing.value).toLong()
                } else {
                    0L
                }

                // Turn off previous note
                previousMidiNote?.let { synthEngine.noteOff(it) }

                if (step.enabled) {
                    val finalNote = step.midiNote + (step.octaveShift * 12)
                    val vel = if (step.accent) 1.0f else step.velocity
                    synthEngine.noteOn(finalNote, vel)
                    previousMidiNote = finalNote
                } else {
                    previousMidiNote = null
                }

                // Dynamic Gate length
                val gateRatio = _gateLength.value
                val gateDuration = (stepDurationMs * gateRatio).toLong().coerceAtLeast(10L)
                delay(gateDuration + swingOffset)

                if (step.enabled) {
                    previousMidiNote?.let { synthEngine.noteOff(it) }
                    previousMidiNote = null
                }

                val remainingDelay = (stepDurationMs - gateDuration).coerceAtLeast(5L)
                delay(remainingDelay)

                // Advance step index based on direction
                when (_direction.value) {
                    PlaybackDirection.FORWARD -> {
                        stepIndex = (stepIndex + 1) % NUM_STEPS
                    }
                    PlaybackDirection.REVERSE -> {
                        stepIndex = if (stepIndex - 1 < 0) NUM_STEPS - 1 else stepIndex - 1
                    }
                    PlaybackDirection.PING_PONG -> {
                        if (pingPongForward) {
                            if (stepIndex >= NUM_STEPS - 1) {
                                pingPongForward = false
                                stepIndex = NUM_STEPS - 2
                            } else {
                                stepIndex++
                            }
                        } else {
                            if (stepIndex <= 0) {
                                pingPongForward = true
                                stepIndex = 1
                            } else {
                                stepIndex--
                            }
                        }
                    }
                    PlaybackDirection.RANDOM -> {
                        stepIndex = Random.nextInt(NUM_STEPS)
                    }
                }
            }
        }
    }

    fun transpose(semitones: Int) {
        val list = _steps.value.map { step ->
            val shifted = (step.midiNote + semitones).coerceIn(24, 96)
            step.copy(midiNote = shifted)
        }
        _steps.value = list
    }

    fun clearAll() {
        val list = _steps.value.map { it.copy(enabled = false) }
        _steps.value = list
    }

    fun invertSteps() {
        val list = _steps.value.map { it.copy(enabled = !it.enabled) }
        _steps.value = list
    }

    fun shiftSteps(offset: Int) {
        val current = _steps.value
        val list = List(NUM_STEPS) { i ->
            val srcIdx = (i - offset + NUM_STEPS) % NUM_STEPS
            current[srcIdx].copy(index = i)
        }
        _steps.value = list
    }

    fun stop() {
        _isPlaying.value = false
        sequencerJob?.cancel()
        sequencerJob = null
        previousMidiNote?.let { synthEngine.noteOff(it) }
        previousMidiNote = null
        synthEngine.allNotesOff()
        _currentStep.value = -1
    }

    /**
     * Mutate current pattern: intelligently tweak 3-4 steps in key
     */
    fun mutate() {
        val scale = _selectedScale.value.intervals
        val root = _rootNote.value
        val list = _steps.value.toMutableList()

        repeat(4) {
            val idx = Random.nextInt(NUM_STEPS)
            val interval = scale[Random.nextInt(scale.size)]
            val octave = if (Random.nextFloat() > 0.75f) 12 else 0
            val shouldEnable = Random.nextFloat() > 0.25f
            list[idx] = list[idx].copy(
                enabled = shouldEnable,
                midiNote = root + interval + octave,
                accent = Random.nextFloat() > 0.7f
            )
        }
        _steps.value = list
    }

    /**
     * Algorithmic Euclidean Rhythm Generator
     */
    fun generateEuclidean(hits: Int, stepsCount: Int = NUM_STEPS) {
        val pattern = BooleanArray(stepsCount)
        var bucket = 0
        for (i in 0 until stepsCount) {
            bucket += hits
            if (bucket >= stepsCount) {
                bucket -= stepsCount
                pattern[i] = true
            }
        }

        val scale = _selectedScale.value.intervals
        val root = _rootNote.value
        val list = _steps.value.toMutableList()

        for (i in 0 until stepsCount) {
            val interval = scale[(i % scale.size)]
            list[i] = list[i].copy(
                enabled = pattern[i],
                midiNote = root + interval,
                accent = i % 4 == 0
            )
        }
        _steps.value = list
    }

    /**
     * Generate fresh algorithmic bass/arp riff
     */
    fun randomizeAll() {
        val scale = _selectedScale.value.intervals
        val root = _rootNote.value
        val list = List(NUM_STEPS) { i ->
            val isEnabled = Random.nextFloat() > 0.3f
            val interval = scale[Random.nextInt(scale.size)]
            val octave = if (Random.nextFloat() > 0.65f) 12 else 0
            SequencerStep(
                index = i,
                enabled = isEnabled,
                midiNote = root + interval + octave,
                velocity = if (Random.nextFloat() > 0.5f) 0.9f else 0.7f,
                accent = Random.nextFloat() > 0.7f
            )
        }
        _steps.value = list
    }

    private fun quantizeAllStepsToScale() {
        val scale = _selectedScale.value.intervals
        val root = _rootNote.value
        val list = _steps.value.map { step ->
            val noteOffset = (step.midiNote - root + 1200) % 12
            // Find closest interval in scale
            val closestInterval = scale.minByOrNull { kotlin.math.abs(it - noteOffset) } ?: 0
            val octaveBase = ((step.midiNote - root) / 12) * 12
            step.copy(midiNote = root + octaveBase + closestInterval)
        }
        _steps.value = list
    }
}
