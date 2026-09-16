package com.example.audio

import kotlin.math.PI

/**
 * Waveform types supported by Synapse Oscillators
 */
enum class Waveform(val label: String) {
    SINE("SIN"),
    TRIANGLE("TRI"),
    SAWTOOTH("SAW"),
    SQUARE("SQR"),
    NOISE("NOI")
}

/**
 * Filter modes for the resonant State Variable Filter
 */
enum class FilterType(val label: String) {
    LOW_PASS("LP"),
    BAND_PASS("BP"),
    HIGH_PASS("HP")
}

/**
 * Musical scales for keyboard and sequencer quantization
 */
enum class MusicalScale(val label: String, val intervals: List<Int>) {
    CHROMATIC("Chromatic", listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)),
    MINOR_PENTATONIC("Min Pentatonic", listOf(0, 3, 5, 7, 10)),
    MAJOR_PENTATONIC("Maj Pentatonic", listOf(0, 2, 4, 7, 9)),
    DORIAN("Dorian (Synthwave)", listOf(0, 2, 3, 5, 7, 9, 10)),
    NATURAL_MINOR("Aeolian Minor", listOf(0, 2, 3, 5, 7, 8, 10)),
    HIRAJOSHI("Hirajoshi (Cyber)", listOf(0, 2, 3, 7, 8)),
    BLUES("Blues", listOf(0, 3, 5, 6, 7, 10))
}

/**
 * Patch bay socket connection endpoints
 */
enum class PatchSource(val label: String) {
    LFO_OUT("LFO 1"),
    AMP_ENV("Amp Env"),
    FILTER_ENV("Filt Env"),
    RANDOM_SH("S&H Noise"),
    VELOCITY("Velocity")
}

enum class PatchDestination(val label: String) {
    FILTER_CUTOFF("Cutoff"),
    FILTER_RES("Resonance"),
    OSC1_PITCH("Osc1 Pitch"),
    OSC2_PITCH("Osc2 Pitch"),
    DELAY_TIME("Delay Time"),
    DRIVE_GAIN("Drive"),
    CHORUS_MIX("Chorus Mix"),
    REVERB_MIX("Reverb Mix")
}

data class PatchCable(
    val id: String = java.util.UUID.randomUUID().toString(),
    val source: PatchSource,
    val destination: PatchDestination,
    val amount: Float = 0.6f,
    val colorHex: Long = 0xFF00E5FF
)

/**
 * Core parameters defining a synth patch
 */
data class SynthPatch(
    val name: String = "Init Patch",
    // Oscillator 1
    val osc1Waveform: Waveform = Waveform.SAWTOOTH,
    val osc1Octave: Int = 0, // -2 to +2
    val osc1Semi: Int = 0,   // -12 to +12
    val osc1Fine: Float = 0f, // -0.5 to +0.5
    val osc1Volume: Float = 0.8f,
    
    // Oscillator 2
    val osc2Waveform: Waveform = Waveform.SQUARE,
    val osc2Octave: Int = 0,
    val osc2Semi: Int = 7,   // 5th interval default
    val osc2Fine: Float = 0.05f,
    val osc2Volume: Float = 0.5f,
    val osc2Sync: Boolean = false,
    
    // Resonant Filter
    val filterType: FilterType = FilterType.LOW_PASS,
    val filterCutoff: Float = 2400f, // 20Hz - 18000Hz
    val filterResonance: Float = 3.5f, // 0.2 - 9.0
    val filterEnvAmount: Float = 0.5f, // -1.0 to 1.0
    
    // Amp ADSR
    val ampAttack: Float = 0.02f, // seconds
    val ampDecay: Float = 0.25f,
    val ampSustain: Float = 0.65f, // 0.0 to 1.0
    val ampRelease: Float = 0.35f,
    
    // Filter ADSR
    val filtAttack: Float = 0.04f,
    val filtDecay: Float = 0.35f,
    val filtSustain: Float = 0.3f,
    val filtRelease: Float = 0.25f,
    
    // LFO
    val lfoRate: Float = 2.5f, // 0.1Hz - 20Hz
    val lfoDepth: Float = 0.3f,
    val lfoShape: Waveform = Waveform.TRIANGLE,
    
    // Effects
    val delayTime: Float = 0.25f, // 0.05s to 0.75s
    val delayFeedback: Float = 0.45f,
    val delayMix: Float = 0.3f,
    val chorusRate: Float = 1.0f, // 0.2Hz - 5.0Hz
    val chorusDepth: Float = 0.5f, // 0.0 - 1.0
    val chorusMix: Float = 0.0f,  // 0.0 - 0.8
    val reverbSize: Float = 0.65f, // 0.1 to 0.95 Room / Hall size
    val reverbDamp: Float = 0.35f, // 0.0 to 0.9 HF Damping
    val reverbMix: Float = 0.0f,   // 0.0 to 0.8 Reverb Wet Mix
    val driveGain: Float = 1.8f, // 1.0 to 6.0
    val masterVolume: Float = 0.85f,
    val glideTime: Float = 0.04f, // Portamento
    val cables: List<PatchCable> = emptyList()
)
