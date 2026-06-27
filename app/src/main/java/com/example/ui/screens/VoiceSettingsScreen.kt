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

        // Warning banner for prototypes (Mandatory from Secret Management Skill)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.Red.copy(alpha = 0.08f),
            borderColor = Color.Red.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Security Alert",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Security Warning",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Text(
                        text = "Do not share prototype APKs publicly. Android files can be decompiled to extract API keys. Keep your credentials private.",
                        fontSize = 10.sp,
                        color = GlassTextSecondary,
                        lineHeight = 14.sp
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

                        // Status indicators (Key active, downloaded, or locked)
                        if (voice.provider == "NATIVE") {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = "Ready offline",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            if (hasKey) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Active license",
                                    tint = PremiumGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Key needed (Falls back offline)",
                                    tint = GlassTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                                text = "How to activate Premium AI Voices?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To unlock premium high-fidelity voices from OpenAI or ElevenLabs:\n\n1. Open Google AI Studio Build UI.\n2. Open the Secrets Panel.\n3. Add OPENAI_API_KEY or ELEVENLABS_API_KEY with your custom API credentials.\n4. Rebuild the app and enjoy hyper-realistic narration!",
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
