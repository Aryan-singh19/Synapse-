package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.FilterType
import com.example.audio.SynthPatch
import com.example.audio.Waveform

@Composable
fun RackSynthView(
    patch: SynthPatch,
    onPatchChange: (SynthPatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D15))
            .verticalScroll(scrollState)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // MODULE 1: DUAL OSCILLATOR GENERATOR (VCO 1 & VCO 2)
        Surface(
            color = Color(0xFF101726),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().testTag("vco_module")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                ModularSectionHeader(
                    title = "VCO // DUAL OSCILLATOR SECTION",
                    badge = "${patch.osc1Waveform.label} + ${patch.osc2Waveform.label}",
                    accentColor = Color(0xFF00E5FF)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Osc 1 Controls
                Text(
                    text = "OSC 1 (PRIMARY)",
                    color = Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                WaveformSelector(
                    selected = patch.osc1Waveform,
                    onSelect = { onPatchChange(patch.copy(osc1Waveform = it)) },
                    accentColor = Color(0xFF00E5FF)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RotaryKnob(
                        value = patch.osc1Octave.toFloat(),
                        onValueChange = { onPatchChange(patch.copy(osc1Octave = it.toInt())) },
                        valueRange = -2f..2f,
                        label = "OCTAVE",
                        displayValue = "${patch.osc1Octave}",
                        indicatorColor = Color(0xFF00E5FF)
                    )
                    RotaryKnob(
                        value = patch.osc1Semi.toFloat(),
                        onValueChange = { onPatchChange(patch.copy(osc1Semi = it.toInt())) },
                        valueRange = -12f..12f,
                        label = "SEMI",
                        displayValue = "${patch.osc1Semi}st",
                        indicatorColor = Color(0xFF00E5FF)
                    )
                    RotaryKnob(
                        value = patch.osc1Fine,
                        onValueChange = { onPatchChange(patch.copy(osc1Fine = it)) },
                        valueRange = -0.5f..0.5f,
                        label = "FINE",
                        displayValue = "${(patch.osc1Fine * 100).toInt()}c",
                        indicatorColor = Color(0xFF00E5FF)
                    )
                    RotaryKnob(
                        value = patch.osc1Volume,
                        onValueChange = { onPatchChange(patch.copy(osc1Volume = it)) },
                        valueRange = 0f..1f,
                        label = "LEVEL",
                        displayValue = "${(patch.osc1Volume * 100).toInt()}%",
                        indicatorColor = Color(0xFF00E5FF)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Osc 2 Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OSC 2 (SECONDARY & DETUNE)",
                        color = Color(0xFFFF9100),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (patch.osc2Sync) Color(0xFFFF9100).copy(alpha = 0.3f) else Color(0xFF1E293B))
                            .clickable { onPatchChange(patch.copy(osc2Sync = !patch.osc2Sync)) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (patch.osc2Sync) "SYNC ON" else "SYNC OFF",
                            color = if (patch.osc2Sync) Color(0xFFFF9100) else Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                WaveformSelector(
                    selected = patch.osc2Waveform,
                    onSelect = { onPatchChange(patch.copy(osc2Waveform = it)) },
                    accentColor = Color(0xFFFF9100)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RotaryKnob(
                        value = patch.osc2Octave.toFloat(),
                        onValueChange = { onPatchChange(patch.copy(osc2Octave = it.toInt())) },
                        valueRange = -2f..2f,
                        label = "OCTAVE",
                        displayValue = "${patch.osc2Octave}",
                        indicatorColor = Color(0xFFFF9100)
                    )
                    RotaryKnob(
                        value = patch.osc2Semi.toFloat(),
                        onValueChange = { onPatchChange(patch.copy(osc2Semi = it.toInt())) },
                        valueRange = -12f..12f,
                        label = "SEMI",
                        displayValue = "${patch.osc2Semi}st",
                        indicatorColor = Color(0xFFFF9100)
                    )
                    RotaryKnob(
                        value = patch.osc2Fine,
                        onValueChange = { onPatchChange(patch.copy(osc2Fine = it)) },
                        valueRange = -0.5f..0.5f,
                        label = "DETUNE",
                        displayValue = "${(patch.osc2Fine * 100).toInt()}c",
                        indicatorColor = Color(0xFFFF9100)
                    )
                    RotaryKnob(
                        value = patch.osc2Volume,
                        onValueChange = { onPatchChange(patch.copy(osc2Volume = it)) },
                        valueRange = 0f..1f,
                        label = "LEVEL",
                        displayValue = "${(patch.osc2Volume * 100).toInt()}%",
                        indicatorColor = Color(0xFFFF9100)
                    )
                }
            }
        }

        // MODULE 2: RESONANT VCF (VOLTAGE CONTROLLED FILTER)
        Surface(
            color = Color(0xFF101726),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().testTag("vcf_module")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                ModularSectionHeader(
                    title = "VCF // RESONANT FILTER",
                    badge = "${patch.filterType.label} // ${patch.filterCutoff.toInt()}Hz",
                    accentColor = Color(0xFFFF4081)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Filter Mode Selector (LP / BP / HP)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF151D2A))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterType.values().forEach { mode ->
                        val isSel = mode == patch.filterType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) Color(0xFFFF4081).copy(alpha = 0.25f) else Color.Transparent)
                                .border(1.dp, if (isSel) Color(0xFFFF4081) else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { onPatchChange(patch.copy(filterType = mode)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mode.label} (${if (mode == FilterType.LOW_PASS) "Low Pass" else if (mode == FilterType.BAND_PASS) "Band Pass" else "High Pass"})",
                                color = if (isSel) Color(0xFFFF4081) else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RotaryKnob(
                        value = patch.filterCutoff,
                        onValueChange = { onPatchChange(patch.copy(filterCutoff = it)) },
                        valueRange = 40f..18000f,
                        isLogarithmic = true,
                        label = "CUTOFF",
                        displayValue = if (patch.filterCutoff > 1000f) "${(patch.filterCutoff / 1000f).let { String.format("%.1f", it) }}k" else "${patch.filterCutoff.toInt()}Hz",
                        indicatorColor = Color(0xFFFF4081)
                    )
                    RotaryKnob(
                        value = patch.filterResonance,
                        onValueChange = { onPatchChange(patch.copy(filterResonance = it)) },
                        valueRange = 0.3f..9.0f,
                        label = "RESONANCE",
                        displayValue = "Q ${String.format("%.1f", patch.filterResonance)}",
                        indicatorColor = Color(0xFFFF4081)
                    )
                    RotaryKnob(
                        value = patch.filterEnvAmount,
                        onValueChange = { onPatchChange(patch.copy(filterEnvAmount = it)) },
                        valueRange = -1.0f..1.0f,
                        label = "ENV AMT",
                        displayValue = "${(patch.filterEnvAmount * 100).toInt()}%",
                        indicatorColor = Color(0xFFFF4081)
                    )
                }
            }
        }

        // MODULE 3: DUAL ENVELOPES (AMP ADSR & FILTER ADSR)
        Surface(
            color = Color(0xFF101726),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                ModularSectionHeader(
                    title = "ENVELOPES // ADSR GENERATORS",
                    badge = "AMP & FILTER",
                    accentColor = Color(0xFF00E676)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Amp Envelope
                Text(
                    text = "AMPLITUDE ENVELOPE (VCA)",
                    color = Color(0xFF00E676),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                AdsrCurvePreview(
                    attack = patch.ampAttack,
                    decay = patch.ampDecay,
                    sustain = patch.ampSustain,
                    release = patch.ampRelease,
                    curveColor = Color(0xFF00E676)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RotaryKnob(
                        value = patch.ampAttack,
                        onValueChange = { onPatchChange(patch.copy(ampAttack = it)) },
                        valueRange = 0.002f..2.0f,
                        label = "ATTACK",
                        displayValue = "${(patch.ampAttack * 1000).toInt()}ms",
                        indicatorColor = Color(0xFF00E676)
                    )
                    RotaryKnob(
                        value = patch.ampDecay,
                        onValueChange = { onPatchChange(patch.copy(ampDecay = it)) },
                        valueRange = 0.01f..2.0f,
                        label = "DECAY",
                        displayValue = "${(patch.ampDecay * 1000).toInt()}ms",
                        indicatorColor = Color(0xFF00E676)
                    )
                    RotaryKnob(
                        value = patch.ampSustain,
                        onValueChange = { onPatchChange(patch.copy(ampSustain = it)) },
                        valueRange = 0.0f..1.0f,
                        label = "SUSTAIN",
                        displayValue = "${(patch.ampSustain * 100).toInt()}%",
                        indicatorColor = Color(0xFF00E676)
                    )
                    RotaryKnob(
                        value = patch.ampRelease,
                        onValueChange = { onPatchChange(patch.copy(ampRelease = it)) },
                        valueRange = 0.01f..3.0f,
                        label = "RELEASE",
                        displayValue = "${(patch.ampRelease * 1000).toInt()}ms",
                        indicatorColor = Color(0xFF00E676)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Envelope
                Text(
                    text = "FILTER ENVELOPE (VCF)",
                    color = Color(0xFFFF9100),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                AdsrCurvePreview(
                    attack = patch.filtAttack,
                    decay = patch.filtDecay,
                    sustain = patch.filtSustain,
                    release = patch.filtRelease,
                    curveColor = Color(0xFFFF9100)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RotaryKnob(
                        value = patch.filtAttack,
                        onValueChange = { onPatchChange(patch.copy(filtAttack = it)) },
                        valueRange = 0.002f..2.0f,
                        label = "ATTACK",
                        displayValue = "${(patch.filtAttack * 1000).toInt()}ms",
                        indicatorColor = Color(0xFFFF9100)
                    )
                    RotaryKnob(
                        value = patch.filtDecay,
                        onValueChange = { onPatchChange(patch.copy(filtDecay = it)) },
                        valueRange = 0.01f..2.0f,
                        label = "DECAY",
                        displayValue = "${(patch.filtDecay * 1000).toInt()}ms",
                        indicatorColor = Color(0xFFFF9100)
                    )
                    RotaryKnob(
                        value = patch.filtSustain,
                        onValueChange = { onPatchChange(patch.copy(filtSustain = it)) },
                        valueRange = 0.0f..1.0f,
                        label = "SUSTAIN",
                        displayValue = "${(patch.filtSustain * 100).toInt()}%",
                        indicatorColor = Color(0xFFFF9100)
                    )
                    RotaryKnob(
                        value = patch.filtRelease,
                        onValueChange = { onPatchChange(patch.copy(filtRelease = it)) },
                        valueRange = 0.01f..3.0f,
                        label = "RELEASE",
                        displayValue = "${(patch.filtRelease * 1000).toInt()}ms",
                        indicatorColor = Color(0xFFFF9100)
                    )
                }
            }
        }

        // MODULE 4: LFO MODULATOR & MASTER STUDIO FX
        Surface(
            color = Color(0xFF101726),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                ModularSectionHeader(
                    title = "MODULATION LFO & MASTER FX",
                    badge = "WARM ANALOG CHAIN",
                    accentColor = Color(0xFF7C4DFF)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // LFO Section
                Text(
                    text = "LFO 1 (LOW FREQUENCY OSCILLATOR)",
                    color = Color(0xFF7C4DFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                WaveformSelector(
                    selected = patch.lfoShape,
                    onSelect = { onPatchChange(patch.copy(lfoShape = it)) },
                    accentColor = Color(0xFF7C4DFF)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RotaryKnob(
                        value = patch.lfoRate,
                        onValueChange = { onPatchChange(patch.copy(lfoRate = it)) },
                        valueRange = 0.1f..20.0f,
                        label = "RATE",
                        displayValue = "${String.format("%.1f", patch.lfoRate)}Hz",
                        indicatorColor = Color(0xFF7C4DFF)
                    )
                    RotaryKnob(
                        value = patch.lfoDepth,
                        onValueChange = { onPatchChange(patch.copy(lfoDepth = it)) },
                        valueRange = 0.0f..1.0f,
                        label = "DEPTH",
                        displayValue = "${(patch.lfoDepth * 100).toInt()}%",
                        indicatorColor = Color(0xFF7C4DFF)
                    )
                    RotaryKnob(
                        value = patch.glideTime,
                        onValueChange = { onPatchChange(patch.copy(glideTime = it)) },
                        valueRange = 0.001f..0.3f,
                        label = "PORTA GLIDE",
                        displayValue = "${(patch.glideTime * 1000).toInt()}ms",
                        indicatorColor = Color(0xFF00E5FF)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Studio FX: Stereo Chorus Ensemble
                Text(
                    text = "DIMENSION CHORUS / STEREO ENSEMBLE",
                    color = Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RotaryKnob(
                        value = patch.chorusRate,
                        onValueChange = { onPatchChange(patch.copy(chorusRate = it)) },
                        valueRange = 0.2f..5.0f,
                        label = "CHORUS RATE",
                        displayValue = "${String.format("%.1f", patch.chorusRate)}Hz",
                        indicatorColor = Color(0xFF00E5FF)
                    )
                    RotaryKnob(
                        value = patch.chorusDepth,
                        onValueChange = { onPatchChange(patch.copy(chorusDepth = it)) },
                        valueRange = 0.0f..1.0f,
                        label = "DEPTH",
                        displayValue = "${(patch.chorusDepth * 100).toInt()}%",
                        indicatorColor = Color(0xFF00E5FF)
                    )
                    RotaryKnob(
                        value = patch.chorusMix,
                        onValueChange = { onPatchChange(patch.copy(chorusMix = it)) },
                        valueRange = 0.0f..0.8f,
                        label = "CHORUS WET",
                        displayValue = "${(patch.chorusMix * 100).toInt()}%",
                        indicatorColor = Color(0xFF00E5FF)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Studio FX: Ping-Pong Delay & Drive
                Text(
                    text = "STUDIO EFFECTS CHAIN (PING-PONG DELAY & TAPE DRIVE)",
                    color = Color(0xFFFF9100),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RotaryKnob(
                        value = patch.delayTime,
                        onValueChange = { onPatchChange(patch.copy(delayTime = it)) },
                        valueRange = 0.05f..0.8f,
                        label = "DELAY TIME",
                        displayValue = "${(patch.delayTime * 1000).toInt()}ms",
                        indicatorColor = Color(0xFFFF9100)
                    )
                    RotaryKnob(
                        value = patch.delayFeedback,
                        onValueChange = { onPatchChange(patch.copy(delayFeedback = it)) },
                        valueRange = 0.0f..0.85f,
                        label = "FEEDBACK",
                        displayValue = "${(patch.delayFeedback * 100).toInt()}%",
                        indicatorColor = Color(0xFFFF9100)
                    )
                    RotaryKnob(
                        value = patch.delayMix,
                        onValueChange = { onPatchChange(patch.copy(delayMix = it)) },
                        valueRange = 0.0f..0.8f,
                        label = "DELAY WET",
                        displayValue = "${(patch.delayMix * 100).toInt()}%",
                        indicatorColor = Color(0xFFFF9100)
                    )
                    RotaryKnob(
                        value = patch.driveGain,
                        onValueChange = { onPatchChange(patch.copy(driveGain = it)) },
                        valueRange = 1.0f..6.0f,
                        label = "OVERDRIVE",
                        displayValue = "${String.format("%.1f", patch.driveGain)}x",
                        indicatorColor = Color(0xFFFF4081)
                    )
                }
            }
        }
    }
}
