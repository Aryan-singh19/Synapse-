package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PatchCable
import com.example.audio.PatchDestination
import com.example.audio.PatchSource
import kotlin.math.max

private val CABLE_PALETTE = listOf(
    0xFF00E5FF, // Cyan
    0xFFFF9100, // Amber
    0xFFFF4081, // Magenta
    0xFF00E676, // Emerald
    0xFF7C4DFF, // Purple
    0xFFFFEB3B  // Electric Yellow
)

@Composable
fun PatchBayView(
    cables: List<PatchCable>,
    onAddCable: (PatchSource, PatchDestination, Float, Long) -> Unit,
    onRemoveCable: (String) -> Unit,
    onUpdateCableAmount: (String, Float) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSource by remember { mutableStateOf<PatchSource?>(null) }
    var selectedDestination by remember { mutableStateOf<PatchDestination?>(null) }

    val sourceOffsets = remember { mutableStateMapOf<PatchSource, Offset>() }
    val destOffsets = remember { mutableStateMapOf<PatchDestination, Offset>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(12.dp)
    ) {
        // Modular Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModularSectionHeader(
                title = "MODULAR ROUTING MATRIX",
                badge = "${cables.size} PATCHED",
                accentColor = Color(0xFF00E5FF)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Instruction strip
        Surface(
            color = Color(0xFF141C2E),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        selectedSource != null -> "Selected [${selectedSource?.label}] -> Tap a destination socket below"
                        selectedDestination != null -> "Selected [${selectedDestination?.label}] -> Tap a source socket"
                        else -> "Tap a SOURCE socket, then an INPUT socket to connect a patch cable."
                    },
                    color = if (selectedSource != null || selectedDestination != null) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (selectedSource != null || selectedDestination != null) {
                    TextButton(
                        onClick = {
                            selectedSource = null
                            selectedDestination = null
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Cancel", color = Color(0xFFFF5252), fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Visual Patch Panel with Sockets and sagging Cable Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111726))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            // Cable Canvas Layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (cable in cables) {
                    val srcPos = sourceOffsets[cable.source]
                    val dstPos = destOffsets[cable.destination]
                    if (srcPos != null && dstPos != null) {
                        val path = Path()
                        path.moveTo(srcPos.x, srcPos.y)

                        // Eurorack cable gravity sag effect
                        val midX = (srcPos.x + dstPos.x) / 2f
                        val midY = max(srcPos.y, dstPos.y) + 38f
                        path.quadraticTo(midX, midY, dstPos.x, dstPos.y)

                        val cableColor = Color(cable.colorHex)

                        // Ambient glow
                        drawPath(
                            path = path,
                            color = cableColor.copy(alpha = 0.35f),
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )
                        // Core wire
                        drawPath(
                            path = path,
                            color = cableColor,
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Sockets Row Layout
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: SOURCES (Outputs)
                Column {
                    Text(
                        text = "CV MODULATION SOURCES (OUTPUTS)",
                        color = Color(0xFF00E5FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        PatchSource.values().forEach { source ->
                            val isSelected = selectedSource == source
                            val isConnected = cables.any { it.source == source }
                            JackSocket(
                                label = source.label,
                                isSelected = isSelected,
                                isConnected = isConnected,
                                ringColor = Color(0xFF00E5FF),
                                onPositioned = { offset -> sourceOffsets[source] = offset },
                                onClick = {
                                    if (selectedDestination != null) {
                                        val dest = selectedDestination!!
                                        val existing = cables.find { it.source == source && it.destination == dest }
                                        if (existing != null) {
                                            onRemoveCable(existing.id)
                                        } else {
                                            val color = CABLE_PALETTE[(cables.size) % CABLE_PALETTE.size]
                                            onAddCable(source, dest, 0.6f, color)
                                        }
                                        selectedSource = null
                                        selectedDestination = null
                                    } else {
                                        selectedSource = if (selectedSource == source) null else source
                                    }
                                }
                            )
                        }
                    }
                }

                // Bottom: DESTINATIONS (Inputs)
                Column {
                    Text(
                        text = "TARGET CV DESTINATIONS (INPUTS)",
                        color = Color(0xFFFF9100),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        PatchDestination.values().forEach { dest ->
                            val isSelected = selectedDestination == dest
                            val isConnected = cables.any { it.destination == dest }
                            JackSocket(
                                label = dest.label,
                                isSelected = isSelected,
                                isConnected = isConnected,
                                ringColor = Color(0xFFFF9100),
                                onPositioned = { offset -> destOffsets[dest] = offset },
                                onClick = {
                                    if (selectedSource != null) {
                                        val src = selectedSource!!
                                        val existing = cables.find { it.source == src && it.destination == dest }
                                        if (existing != null) {
                                            onRemoveCable(existing.id)
                                        } else {
                                            val color = CABLE_PALETTE[(cables.size) % CABLE_PALETTE.size]
                                            onAddCable(src, dest, 0.6f, color)
                                        }
                                        selectedSource = null
                                        selectedDestination = null
                                    } else {
                                        selectedDestination = if (selectedDestination == dest) null else dest
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cable List & Modulation Attenuators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE PATCH CABLES & ATTENUATORS",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (cables.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Clear All", color = Color(0xFFFF5252), fontSize = 11.sp)
                }
            }
        }

        if (cables.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No patch cables connected.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            val color1 = CABLE_PALETTE[0]
                            val color2 = CABLE_PALETTE[1]
                            onAddCable(PatchSource.LFO_OUT, PatchDestination.FILTER_CUTOFF, 0.7f, color1)
                            onAddCable(PatchSource.AMP_ENV, PatchDestination.DRIVE_GAIN, 0.5f, color2)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Default Modulation Patch", fontSize = 11.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cables, key = { it.id }) { cable ->
                    Surface(
                        color = Color(0xFF151D2A),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(cable.colorHex).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cable Color Pip
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(cable.colorHex))
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Route info
                            Column(modifier = Modifier.width(130.dp)) {
                                Text(
                                    text = "${cable.source.label} -> ${cable.destination.label}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Amt: ${(cable.amount * 100).toInt()}%",
                                    color = Color(cable.colorHex),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Attenuator Slider
                            Slider(
                                value = cable.amount,
                                onValueChange = { onUpdateCableAmount(cable.id, it) },
                                valueRange = -1.0f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(cable.colorHex),
                                    activeTrackColor = Color(cable.colorHex)
                                )
                            )

                            // Unplug button
                            IconButton(
                                onClick = { onRemoveCable(cable.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Unplug Cable",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3.5mm Eurorack Jack Socket Composable
 */
@Composable
private fun JackSocket(
    label: String,
    isSelected: Boolean,
    isConnected: Boolean,
    ringColor: Color,
    onPositioned: (Offset) -> Unit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .onGloballyPositioned { coordinates ->
                    val pos = coordinates.positionInParent()
                    onPositioned(Offset(pos.x + 16.dp.value, pos.y + 16.dp.value))
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Chrome socket nut
                drawCircle(
                    color = Color(0xFF475569),
                    radius = 14f,
                    center = center
                )
                // Outer ring
                drawCircle(
                    color = if (isSelected) ringColor else if (isConnected) ringColor.copy(alpha = 0.8f) else Color(0xFF1E293B),
                    radius = 12f,
                    center = center,
                    style = Stroke(width = if (isSelected) 3f else 2f)
                )
                // Inner dark hole
                drawCircle(
                    color = Color(0xFF0A0E17),
                    radius = 7f,
                    center = center
                )
                // Center contact pin
                drawCircle(
                    color = if (isConnected) ringColor else Color(0xFF334155),
                    radius = 2.5f,
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = if (isSelected) ringColor else Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
