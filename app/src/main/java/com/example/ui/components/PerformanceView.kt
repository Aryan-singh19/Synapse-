package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sequencer.StepSequencer

@Composable
fun PerformanceView(
    baseOctave: Int,
    onOctaveChange: (Int) -> Unit,
    onNoteOn: (Int, Float) -> Unit,
    onNoteOff: (Int) -> Unit,
    filterCutoff: Float,
    filterResonance: Float,
    onXyPadChange: (cutoff: Float, resonance: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var xyTouchPos by remember { mutableStateOf<Offset?>(null) }
    var isTouchingXy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D15))
            .padding(10.dp)
    ) {
        // Module Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModularSectionHeader(
                title = "LIVE PERFORMANCE & XY PAD",
                badge = "OCTAVE $baseOctave",
                accentColor = Color(0xFFFF9100)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // XY Kaoss-Style Touch Controller
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D1322))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            isTouchingXy = true
                            xyTouchPos = offset
                            val normX = (offset.x / size.width).coerceIn(0f, 1f)
                            val normY = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                            // Cutoff: 80Hz - 16000Hz (exponential curve)
                            val newCutoff = 80f * kotlin.math.exp(normX * kotlin.math.ln(16000f / 80f))
                            val newRes = 0.5f + normY * 8.5f
                            onXyPadChange(newCutoff, newRes)
                            tryAwaitRelease()
                            isTouchingXy = false
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isTouchingXy = true
                            xyTouchPos = offset
                        },
                        onDragEnd = { isTouchingXy = false },
                        onDragCancel = { isTouchingXy = false },
                        onDrag = { change, _ ->
                            change.consume()
                            xyTouchPos = change.position
                            val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                            val normY = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                            val newCutoff = 80f * kotlin.math.exp(normX * kotlin.math.ln(16000f / 80f))
                            val newRes = 0.5f + normY * 8.5f
                            onXyPadChange(newCutoff, newRes)
                        }
                    )
                }
                .testTag("xy_performance_pad")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Radial ambient glow
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.12f), Color.Transparent),
                        center = xyTouchPos ?: Offset(w / 2f, h / 2f),
                        radius = w * 0.6f
                    )
                )

                // Grid divisions
                val gridColor = Color(0xFF162238)
                for (i in 1..7) {
                    val x = w * (i / 8f)
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                }
                for (j in 1..4) {
                    val y = h * (j / 5f)
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                // Laser target crosshairs
                val currentPoint = xyTouchPos ?: Offset(
                    w * (kotlin.math.ln(filterCutoff.coerceAtLeast(80f) / 80f) / kotlin.math.ln(16000f / 80f)).coerceIn(0f, 1f),
                    h * (1f - ((filterResonance - 0.5f) / 8.5f).coerceIn(0f, 1f))
                )

                // Crosshair lines
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                    start = Offset(0f, currentPoint.y),
                    end = Offset(w, currentPoint.y),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = Color(0xFFFF9100).copy(alpha = 0.4f),
                    start = Offset(currentPoint.x, 0f),
                    end = Offset(currentPoint.x, h),
                    strokeWidth = 1.2f
                )

                // Reticle ring
                drawCircle(
                    color = if (isTouchingXy) Color(0xFF00E5FF) else Color(0xFF00E5FF).copy(alpha = 0.6f),
                    radius = if (isTouchingXy) 22f else 14f,
                    center = currentPoint,
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = currentPoint
                )
            }

            // HUD Labels
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "X: CUTOFF ${filterCutoff.toInt()}Hz",
                    color = Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Y: RES ${String.format("%.1f", filterResonance)}",
                    color = Color(0xFFFF9100),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "DRAG / SLIDE TO MODULATE FILTER",
                color = Color(0xFF475569),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(6.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Octave & Pitch Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onOctaveChange((baseOctave - 1).coerceAtLeast(1)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.size(height = 32.dp, width = 64.dp)
                ) {
                    Text("OCT -", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "C$baseOctave",
                    color = Color(0xFFFF9100),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = { onOctaveChange((baseOctave + 1).coerceAtMost(6)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.size(height = 32.dp, width = 64.dp)
                ) {
                    Text("OCT +", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }
            }

            Text(
                text = "POLYPHONIC TOUCH KEYS",
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Multi-touch Piano Keyboard (14 natural keys: 2 full octaves)
        val startMidi = (baseOctave + 1) * 12 // C3 = 48 when baseOctave = 3
        PianoKeyboard(
            startMidiNote = startMidi,
            numKeys = 14,
            onNoteOn = onNoteOn,
            onNoteOff = onNoteOff,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        )
    }
}

/**
 * Interactive Dual-Octave Piano Keyboard
 */
@Composable
private fun PianoKeyboard(
    startMidiNote: Int,
    numKeys: Int,
    onNoteOn: (Int, Float) -> Unit,
    onNoteOff: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val whiteKeyOffsets = listOf(0, 2, 4, 5, 7, 9, 11)
    val activeNotes = remember { mutableStateListOf<Int>() }

    // Black key definitions: (white key index after which black key appears, semitone offset in octave)
    val blackKeyDefs = listOf(0 to 1, 1 to 3, 3 to 6, 4 to 8, 5 to 10)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F1420))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(2.dp)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val whiteKeyWidth = totalWidth / numKeys
        val blackKeyWidth = whiteKeyWidth * 0.66f
        val blackKeyHeight = totalHeight * 0.58f

        // White Keys Layer
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            for (i in 0 until numKeys) {
                val octaveOffset = (i / 7) * 12
                val noteInOctave = whiteKeyOffsets[i % 7]
                val midiNote = startMidiNote + octaveOffset + noteInOctave
                val isPressed = activeNotes.contains(midiNote)
                val label = StepSequencer.getNoteLabel(midiNote)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(
                            if (isPressed) Color(0xFF00E5FF)
                            else Color(0xFFE2E8F0)
                        )
                        .pointerInput(midiNote) {
                            detectTapGestures(
                                onPress = {
                                    activeNotes.add(midiNote)
                                    onNoteOn(midiNote, 0.85f)
                                    tryAwaitRelease()
                                    activeNotes.remove(midiNote)
                                    onNoteOff(midiNote)
                                }
                            )
                        }
                        .testTag("key_$label"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = label,
                        color = if (isPressed) Color.Black else Color(0xFF475569),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }

        // Black Keys Layer (Overlay with precise absolute offsets)
        val octaves = (numKeys / 7) + 1
        for (oct in 0 until octaves) {
            for ((whiteIndex, semiOffset) in blackKeyDefs) {
                val globalWhiteIdx = (oct * 7) + whiteIndex
                if (globalWhiteIdx < numKeys - 1) {
                    val midiNote = startMidiNote + (oct * 12) + semiOffset
                    val isPressed = activeNotes.contains(midiNote)
                    val label = StepSequencer.getNoteLabel(midiNote)
                    val xOffset = (whiteKeyWidth * (globalWhiteIdx + 1)) - (blackKeyWidth / 2f)

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = 0.dp)
                            .width(blackKeyWidth)
                            .height(blackKeyHeight)
                            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(
                                if (isPressed) Color(0xFFFF9100)
                                else Color(0xFF181C26)
                            )
                            .border(0.8.dp, Color(0xFF334155), RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .pointerInput(midiNote) {
                                detectTapGestures(
                                    onPress = {
                                        activeNotes.add(midiNote)
                                        onNoteOn(midiNote, 0.95f)
                                        tryAwaitRelease()
                                        activeNotes.remove(midiNote)
                                        onNoteOff(midiNote)
                                    }
                                )
                            }
                            .testTag("key_$label"),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = label,
                            color = if (isPressed) Color.Black else Color(0xFF94A3B8),
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
