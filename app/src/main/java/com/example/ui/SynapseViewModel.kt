package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.*
import com.example.presets.PresetBank
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

    private val _currentPatch = MutableStateFlow(PresetBank.factoryPresets[0])
    val currentPatch: StateFlow<SynthPatch> = _currentPatch.asStateFlow()

    private val _activeTab = MutableStateFlow(StudioTab.RACK)
    val activeTab: StateFlow<StudioTab> = _activeTab.asStateFlow()

    private val _baseOctave = MutableStateFlow(3) // C3 base
    val baseOctave: StateFlow<Int> = _baseOctave.asStateFlow()

    private val _waveformSnapshot = MutableStateFlow(FloatArray(SynthEngine.WAVEFORM_BUFFER_SIZE))
    val waveformSnapshot: StateFlow<FloatArray> = _waveformSnapshot.asStateFlow()

    private val _peakRms = MutableStateFlow(0f)
    val peakRms: StateFlow<Float> = _peakRms.asStateFlow()

    private val _customPresets = MutableStateFlow<List<SynthPatch>>(emptyList())
    val customPresets: StateFlow<List<SynthPatch>> = _customPresets.asStateFlow()

    private var visualizerJob: Job? = null

    init {
        synthEngine.patch = _currentPatch.value
        synthEngine.start()
        startVisualizerLoop()
    }

    private fun startVisualizerLoop() {
        visualizerJob = viewModelScope.launch(Dispatchers.Default) {
            val localBuffer = FloatArray(SynthEngine.WAVEFORM_BUFFER_SIZE)
            while (isActive) {
                synthEngine.getWaveformSnapshot(localBuffer)
                _waveformSnapshot.value = localBuffer.copyOf()
                _peakRms.value = synthEngine.currentPeakRms
                delay(25L) // ~40-50 fps refresh
            }
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
        synthEngine.noteOn(midiNote, velocity)
    }

    fun noteOff(midiNote: Int) {
        synthEngine.noteOff(midiNote)
    }

    fun panic() {
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
        sequencer.stop()
        synthEngine.stop()
    }
}
