package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presets.PresetBank
import com.example.ui.StudioTab
import com.example.ui.SynapseViewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SynapseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SynapseApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SynapseApp(viewModel: SynapseViewModel) {
    val currentPatch by viewModel.currentPatch.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val baseOctave by viewModel.baseOctave.collectAsStateWithLifecycle()
    val waveform by viewModel.waveformSnapshot.collectAsStateWithLifecycle()
    val peakRms by viewModel.peakRms.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()

    // Sequencer states
    val isPlaying by viewModel.sequencer.isPlaying.collectAsStateWithLifecycle()
    val currentStep by viewModel.sequencer.currentStep.collectAsStateWithLifecycle()
    val bpm by viewModel.sequencer.bpm.collectAsStateWithLifecycle()
    val swing by viewModel.sequencer.swing.collectAsStateWithLifecycle()
    val steps by viewModel.sequencer.steps.collectAsStateWithLifecycle()
    val selectedScale by viewModel.sequencer.selectedScale.collectAsStateWithLifecycle()

    var showPresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A11)),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF070A11))
        ) {
            // TOP HEADER BAR: Logo + Patch Selector + Panic
            Surface(
                color = Color(0xFF0D1322),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title Branding
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying || peakRms > 0.05f) Color(0xFF00E5FF) else Color(0xFF334155))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SYNAPSE",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "MODULAR LAB // AUDIO ENGINE",
                                color = Color(0xFF00E5FF),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Patch Selector Pill
                    OutlinedButton(
                        onClick = { showPresetDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("preset_selector_btn")
                    ) {
                        Icon(
                            Icons.Default.LibraryMusic,
                            contentDescription = "Presets",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentPatch.name,
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Panic Button (Kill Stuck Notes / Audio Reset)
                    IconButton(
                        onClick = { viewModel.panic() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF261824))
                            .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .testTag("btn_panic")
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = "Panic / All Notes Off",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // LIVE REAL-TIME OSCILLOSCOPE & VU METER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                OscilloscopeView(
                    waveform = waveform,
                    peakRms = peakRms,
                    traceColor = Color(0xFF00E5FF)
                )
            }

            // STUDIO RACK NAVIGATION TABS
            Surface(
                color = Color(0xFF0E1422),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StudioTab.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        val accentColor = when (tab) {
                            StudioTab.RACK -> Color(0xFF00E5FF)
                            StudioTab.SEQUENCER -> Color(0xFF00E676)
                            StudioTab.PATCH_BAY -> Color(0xFFFF9100)
                            StudioTab.PERFORMANCE -> Color(0xFFFF4081)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.2f)
                                    else Color(0xFF131A29)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) accentColor else Color(0xFF1E293B),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.setTab(tab) }
                                .testTag("tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) accentColor else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = tab.iconBadge,
                                    color = if (isSelected) accentColor.copy(alpha = 0.8f) else Color(0xFF64748B),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // MAIN CONTENT VIEW AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    StudioTab.RACK -> {
                        RackSynthView(
                            patch = currentPatch,
                            onPatchChange = { viewModel.updatePatch(it) }
                        )
                    }
                    StudioTab.SEQUENCER -> {
                        SequencerView(
                            steps = steps,
                            currentStep = currentStep,
                            isPlaying = isPlaying,
                            bpm = bpm,
                            swing = swing,
                            selectedScale = selectedScale,
                            onTogglePlay = { viewModel.sequencer.togglePlayback() },
                            onSetBpm = { viewModel.sequencer.setBpm(it) },
                            onSetSwing = { viewModel.sequencer.setSwing(it) },
                            onSetScale = { viewModel.sequencer.setScale(it) },
                            onToggleStep = { viewModel.sequencer.toggleStep(it) },
                            onStepNoteChange = { idx, note -> viewModel.sequencer.setStepNote(idx, note) },
                            onToggleAccent = { viewModel.sequencer.toggleAccent(it) },
                            onMutate = { viewModel.sequencer.mutate() },
                            onEuclidean = { viewModel.sequencer.generateEuclidean(it) },
                            onRandomize = { viewModel.sequencer.randomizeAll() }
                        )
                    }
                    StudioTab.PATCH_BAY -> {
                        PatchBayView(
                            cables = currentPatch.cables,
                            onAddCable = { src, dst, amt, color -> viewModel.addPatchCable(src, dst, amt, color) },
                            onRemoveCable = { viewModel.removePatchCable(it) },
                            onUpdateCableAmount = { id, amt -> viewModel.updatePatchCableAmount(id, amt) },
                            onClearAll = { viewModel.clearAllPatchCables() }
                        )
                    }
                    StudioTab.PERFORMANCE -> {
                        PerformanceView(
                            baseOctave = baseOctave,
                            onOctaveChange = { viewModel.setBaseOctave(it) },
                            onNoteOn = { note, vel -> viewModel.noteOn(note, vel) },
                            onNoteOff = { note -> viewModel.noteOff(note) },
                            filterCutoff = currentPatch.filterCutoff,
                            filterResonance = currentPatch.filterResonance,
                            onXyPadChange = { cut, res -> viewModel.updateXyPad(cut, res) }
                        )
                    }
                }
            }
        }
    }

    // PRESET SELECTOR & SAVE MODAL DIALOG
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = {
                Text(
                    "SYNTHESIS PRESETS & SOUND BANK",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save custom patch input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            placeholder = { Text("Custom Patch Name", fontSize = 11.sp, color = Color(0xFF64748B)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newPresetName.isNotBlank()) {
                                    viewModel.saveCurrentAsCustom(newPresetName)
                                    newPresetName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Divider(color = Color(0xFF1E293B))

                    Text(
                        text = "FACTORY PRESET BANK",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    PresetBank.factoryPresets.forEach { preset ->
                        val isSelected = currentPatch.name == preset.name
                        Surface(
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF131A29),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadPreset(preset)
                                    showPresetDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = preset.name,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${preset.osc1Waveform.label} + ${preset.osc2Waveform.label} // ${preset.filterType.label}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (isSelected) {
                                    Text("ACTIVE", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    if (customPresets.isNotEmpty()) {
                        Divider(color = Color(0xFF1E293B))
                        Text(
                            text = "USER CUSTOM PATCHES",
                            color = Color(0xFFFF9100),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        customPresets.forEach { preset ->
                            val isSelected = currentPatch.name == preset.name
                            Surface(
                                color = if (isSelected) Color(0xFFFF9100).copy(alpha = 0.2f) else Color(0xFF131A29),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF9100) else Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadPreset(preset)
                                        showPresetDialog = false
                                    }
                            ) {
                                Text(
                                    text = preset.name,
                                    color = if (isSelected) Color(0xFFFF9100) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("Close", color = Color(0xFF00E5FF))
                }
            },
            containerColor = Color(0xFF101726)
        )
    }
}
