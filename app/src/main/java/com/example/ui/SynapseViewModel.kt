package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.*
import com.example.presets.PresetBank
import com.example.sequencer.Arpeggiator
import com.example.sequencer.StepSequencer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StudioTab(val label: String, val iconBadge: String) {
    RACK("RACK", "VCO/VCF"),
    SEQUENCER("SEQUENCER", "16-STEP"),
    PATCH_BAY("PATCH BAY", "ROUTING"),
    PERFORMANCE("PERFORM", "KEYS/XY")
}

class SynapseViewModel : ViewModel() {
    val synthEngine = SynthEngine()
    val sequencer = StepSequencer(synthEngine)
    val arpeggiator = Arpeggiator(
        onNoteOn = { note, vel -> synthEngine.noteOn(note, vel) },
        onNoteOff = { note -> synthEngine.noteOff(note) }
    )

    private val _currentPatch = MutableStateFlow(PresetBank.factoryPresets[0])
    val currentPatch: StateFlow<SynthPatch> = _currentPatch.asStateFlow()

    private val _activeTab = MutableStateFlow(StudioTab.RACK)
    val activeTab: StateFlow<StudioTab> = _activeTab.asStateFlow()

    private val _baseOctave = MutableStateFlow(3) // C3 base
    val baseOctave: StateFlow<Int> = _baseOctave.asStateFlow()

    private val _waveformSnapshot = MutableStateFlow(FloatArray(SynthEngine.WAVEFORM_BUFFER_SIZE))
    val waveformSnapshot: StateFlow<FloatArray> = _waveformSnapshot.asStateFlow()

    private val _spectrumSnapshot = MutableStateFlow(FloatArray(32))
    val spectrumSnapshot: StateFlow<FloatArray> = _spectrumSnapshot.asStateFlow()

    private val _peakRms = MutableStateFlow(0f)
    val peakRms: StateFlow<Float> = _peakRms.asStateFlow()

    private val _customPresets = MutableStateFlow<List<SynthPatch>>(emptyList())
    val customPresets: StateFlow<List<SynthPatch>> = _customPresets.asStateFlow()

    // WAV Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0f)
    val recordingDurationSec: StateFlow<Float> = _recordingDurationSec.asStateFlow()

    private val _lastRecordedFile = MutableStateFlow<java.io.File?>(null)
    val lastRecordedFile: StateFlow<java.io.File?> = _lastRecordedFile.asStateFlow()

    private val _recordedFiles = MutableStateFlow<List<java.io.File>>(emptyList())
    val recordedFiles: StateFlow<List<java.io.File>> = _recordedFiles.asStateFlow()

    // Performance controllers
    private val _pitchBend = MutableStateFlow(0f)
    val pitchBend: StateFlow<Float> = _pitchBend.asStateFlow()

    private val _modWheel = MutableStateFlow(0f)
    val modWheel: StateFlow<Float> = _modWheel.asStateFlow()

    private val _masterTuning = MutableStateFlow(440f)
    val masterTuning: StateFlow<Float> = _masterTuning.asStateFlow()

    // Real-time voice allocation monitoring (4 voices)
    private val _activeVoices = MutableStateFlow(BooleanArray(SynthEngine.MAX_VOICES))
    val activeVoices: StateFlow<BooleanArray> = _activeVoices.asStateFlow()

    // Performance Sustain Latch
    private val _isSustainPedal = MutableStateFlow(false)
    val isSustainPedal: StateFlow<Boolean> = _isSustainPedal.asStateFlow()
    private val sustainedNotes = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    private val tapTimestamps = mutableListOf<Long>()

    private var visualizerJob: Job? = null
    private var recordingTimerJob: Job? = null

    init {
        synthEngine.patch = _currentPatch.value
        synthEngine.start()
        startVisualizerLoop()
        viewModelScope.launch {
            sequencer.bpm.collect { bpm ->
                arpeggiator.setBpm(bpm)
            }
        }
    }

    fun startRecording(context: android.content.Context) {
        if (_isRecording.value) return
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val dir = java.io.File(context.filesDir, "recordings")
        dir.mkdirs()
        val file = java.io.File(dir, "Synapse_Jam_$timeStamp.wav")

        if (synthEngine.recorder.start(file)) {
            _isRecording.value = true
            _recordingDurationSec.value = 0f
            recordingTimerJob = viewModelScope.launch(Dispatchers.Default) {
                while (isActive && _isRecording.value) {
                    _recordingDurationSec.value = synthEngine.recorder.recordedDurationSeconds
                    delay(200L)
                }
            }
        }
    }

    fun stopRecording(context: android.content.Context? = null): java.io.File? {
        if (!_isRecording.value) return null
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        val recordedFile = synthEngine.recorder.stop()
        _isRecording.value = false
        _recordingDurationSec.value = 0f
        if (recordedFile != null && recordedFile.exists()) {
            _lastRecordedFile.value = recordedFile
            context?.let { refreshRecordedFiles(it) }
        }
        return recordedFile
    }

    fun refreshRecordedFiles(context: android.content.Context) {
        val dir = java.io.File(context.filesDir, "recordings")
        if (dir.exists()) {
            val list = dir.listFiles { file -> file.extension.lowercase() == "wav" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            _recordedFiles.value = list
        }
    }

    fun dismissLastRecording() {
        _lastRecordedFile.value = null
    }

    fun setPitchBend(semitones: Float) {
        val clamped = semitones.coerceIn(-2.0f, 2.0f)
        _pitchBend.value = clamped
        synthEngine.pitchBendSemitones = clamped
    }

    fun setModWheel(amount: Float) {
        val clamped = amount.coerceIn(0f, 1.0f)
        _modWheel.value = clamped
        synthEngine.modWheelAmount = clamped
    }

    fun setMasterTuning(hz: Float) {
        val clamped = hz.coerceIn(430f, 450f)
        _masterTuning.value = clamped
        synthEngine.masterTuningHz = clamped
    }

    fun setMasterVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1.2f)
        val updated = _currentPatch.value.copy(masterVolume = clamped)
        updatePatch(updated)
    }

    private fun startVisualizerLoop() {
        visualizerJob = viewModelScope.launch(Dispatchers.Default) {
            val localBuffer = FloatArray(SynthEngine.WAVEFORM_BUFFER_SIZE)
            val tempSpectrum = FloatArray(32)
            while (isActive) {
                synthEngine.getWaveformSnapshot(localBuffer)
                _waveformSnapshot.value = localBuffer.copyOf()
                _peakRms.value = synthEngine.currentPeakRms

                // Real-time 32-band spectral magnitude estimation
                val step = 4
                val samplesCount = localBuffer.size / step
                for (b in 0 until 32) {
                    val k = (b * 3 + 1)
                    var real = 0f
                    var imag = 0f
                    for (i in 0 until localBuffer.size step step) {
                        val angle = 2.0 * Math.PI * k * i / localBuffer.size
                        val s = localBuffer[i]
                        real += s * kotlin.math.cos(angle).toFloat()
                        imag -= s * kotlin.math.sin(angle).toFloat()
                    }
                    val mag = (kotlin.math.sqrt(real * real + imag * imag) / samplesCount) * (1.2f + b * 0.04f)
                    // Smooth decay response (analog meter ballistics)
                    tempSpectrum[b] = kotlin.math.max(mag * 4.2f, tempSpectrum[b] * 0.82f).coerceIn(0f, 1f)
                }
                _spectrumSnapshot.value = tempSpectrum.copyOf()
                _activeVoices.value = synthEngine.getVoiceActiveMask()

                delay(25L) // ~40 fps refresh
            }
        }
    }

    fun tapTempo() {
        val now = System.currentTimeMillis()
        tapTimestamps.add(now)
        while (tapTimestamps.size > 4 || (tapTimestamps.size > 1 && now - tapTimestamps.first() > 2500)) {
            tapTimestamps.removeAt(0)
        }
        if (tapTimestamps.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until tapTimestamps.size) {
                intervals.add(tapTimestamps[i] - tapTimestamps[i - 1])
            }
            val avgInterval = intervals.average()
            if (avgInterval > 150) {
                val calculatedBpm = (60000.0 / avgInterval).toInt().coerceIn(40, 260)
                sequencer.setBpm(calculatedBpm)
                arpeggiator.setBpm(calculatedBpm)
            }
        }
    }

    fun toggleSustainPedal() {
        val newSustain = !_isSustainPedal.value
        _isSustainPedal.value = newSustain
        if (!newSustain) {
            for (note in sustainedNotes) {
                synthEngine.noteOff(note)
            }
            sustainedNotes.clear()
        }
    }

    fun setTab(tab: StudioTab) {
        _activeTab.value = tab
    }

    fun setBaseOctave(octave: Int) {
        _baseOctave.value = octave.coerceIn(1, 6)
    }

    fun updatePatch(patch: SynthPatch) {
        _currentPatch.value = patch
        synthEngine.patch = patch
    }

    fun loadPreset(preset: SynthPatch) {
        _currentPatch.value = preset
        synthEngine.patch = preset
    }

    fun saveCurrentAsCustom(name: String) {
        val trimmed = name.trim().ifEmpty { "Custom Patch" }
        val newPatch = _currentPatch.value.copy(name = trimmed)
        val list = _customPresets.value.toMutableList()
        list.removeAll { it.name == trimmed }
        list.add(newPatch)
        _customPresets.value = list
    }

    fun noteOn(midiNote: Int, velocity: Float = 0.85f) {
        if (arpeggiator.isEnabled.value) {
            arpeggiator.onKeyPressed(midiNote)
        } else {
            synthEngine.noteOn(midiNote, velocity)
            if (_isSustainPedal.value) {
                sustainedNotes.add(midiNote)
            }
        }
    }

    fun noteOff(midiNote: Int) {
        if (arpeggiator.isEnabled.value) {
            arpeggiator.onKeyReleased(midiNote)
        } else {
            if (_isSustainPedal.value) {
                sustainedNotes.add(midiNote)
            } else {
                synthEngine.noteOff(midiNote)
            }
        }
    }

    fun panic() {
        sustainedNotes.clear()
        _isSustainPedal.value = false
        arpeggiator.clearLatch()
        sequencer.stop()
        synthEngine.allNotesOff()
    }

    fun updateXyPad(cutoff: Float, resonance: Float) {
        val updated = _currentPatch.value.copy(
            filterCutoff = cutoff,
            filterResonance = resonance
        )
        updatePatch(updated)
    }

    // Patch Bay Operations
    fun addPatchCable(source: PatchSource, destination: PatchDestination, amount: Float, colorHex: Long) {
        val currentCables = _currentPatch.value.cables.toMutableList()
        currentCables.removeAll { it.source == source && it.destination == destination }
        val newCable = PatchCable(
            source = source,
            destination = destination,
            amount = amount,
            colorHex = colorHex
        )
        currentCables.add(newCable)
        updatePatch(_currentPatch.value.copy(cables = currentCables))
    }

    fun removePatchCable(cableId: String) {
        val currentCables = _currentPatch.value.cables.filterNot { it.id == cableId }
        updatePatch(_currentPatch.value.copy(cables = currentCables))
    }

    fun updatePatchCableAmount(cableId: String, amount: Float) {
        val currentCables = _currentPatch.value.cables.map {
            if (it.id == cableId) it.copy(amount = amount) else it
        }
        updatePatch(_currentPatch.value.copy(cables = currentCables))
    }

    fun clearAllPatchCables() {
        updatePatch(_currentPatch.value.copy(cables = emptyList()))
    }

    override fun onCleared() {
        super.onCleared()
        visualizerJob?.cancel()
        recordingTimerJob?.cancel()
        arpeggiator.stop()
        sequencer.stop()
        synthEngine.stop()
    }
}
