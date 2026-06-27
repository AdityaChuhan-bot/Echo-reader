package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.AnimatedAmbientGlow
import com.example.ui.components.GlassCard
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan

enum class Screen {
    Library,
    Player,
    Search,
    Voices
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(Screen.Library) }
                val currentBook by viewModel.currentBook.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    // Floating ambient neon bubble glows
                    AnimatedAmbientGlow()

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Screen navigation dispatcher
                            when (currentScreen) {
                                Screen.Library -> {
                                    LibraryScreen(
                                        viewModel = viewModel,
                                        onBookSelected = { currentScreen = Screen.Player }
                                    )
                                }
                                Screen.Player -> {
                                    PlayerScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.Library },
                                        onNavigateToSearch = { currentScreen = Screen.Search },
                                        onNavigateToVoices = { currentScreen = Screen.Voices }
                                    )
                                }
                                Screen.Search -> {
                                    SearchScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.Player }
                                    )
                                }
                                Screen.Voices -> {
                                    VoiceSettingsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.Player }
                                    )
                                }
                            }

                            // Dynamic bottom floating mini-player inside Library screen
                            AnimatedVisibility(
                                visible = currentScreen == Screen.Library && currentBook != null,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                currentBook?.let { book ->
                                    MiniPlayer(
                                        book = book,
                                        isPlaying = isPlaying,
                                        onPlayPause = {
                                            if (isPlaying) viewModel.pause() else viewModel.resume()
                                        },
                                        onClick = { currentScreen = Screen.Player }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(
    book: com.example.data.database.BookEntity,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("floating_mini_player"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color(0xFF1E293B).copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Book cover thumbnail
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(book.coverColor))
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = book.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Chapter ${book.currentChapterIndex + 1} • sentence ${book.currentSentenceIndex + 1}",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Inline Play control
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
