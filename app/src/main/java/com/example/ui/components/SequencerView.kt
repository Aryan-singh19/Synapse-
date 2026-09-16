package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.MusicalScale
import com.example.sequencer.PlaybackDirection
import com.example.sequencer.SequencerStep
import com.example.sequencer.StepSequencer

@Composable
fun SequencerView(
    steps: List<SequencerStep>,
    currentStep: Int,
    isPlaying: Boolean,
    bpm: Int,
    swing: Float,
    selectedScale: MusicalScale,
    direction: PlaybackDirection = PlaybackDirection.FORWARD,
    gateLength: Float = 0.8f,
    onTogglePlay: () -> Unit,
    onSetBpm: (Int) -> Unit,
    onSetSwing: (Float) -> Unit,
    onSetScale: (MusicalScale) -> Unit,
    onSetDirection: (PlaybackDirection) -> Unit = {},
    onSetGateLength: (Float) -> Unit = {},
    onTranspose: (Int) -> Unit = {},
    onClearAll: () -> Unit = {},
    onInvert: () -> Unit = {},
    onShift: (Int) -> Unit = {},
    onToggleStep: (Int) -> Unit,
    onStepNoteChange: (Int, Int) -> Unit,
    onToggleAccent: (Int) -> Unit,
    onMutate: () -> Unit,
    onEuclidean: (Int) -> Unit,
    onRandomize: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }
    var showScaleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .padding(10.dp)
    ) {
        // Module Header & Master Transport
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModularSectionHeader(
                title = "16-STEP MELODIC SEQUENCER",
                badge = "${bpm} BPM // ${selectedScale.label}",
                accentColor = Color(0xFF00E676)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Transport & Clock Bar
        Surface(
            color = Color(0xFF131A29),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play / Pause Button
                Button(
                    onClick = onTogglePlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFF00E676) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("seq_play_pause_btn")
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "STOP" else "RUN",
                        color = if (isPlaying) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                // BPM Dial Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onSetBpm(bpm - 5) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-5", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    IconButton(
                        onClick = { onSetBpm(bpm - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease BPM", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "$bpm",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { onSetBpm(bpm + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase BPM", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { onSetBpm(bpm + 5) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+5", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // Scale Selector Button
                OutlinedButton(
                    onClick = { showScaleDialog = true },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = selectedScale.label.take(12),
                        color = Color(0xFF00E676),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Generative Lab Tools
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onMutate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).testTag("btn_mutate")
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF00E5FF))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mutate", fontSize = 10.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = { onEuclidean(5) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).testTag("btn_euclidean")
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF9100))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Euclid 5", fontSize = 10.sp, color = Color(0xFFFF9100), fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = onRandomize,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).testTag("btn_randomize")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF4081))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gen Riff", fontSize = 10.sp, color = Color(0xFFFF4081), fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Advanced Pattern & Direction Strip
        Surface(
            color = Color(0xFF0F1626),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Direction Modes
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DIR:", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    PlaybackDirection.values().forEach { dir ->
                        val isDirActive = direction == dir
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDirActive) Color(0xFF00E676).copy(alpha = 0.25f) else Color(0xFF192233))
                                .border(1.dp, if (isDirActive) Color(0xFF00E676) else Color(0xFF26334D), RoundedCornerShape(4.dp))
                                .clickable { onSetDirection(dir) }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(dir.label, fontSize = 9.sp, color = if (isDirActive) Color(0xFF00E676) else Color(0xFF94A3B8), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Transpose & Clear tools
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF192233))
                            .border(1.dp, Color(0xFF26334D), RoundedCornerShape(4.dp))
                            .clickable { onTranspose(-12) }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("-OCT", fontSize = 9.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF192233))
                            .border(1.dp, Color(0xFF26334D), RoundedCornerShape(4.dp))
                            .clickable { onTranspose(12) }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("+OCT", fontSize = 9.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF221520))
                            .border(1.dp, Color(0xFF4A1A2C), RoundedCornerShape(4.dp))
                            .clickable { onClearAll() }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("CLEAR", fontSize = 9.sp, color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 16-Step Grid (2 rows of 8 steps)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(steps, key = { it.index }) { step ->
                val isCurrent = currentStep == step.index
                val noteLabel = StepSequencer.getNoteLabel(step.midiNote)

                Surface(
                    color = when {
                        isCurrent && step.enabled -> Color(0xFF00E5FF).copy(alpha = 0.35f)
                        isCurrent -> Color(0xFF334155).copy(alpha = 0.4f)
                        step.enabled -> Color(0xFF1A2333)
                        else -> Color(0xFF0F141F)
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isCurrent) 2.dp else 1.dp,
                        when {
                            isCurrent -> Color.White
                            step.enabled -> if (step.accent) Color(0xFFFF9100) else Color(0xFF00E5FF).copy(alpha = 0.6f)
                            else -> Color(0xFF1E293B)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clickable { onToggleStep(step.index) }
                        .testTag("step_${step.index}")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Step Index & LED
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${step.index + 1}",
                                fontSize = 9.sp,
                                color = if (isCurrent) Color.White else Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            // Status LED
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrent && step.enabled -> Color.White
                                            step.enabled && step.accent -> Color(0xFFFF9100)
                                            step.enabled -> Color(0xFF00E5FF)
                                            else -> Color(0xFF1E293B)
                                        }
                                    )
                            )
                        }

                        // Pitch Note Label (tap to adjust)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (step.enabled) noteLabel else "--",
                                fontSize = 13.sp,
                                color = if (step.enabled) {
                                    if (step.accent) Color(0xFFFF9100) else Color(0xFF00E5FF)
                                } else Color(0xFF475569),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Bottom row: Accent indicator + Quick pitch nudgers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Accent toggle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (step.accent) Color(0xFFFF9100).copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable { onToggleAccent(step.index) }
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (step.accent) "ACC" else "acc",
                                    fontSize = 8.sp,
                                    color = if (step.accent) Color(0xFFFF9100) else Color(0xFF475569),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Pitch adjustments (+/- semitone)
                            Row {
                                Box(
                                    modifier = Modifier
                                        .clickable { onStepNoteChange(step.index, step.midiNote - 1) }
                                        .padding(2.dp)
                                ) {
                                    Text("▼", fontSize = 8.sp, color = Color(0xFF64748B))
                                }
                                Box(
                                    modifier = Modifier
                                        .clickable { onStepNoteChange(step.index, step.midiNote + 1) }
                                        .padding(2.dp)
                                ) {
                                    Text("▲", fontSize = 8.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Scale Dialog Picker
    if (showScaleDialog) {
        AlertDialog(
            onDismissRequest = { showScaleDialog = false },
            title = {
                Text(
                    "SELECT MUSICAL SCALE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MusicalScale.values().forEach { scale ->
                        Surface(
                            color = if (scale == selectedScale) Color(0xFF00E676).copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetScale(scale)
                                    showScaleDialog = false
                                }
                        ) {
                            Text(
                                text = scale.label,
                                color = if (scale == selectedScale) Color(0xFF00E676) else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScaleDialog = false }) {
                    Text("Close", color = Color(0xFF00E676))
                }
            },
            containerColor = Color(0xFF141C2E)
        )
    }
}
