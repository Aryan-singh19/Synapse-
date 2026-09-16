# 🎛️ Synapse — Modular Synthesizer & Generative Audio Workstation

[![Android Build](https://img.shields.io/badge/Platform-Android%2014+-3DDC84?style=flat&logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Audio](https://img.shields.io/badge/Audio-Direct%20PCM%20AudioTrack-00E5FF?style=flat)](#audio-architecture)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Synapse** is an authentic, hardware-inspired virtual Eurorack modular synthesizer and generative music studio built entirely in modern Kotlin and Jetpack Compose. It features a low-latency, real-time native audio synthesis engine, an interactive virtual patch bay with color-coded bezier cables, a 16-step generative sequencer with Euclidean rhythm distribution, and studio WAV export capabilities.

---

## ✨ Features & Capabilities

### 1. True Stereo Dual-Oscillator Sound Engine (`AudioTrack` Native Thread)
* **Stereo Interleaved DSP Pipeline**: High-priority real-time audio thread operating in true stereo (`AudioFormat.CHANNEL_OUT_STEREO`) at 44.1 kHz.
* **Oscillator 1 & Oscillator 2**: 5 classic analog waveforms (Sawtooth, Square with Pulse-Width Modulation, Triangle, Sine, Pink Noise).
* **Analog Drift & Detuning**: Fine pitch detune (±24 semitones & fine cents) for thick supersaws and ultra-wide spatial imaging.
* **Hard Sync & Sub-Oscillator**: Adds aggressive harmonics or deep sub-octave reinforcement.
* **4-Pole Resonant Multi-Mode Filter**: Low-Pass 24dB/oct, High-Pass, Band-Pass, and Notch filtering with aggressive resonance feedback and self-oscillation behavior.
* **Dual ADSR Envelopes**: Dedicated Amp and Filter envelope generators with exponential/linear curves.
* **Multi-Waveform LFO**: Sine, Triangle, Square, and Random Sample & Hold, routable to pitch, cutoff, resonance, or effects.

### 2. Studio Effects Chain & Spatial Processing
* **Dimension Stereo Chorus / Ensemble**: Wide quadrature 90-degree dual-voice chorus with adjustable Rate, Depth, and Wet/Dry Mix.
* **Ping-Pong Stereo Delay**: Cross-channel stereo feedback bounce with 75% poly-meter offset for immersive depth and space.
* **Hyperbolic Master Overdrive & Analog Tape Limiter**: Soft analog saturation with hyperbolic tangent clipping curve to prevent harsh digital clipping.

### 3. Hardware-Grade Arpeggiator
* **Modes**: UP, DOWN, UP & DOWN, RANDOM, and CHORD.
* **Tempo Sync & Rates**: Synchronized to master BPM with division rates `1/4`, `1/8`, `1/16`, and `1/32`.
* **Octave Range**: Traverses 1 to 3 octaves automatically.
* **Latch Functionality**: Hold keys indefinitely to keep generative arpeggio loops running hands-free.

### 4. Dual-Mode Visualizer: CRT Oscilloscope & 32-Band FFT Spectrum Analyzer
* **Cathode-Ray Phosphor Oscilloscope**: Real-time waveform monitor with dual-layer CRT glow, crosshair grid, and trace rendering.
* **32-Band FFT Frequency Spectrum Analyzer**: Real-time logarithmic frequency magnitude analysis with neon gradient bars and floating peak-hold caps.
* **Studio VU Peak Meter**: Dual-column dB peak meters with color-coded headroom warnings (-18dB, -12dB, -6dB, 0dB, CLIP).

### 5. Virtual Eurorack Patch Bay
* **Modular Signal Matrix**: Connect modulation sources (LFO, Amp Env, Filter Env, Velocity, Random S&H) to any destination parameter (Cutoff, Pitch, Resonance, Drive, Chorus Mix, Delay Time).
* **Color-Coded Animated Patch Cables**: Visual cables with dynamic sag curvature, signal flow pulses, and bi-directional attenuators.
* **Quick Disconnect & Polarity Tuning**: Fine-tune depth and invert modulation on the fly.

### 6. 16-Step Generative Sequencer
* **Dynamic Playback Modes**: Forward, Reverse, Ping-Pong, and Random step traversals.
* **Algorithmic Rhythm Generation**:
  * **Euclidean Distribution**: Generate complex polyrhythms based on the Euclidean algorithm ($E(k, n)$).
  * **Mutation & Randomize**: Controlled musical variation and drift.
  * **Invert & Shift**: Rotate patterns left/right or flip active notes.
* **Musical Scale Quantization**: Minor Pentatonic, Major Pentatonic, Natural Minor, Dorian, Phrygian, Blues, and Japanese Insen scales.
* **Micro-Timing & Groove**: Independent swing shuffle and dynamic gate length shaping.

### 7. Performance Surface & Expressive Controls
* **Kaoss-Style XY Touch Pad**: Dual-axis continuous filter cutoff and resonance modulation with tactile visual feedback.
* **Spring-Loaded Pitch Bend Wheel**: Returns to center detune upon release.
* **Continuous Modulation Wheel (MOD)**: Assignable real-time vibrato and filter sweep depth.
* **Dual-Octave Multi-Touch Piano Keyboard**: Natural and accidental keys with polyphonic touch tracking and octave transpositions.
* **Concert Reference Tuning**: Switch between standard 440 Hz, alternative 432 Hz, and brilliant 444 Hz concert pitch.

### 8. High-Fidelity Stereo WAV Recording & Export
* Direct, lossless in-engine recording to 16-bit 44.1 kHz stereo `.wav` files.
* Built-in Android system share integration via secure `FileProvider` to send recordings to DAWs, Discord, Google Drive, or messaging apps.

### 9. Curated Factory Sound Banks & Custom Presets
* **Factory Presets**: *Cyberpunk Bass*, *Blade Runner Pad*, *Acid Lead 303*, *Ambient Drone*, *Analog Moog Brass*, *Sub Bass Heavy*, *Stranger Synth 80s*, *Vangelis CS-80 Pad*.
* **Custom Patch Saving**: Save, name, and recall custom modular configurations.

---

## 🏗️ Architecture & Modules

```
com.example/
├── MainActivity.kt               # Root Activity, top studio rack bar, navigation, and audio export
├── audio/
│   ├── SynthEngine.kt            # High-priority native audio thread running real-time DSP
│   ├── SynthVoice.kt             # Polyphonic voice allocator, oscillators, and envelopes
│   ├── Filter.kt                 # 4-pole State Variable Filter (SVF) with non-linear saturation
│   ├── Lfo.kt                    # Multi-shape low-frequency oscillator
│   └── WavAudioRecorder.kt       # Stream-to-disk 16-bit PCM WAV encoder
├── sequencer/
│   ├── StepSequencer.kt          # Coroutine-based timing loop with Euclidean math & swing
│   └── Scale.kt                  # Musical scale intervals and MIDI quantization maps
├── presets/
│   └── PresetBank.kt             # Factory sound presets & patch definitions
└── ui/
    ├── SynapseViewModel.kt       # Reactive MVI/MVVM state management
    ├── components/
    │   ├── ModularControls.kt    # Skeuomorphic rotary knobs, LED meters, and toggle switches
    │   ├── OscilloscopeView.kt   # High-refresh Canvas audio waveform oscilloscope
    │   ├── RackSynthView.kt      # Main oscillator, filter, envelope, and effect panels
    │   ├── SequencerView.kt      # 16-step grid, Euclidean generator, and transport controls
    │   ├── PatchBayView.kt       # Bezier cable patch matrix
    │   └── PerformanceView.kt    # Dual-wheel touch keyboard and XY controller
    └── theme/                    # Material 3 dark cyber-industrial theme
```

---

## 🚀 Building & Exporting the APK

### Method 1: Download from Google AI Studio / GitHub
1. In the **Google AI Studio** project menu, select **Export Project as ZIP** or **Generate APK**.
2. If linked to GitHub, the included GitHub Actions workflow automatically compiles `app-debug.apk` on every push and tag. Download the APK directly from the **Releases** or **Actions** tab.

### Method 2: Local Command Line Build
To compile the APK locally using the Android SDK:

```bash
# Clone repository
git clone <your-repo-url>
cd synapse

# Build Debug APK
gradle assembleDebug

# The generated APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

To install directly to a connected Android device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Permissions & Safety
* **Audio Track**: Runs in user space via standard Android `AudioTrack` API. No special runtime microphone permissions required for synthesis.
* **File Sharing**: Uses modern Android `FileProvider` with content URIs — zero storage permissions needed.
