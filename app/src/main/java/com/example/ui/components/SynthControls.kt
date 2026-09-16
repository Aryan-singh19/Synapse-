package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.Waveform
import kotlin.math.*

/**
 * Tactile Eurorack-style Rotary Knob with drag sensitivity, glowing notch, and value readouts
 */
@Composable
fun RotaryKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    label: String,
    displayValue: String,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    indicatorColor: Color = Color(0xFF00E5FF),
    isLogarithmic: Boolean = false
) {
    val normalizedValue = remember(value, valueRange, isLogarithmic) {
        if (isLogarithmic) {
            val minLog = ln(valueRange.start.coerceAtLeast(1e-4f))
            val maxLog = ln(valueRange.endInclusive.coerceAtLeast(1e-4f))
            val currentLog = ln(value.coerceIn(valueRange.start, valueRange.endInclusive).coerceAtLeast(1e-4f))
            ((currentLog - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)
        } else {
            ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        }
    }

    // Angle range: -135 deg to +135 deg (total 270 deg)
    val angle = -135f + (normalizedValue * 270f)

    Column(
        modifier = modifier.testTag("knob_${label.lowercase().replace(" ", "_")}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(valueRange, isLogarithmic) {
                    var accumulatedDrag = 0f
                    detectDragGestures(
                        onDragStart = { accumulatedDrag = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDrag -= dragAmount.y // Drag up to increase, down to decrease
                            val sensitivity = 200f
                            val delta = accumulatedDrag / sensitivity
                            accumulatedDrag = 0f

                            val newNorm = (normalizedValue + delta).coerceIn(0f, 1f)
                            val newValue = if (isLogarithmic) {
                                val minLog = ln(valueRange.start.coerceAtLeast(1e-4f))
                                val maxLog = ln(valueRange.endInclusive.coerceAtLeast(1e-4f))
                                exp(minLog + newNorm * (maxLog - minLog))
                            } else {
                                valueRange.start + newNorm * (valueRange.endInclusive - valueRange.start)
                            }
                            onValueChange(newValue)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = this.size.minDimension / 2f
                val center = Offset(this.size.width / 2f, this.size.height / 2f)

                // Background track arc
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius + 5f, center.y - radius + 5f),
                    size = androidx.compose.ui.geometry.Size((radius - 5f) * 2f, (radius - 5f) * 2f),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Active glowing value arc
                drawArc(
                    color = indicatorColor.copy(alpha = 0.85f),
                    startAngle = 135f,
                    sweepAngle = (normalizedValue * 270f),
                    useCenter = false,
                    topLeft = Offset(center.x - radius + 5f, center.y - radius + 5f),
                    size = androidx.compose.ui.geometry.Size((radius - 5f) * 2f, (radius - 5f) * 2f),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Knob metal body
                val bodyRadius = radius - 10f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF334155), Color(0xFF1E2230), Color(0xFF0F172A)),
                        center = center,
                        radius = bodyRadius
                    ),
                    radius = bodyRadius,
                    center = center
                )

                // Outer metallic bevel ring
                drawCircle(
                    color = Color(0xFF475569),
                    radius = bodyRadius,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // Indicator needle/tick
                val rad = Math.toRadians((angle - 90f).toDouble())
                val startDist = bodyRadius * 0.4f
                val endDist = bodyRadius * 0.88f

                val startX = center.x + (cos(rad) * startDist).toFloat()
                val startY = center.y + (sin(rad) * startDist).toFloat()
                val endX = center.x + (cos(rad) * endDist).toFloat()
                val endY = center.y + (sin(rad) * endDist).toFloat()

                drawLine(
                    color = indicatorColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = displayValue,
            style = MaterialTheme.typography.labelSmall,
            color = indicatorColor.copy(alpha = 0.9f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Real-time Oscilloscope & Phosphor Waveform Monitor with VU Peak Meter
 */
@Composable
fun OscilloscopeView(
    waveform: FloatArray,
    peakRms: Float,
    modifier: Modifier = Modifier,
    traceColor: Color = Color(0xFF00E5FF)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF07090E))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(4.dp)
            .testTag("oscilloscope_screen")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            // Grid lines (Cathode Ray Tube / Scope aesthetic)
            val gridColor = Color(0xFF0F1E36)
            for (x in 0..8) {
                val gx = width * (x / 8f)
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, height), strokeWidth = 1f)
            }
            for (y in 0..4) {
                val gy = height * (y / 4f)
                drawLine(gridColor, Offset(0f, gy), Offset(width, gy), strokeWidth = 1f)
            }
            // Center crosshair
            drawLine(Color(0xFF1E3A5F), Offset(0f, midY), Offset(width, midY), strokeWidth = 1.2f)

            if (waveform.isNotEmpty()) {
                val path = Path()
                val step = width / (waveform.size - 1).coerceAtLeast(1)

                val firstY = midY - (waveform[0] * midY * 0.9f).coerceIn(-midY * 0.95f, midY * 0.95f)
                path.moveTo(0f, firstY)

                for (i in 1 until waveform.size) {
                    val x = i * step
                    val y = midY - (waveform[i] * midY * 0.9f).coerceIn(-midY * 0.95f, midY * 0.95f)
                    path.lineTo(x, y)
                }

                // Ambient glow layer
                drawPath(
                    path = path,
                    color = traceColor.copy(alpha = 0.35f),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Sharp center trace
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // VU Peak Level Bar Overlay on right edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .width(5.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1E293B))
        ) {
            val levelRatio = (peakRms * 2.5f).coerceIn(0f, 1f)
            val vuColor = when {
                levelRatio > 0.85f -> Color(0xFFFF1744) // Red clipping warning
                levelRatio > 0.6f -> Color(0xFFFF9100)  // Amber
                else -> Color(0xFF00E676)              // Green
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(levelRatio)
                    .background(vuColor)
            )
        }

        // HUD Labels
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (peakRms > 0.01f) Color(0xFF00E5FF) else Color(0xFF334155))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "OSC // LIVE FFT 44.1kHz",
                color = Color(0xFF64748B),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Waveform Shape Selector Pill Button Group
 */
@Composable
fun WaveformSelector(
    selected: Waveform,
    onSelect: (Waveform) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00E5FF)
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF111827))
            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(6.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Waveform.values().forEach { wave ->
            val isSelected = wave == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.25f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isSelected) accentColor else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .pointerInput(wave) {
                        detectDragGestures(
                            onDragStart = { onSelect(wave) },
                            onDrag = { _, _ -> }
                        )
                    }
                    .testTag("wave_btn_${wave.label}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = wave.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) accentColor else Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Visual ADSR Curve graphic
 */
@Composable
fun AdsrCurvePreview(
    attack: Float,
    decay: Float,
    sustain: Float,
    release: Float,
    modifier: Modifier = Modifier,
    curveColor: Color = Color(0xFF00E5FF)
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0D121F))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val totalTime = attack + decay + 0.3f + release
            val aW = (attack / totalTime) * w
            val dW = (decay / totalTime) * w
            val sW = (0.3f / totalTime) * w
            val rW = (release / totalTime) * w

            val path = Path()
            path.moveTo(0f, h)
            // Attack to top
            path.lineTo(aW, 2f)
            // Decay to sustain
            val sustY = h - (sustain * (h - 4f))
            path.lineTo(aW + dW, sustY)
            // Sustain hold
            path.lineTo(aW + dW + sW, sustY)
            // Release to bottom
            path.lineTo(w, h)

            // Fill area
            val fillPath = Path()
            fillPath.addPath(path)
            fillPath.lineTo(w, h)
            fillPath.lineTo(0f, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(curveColor.copy(alpha = 0.3f), Color.Transparent)
                )
            )

            // Line stroke
            drawPath(
                path = path,
                color = curveColor,
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Eurorack module header with chrome screws and title plate
 */
@Composable
fun ModularSectionHeader(
    title: String,
    badge: String? = null,
    accentColor: Color = Color(0xFF00E5FF)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Hex bolt / screw
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF475569))
                    .border(0.8.dp, Color(0xFF64748B), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        if (badge != null) {
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = badge,
                    color = accentColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
