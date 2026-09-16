package com.example.sequencer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class ArpMode(val label: String) {
    UP("UP"),
    DOWN("DOWN"),
    UP_DOWN("U&D"),
    RANDOM("RND"),
    CHORD("CHORD")
}

enum class ArpRate(val label: String, val beatsPerStep: Float) {
    RATE_1_4("1/4", 1.0f),
    RATE_1_8("1/8", 0.5f),
    RATE_1_16("1/16", 0.25f),
    RATE_1_32("1/32", 0.125f)
}

/**
 * Hardware-grade Arpeggiator engine with Latch, multi-octave traversal,
 * and tempo-synced articulation.
 */
class Arpeggiator(
    private val onNoteOn: (note: Int, velocity: Float) -> Unit,
    private val onNoteOff: (note: Int) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var arpJob: Job? = null

    // Configuration states
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _isLatch = MutableStateFlow(false)
    val isLatch: StateFlow<Boolean> = _isLatch.asStateFlow()

    private val _mode = MutableStateFlow(ArpMode.UP)
    val mode: StateFlow<ArpMode> = _mode.asStateFlow()

    private val _octaves = MutableStateFlow(2) // 1, 2, or 3
    val octaves: StateFlow<Int> = _octaves.asStateFlow()

    private val _rate = MutableStateFlow(ArpRate.RATE_1_16)
    val rate: StateFlow<ArpRate> = _rate.asStateFlow()

    private val _gate = MutableStateFlow(0.65f) // 0.2 to 0.95
    val gate: StateFlow<Float> = _gate.asStateFlow()

    private val _currentActiveNote = MutableStateFlow<Int?>(null)
    val currentActiveNote: StateFlow<Int?> = _currentActiveNote.asStateFlow()

    // Real-time held and latched notes
    private val physicalKeys = mutableSetOf<Int>()
    private val latchedKeys = mutableSetOf<Int>()

    @Volatile
    private var bpm: Int = 120

    fun setBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(40, 260)
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        if (!enabled) {
            stopPlayback()
            latchedKeys.clear()
        } else {
            if (activeNotesPool().isNotEmpty()) {
                startPlayback()
            }
        }
    }

    fun toggleLatch() {
        val newLatch = !_isLatch.value
        _isLatch.value = newLatch
        if (!newLatch && physicalKeys.isEmpty()) {
            latchedKeys.clear()
            stopPlayback()
        }
    }

    fun setLatch(latch: Boolean) {
        if (_isLatch.value != latch) {
            toggleLatch()
        }
    }

    fun setMode(newMode: ArpMode) {
        _mode.value = newMode
    }

    fun setOctaves(newOctaves: Int) {
        _octaves.value = newOctaves.coerceIn(1, 3)
    }

    fun setRate(newRate: ArpRate) {
        _rate.value = newRate
    }

    fun setGate(newGate: Float) {
        _gate.value = newGate.coerceIn(0.1f, 0.95f)
    }

    fun onKeyPressed(midiNote: Int) {
        physicalKeys.add(midiNote)
        if (_isLatch.value) {
            latchedKeys.add(midiNote)
        }
        if (_isEnabled.value) {
            startPlayback()
        }
    }

    fun onKeyReleased(midiNote: Int) {
        physicalKeys.remove(midiNote)
        if (!_isLatch.value) {
            if (physicalKeys.isEmpty()) {
                stopPlayback()
            }
        }
    }

    fun clearLatch() {
        latchedKeys.clear()
        if (physicalKeys.isEmpty()) {
            stopPlayback()
        }
    }

    private fun activeNotesPool(): Set<Int> {
        return if (_isLatch.value && latchedKeys.isNotEmpty()) {
            latchedKeys
        } else {
            physicalKeys
        }
    }

    private fun startPlayback() {
        if (arpJob?.isActive == true) return

        arpJob = scope.launch {
            var stepIndex = 0
            var pingPongForward = true
            var lastTriggeredNotes = listOf<Int>()

            while (isActive && _isEnabled.value) {
                val pool = activeNotesPool().toList().sorted()
                if (pool.isEmpty()) {
                    // Turn off any lingering note
                    lastTriggeredNotes.forEach { onNoteOff(it) }
                    lastTriggeredNotes = emptyList()
                    _currentActiveNote.value = null
                    delay(50)
                    continue
                }

                // Generate expanded multi-octave note list
                val numOct = _octaves.value
                val fullNotes = mutableListOf<Int>()
                for (oct in 0 until numOct) {
                    for (n in pool) {
                        val shifted = n + (oct * 12)
                        if (shifted <= 108) fullNotes.add(shifted)
                    }
                }

                val notesToTrigger = when (_mode.value) {
                    ArpMode.UP -> {
                        val sorted = fullNotes.sorted()
                        val n = sorted[stepIndex % sorted.size]
                        stepIndex++
                        listOf(n)
                    }
                    ArpMode.DOWN -> {
                        val sortedDesc = fullNotes.sortedDescending()
                        val n = sortedDesc[stepIndex % sortedDesc.size]
                        stepIndex++
                        listOf(n)
                    }
                    ArpMode.UP_DOWN -> {
                        val sorted = fullNotes.sorted()
                        if (sorted.size == 1) {
                            listOf(sorted[0])
                        } else {
                            val n = sorted[stepIndex]
                            if (pingPongForward) {
                                stepIndex++
                                if (stepIndex >= sorted.size - 1) pingPongForward = false
                            } else {
                                stepIndex--
                                if (stepIndex <= 0) pingPongForward = true
                            }
                            listOf(n)
                        }
                    }
                    ArpMode.RANDOM -> {
                        listOf(fullNotes[Random.nextInt(fullNotes.size)])
                    }
                    ArpMode.CHORD -> {
                        fullNotes
                    }
                }

                // Release previous notes
                lastTriggeredNotes.forEach { onNoteOff(it) }

                // Trigger current step
                notesToTrigger.forEach { onNoteOn(it, 0.85f) }
                lastTriggeredNotes = notesToTrigger
                _currentActiveNote.value = notesToTrigger.firstOrNull()

                // Calculate duration based on BPM and rate divider
                val beatMs = (60_000.0 / bpm.toDouble())
                val stepMs = (beatMs * _rate.value.beatsPerStep).toLong().coerceAtLeast(30L)
                val gateMs = (stepMs * _gate.value).toLong().coerceAtLeast(15L)
                val restMs = (stepMs - gateMs).coerceAtLeast(10L)

                delay(gateMs)

                // Gate off
                notesToTrigger.forEach { onNoteOff(it) }
                lastTriggeredNotes = emptyList()
                _currentActiveNote.value = null

                delay(restMs)
            }

            // Cleanup when loop ends
            lastTriggeredNotes.forEach { onNoteOff(it) }
            _currentActiveNote.value = null
        }
    }

    fun stop() {
        _isEnabled.value = false
        stopPlayback()
        clearLatch()
    }

    private fun stopPlayback() {
        arpJob?.cancel()
        arpJob = null
        _currentActiveNote.value = null
    }

    fun release() {
        stopPlayback()
        scope.cancel()
    }
}
