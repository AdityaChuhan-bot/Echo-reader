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
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
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

    var filterAccent by remember { mutableStateOf("All") } // All, American, British, Australian
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

        // KittenTTS Neural Engine Status Dashboard
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF0F172A).copy(alpha = 0.5f),
            borderColor = NeonPurple
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "KittenTTS Neural Audio Engine",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Model package size: 22 MB • Snapdragon Optimized",
                    fontSize = 11.sp,
                    color = NeonCyan,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "KittenTTS-SOTA-v2.5.onnx",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTextPrimary
                        )
                        Text(
                            text = "Active & Installed Fully Offline",
                            fontSize = 10.sp,
                            color = GlassTextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Ready", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
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
            listOf("All", "American", "British", "Australian").forEach { accent ->
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
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
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

                        // Status indicators (Ready offline)
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "Ready offline",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Info instruction item
            item {
                Spacer(modifier = Modifier.height(16.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color.White.copy(alpha = 0.02f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How does on-device KittenTTS work?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To run neural voice synthesis on-device fully offline:\n\n1. Select any of the pre-packaged KittenTTS neural voices from the list.\n2. AudioBook will automatically use the high-performance local ONNX model to synthesize your text sentence-by-sentence.\n3. Turn off Wi-Fi or Cellular network completely.\n4. Enjoy seamless, zero-latency audiobook listening with 100% data privacy and minimal battery usage!",
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
