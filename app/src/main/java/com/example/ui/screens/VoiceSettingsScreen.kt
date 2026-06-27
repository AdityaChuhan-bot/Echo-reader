package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.PremiumGold

@Composable
fun VoiceSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val book by viewModel.currentBook.collectAsState()
    val voices = viewModel.ttsManager.availableVoices

    var filterAccent by remember { mutableStateOf("All") } // All, American, British, Indian
    val filteredVoices = if (filterAccent == "All") voices else voices.filter { it.accent == filterAccent }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("voices_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GlassTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI Voice Library",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Neural Model Status Dashboard
        var kokoroProgress by remember { mutableStateOf(viewModel.ttsManager.getModelDownloadProgress("KOKORO")) }
        var piperProgress by remember { mutableStateOf(viewModel.ttsManager.getModelDownloadProgress("PIPER")) }
        var isDownloadingKokoro by remember { mutableStateOf(false) }
        var isDownloadingPiper by remember { mutableStateOf(false) }

        val isKokoroDownloaded = viewModel.ttsManager.isModelDownloaded("KOKORO")
        val isPiperDownloaded = viewModel.ttsManager.isModelDownloaded("PIPER")
        val isEfficient = viewModel.ttsManager.isKokoroSupportedEfficiently()

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF0F172A).copy(alpha = 0.5f),
            borderColor = NeonPurple
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "On-Device Neural Speech Manager",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Device CPU profile: ${if (isEfficient) "High Performance (Kokoro Ready)" else "Standard Profile (Piper Fallback Recommended)"}",
                    fontSize = 11.sp,
                    color = if (isEfficient) NeonCyan else PremiumGold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Kokoro Model Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Kokoro Neural Model v1.0 (78 MB)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTextPrimary
                        )
                        Text(
                            text = if (isKokoroDownloaded) "Installed fully offline" else if (isDownloadingKokoro) "Downloading... ${"%.0f".format(kokoroProgress * 100)}%" else "Available for offline use",
                            fontSize = 10.sp,
                            color = GlassTextSecondary
                        )
                    }
                    if (isKokoroDownloaded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Active", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    } else if (isDownloadingKokoro) {
                        CircularProgressIndicator(
                            progress = kokoroProgress,
                            modifier = Modifier.size(18.dp),
                            color = NeonPurple,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonPurple)
                                .clickable {
                                    isDownloadingKokoro = true
                                    viewModel.ttsManager.downloadModel("KOKORO", { progress ->
                                        kokoroProgress = progress
                                    }, {
                                        isDownloadingKokoro = false
                                    })
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Download", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isDownloadingKokoro) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = kokoroProgress,
                        modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                        color = NeonPurple,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Piper Model Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Piper Neural Model v0.2 (24 MB)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTextPrimary
                        )
                        Text(
                            text = if (isPiperDownloaded) "Installed fully offline" else if (isDownloadingPiper) "Downloading... ${"%.0f".format(piperProgress * 100)}%" else "Available for efficient fallback",
                            fontSize = 10.sp,
                            color = GlassTextSecondary
                        )
                    }
                    if (isPiperDownloaded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Active", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    } else if (isDownloadingPiper) {
                        CircularProgressIndicator(
                            progress = piperProgress,
                            modifier = Modifier.size(18.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    isDownloadingPiper = true
                                    viewModel.ttsManager.downloadModel("PIPER", { progress ->
                                        piperProgress = progress
                                    }, {
                                        isDownloadingPiper = false
                                    })
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Download", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isDownloadingPiper) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = piperProgress,
                        modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                        color = NeonCyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Accent Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("All", "American", "British", "Indian").forEach { accent ->
                val isSelected = filterAccent == accent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { filterAccent = accent }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) NeonCyan else GlassTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voices list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredVoices) { voice ->
                val isCurrent = book?.voiceId == voice.id
                val hasKey = viewModel.ttsManager.hasApiKey(voice.provider)
                
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.changeVoice(voice.id, voice.provider)
                        }
                        .border(
                            width = if (isCurrent) 1.5.dp else 0.dp,
                            brush = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .testTag("voice_item_${voice.id}"),
                    backgroundColor = if (isCurrent) NeonCyan.copy(alpha = 0.05f) else GlassSurface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Microphone dot
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isCurrent) NeonCyan else GlassTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = voice.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Provider Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = voice.provider,
                                        fontSize = 8.sp,
                                        color = GlassTextSecondary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "${voice.gender} • ${voice.accent} • ${voice.description}",
                                fontSize = 11.sp,
                                color = GlassTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Status indicators (Ready offline or needs download)
                        val isDownloaded = viewModel.ttsManager.isModelDownloaded(voice.provider)
                        if (isDownloaded) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = "Ready offline",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Needs download",
                                tint = GlassTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Instructions item on adding keys
            item {
                Spacer(modifier = Modifier.height(16.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color.White.copy(alpha = 0.02f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How does on-device TTS work?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To run neural voice synthesis on-device offline:\n\n1. Select Kokoro TTS or Piper TTS above.\n2. Tap 'Download' to extract neural voice profiles fully on-device.\n3. Turn off Wi-Fi or Cellular network completely.\n4. Listen to any parsed book with zero latency and 100% data privacy!",
                            fontSize = 12.sp,
                            color = GlassTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
