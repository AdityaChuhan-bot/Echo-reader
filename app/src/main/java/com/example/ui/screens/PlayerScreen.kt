package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val book by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val sentences by viewModel.currentSentences.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val sleepTimeRemaining by viewModel.sleepTimeRemaining.collectAsState()

    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkNote by remember { mutableStateOf("") }
    var showChaptersDrawer by remember { mutableStateOf(false) }

    val activeChapterTitle = if (chapters.isNotEmpty() && book != null && book!!.currentChapterIndex < chapters.size) {
        chapters[book!!.currentChapterIndex].title
    } else "Chapter 1"

    val activeSentence = if (sentences.isNotEmpty() && book != null && book!!.currentSentenceIndex < sentences.size) {
        sentences[book!!.currentSentenceIndex]
    } else "No text loaded."

    // Animated visual rotating disk
    val infiniteTransition = rememberInfiniteTransition(label = "player")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 6000 else 0, easing = LinearEasing)
        ),
        label = "rotation"
    )

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active book selected", color = GlassTextSecondary)
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Upper navigation bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("player_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GlassTextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = book!!.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Now Storytelling",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onNavigateToSearch, modifier = Modifier.testTag("player_search_button")) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GlassTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Central rotating disk cover illustration
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(book!!.coverColor), Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .rotate(rotationAngle),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Grooves & Cover
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                )
                // Center pin
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Text Highlights Container
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                backgroundColor = GlassSurface
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(color = NeonCyan)
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    text = activeSentence,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GlassTextPrimary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 26.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Chapter and Progress indicator info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeChapterTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showChaptersDrawer = true }
                        .testTag("chapter_selector_trigger")
                )
                Text(
                    text = "${book!!.currentSentenceIndex + 1}/${sentences.size} sentences",
                    fontSize = 12.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sentence-level Slider Seekbar
            if (sentences.isNotEmpty()) {
                Slider(
                    value = book!!.currentSentenceIndex.toFloat(),
                    onValueChange = { viewModel.seekToSentence(it.toInt()) },
                    valueRange = 0f..(sentences.size - 1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_seekbar")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Audio Player Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev Sentence
                IconButton(
                    onClick = { viewModel.skipBackwardSentence() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Previous sentence",
                        tint = GlassTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause Glass Action Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan, NeonPurple)
                            )
                        )
                        .clickable {
                            if (isPlaying) viewModel.pause() else viewModel.resume()
                        }
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Next Sentence
                IconButton(
                    onClick = { viewModel.skipForwardSentence() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Next sentence",
                        tint = GlassTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Settings Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice selector
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigateToVoices() }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Voice selection",
                        tint = GlassTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Voice Library",
                        fontSize = 11.sp,
                        color = GlassTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Speed
                var speedExpanded by remember { mutableStateOf(false) }
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { speedExpanded = true }
                            .testTag("speed_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Playback speed",
                            tint = GlassTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${book!!.speed}x Speed",
                            fontSize = 11.sp,
                            color = GlassTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = speedExpanded,
                        onDismissRequest = { speedExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { spd ->
                            DropdownMenuItem(
                                text = { Text("${spd}x", color = Color.White) },
                                onClick = {
                                    viewModel.changeSpeed(spd)
                                    speedExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sleep Timer
                var sleepExpanded by remember { mutableStateOf(false) }
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { sleepExpanded = true }
                            .testTag("sleep_timer_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Sleep timer",
                            tint = if (sleepTimeRemaining != null) NeonCyan else GlassTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val timerLabel = when {
                            sleepTimeRemaining == null -> "Timer Off"
                            sleepTimeRemaining == -1L -> "End of Chap"
                            else -> {
                                val min = TimeUnit.MILLISECONDS.toMinutes(sleepTimeRemaining!!)
                                val sec = TimeUnit.MILLISECONDS.toSeconds(sleepTimeRemaining!!) % 60
                                String.format("%02d:%02d", min, sec)
                            }
                        }
                        Text(
                            text = timerLabel,
                            fontSize = 11.sp,
                            color = if (sleepTimeRemaining != null) NeonCyan else GlassTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = sleepExpanded,
                        onDismissRequest = { sleepExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Turn Off", color = Color.White) },
                            onClick = { viewModel.stopSleepTimer(); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("15 minutes", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(15); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("30 minutes", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(30); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("45 minutes", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(45); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("60 minutes", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(60); sleepExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("End of Chapter", color = Color.White) },
                            onClick = { viewModel.startSleepTimer(-1); sleepExpanded = false }
                        )
                    }
                }

                // Bookmark Add
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showBookmarkDialog = true }
                        .testTag("bookmark_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Add bookmark",
                        tint = GlassTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bookmark",
                        fontSize = 11.sp,
                        color = GlassTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bookmark Dialog
        if (showBookmarkDialog) {
            AlertDialog(
                onDismissRequest = { showBookmarkDialog = false },
                title = { Text("Add Bookmark note", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "\"${activeSentence.take(60)}...\"",
                            fontSize = 13.sp,
                            color = GlassTextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        TextField(
                            value = bookmarkNote,
                            onValueChange = { bookmarkNote = it },
                            placeholder = { Text("Enter personal notes (optional)") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        onClick = {
                            viewModel.addBookmark(bookmarkNote)
                            bookmarkNote = ""
                            showBookmarkDialog = false
                        }
                    ) {
                        Text("Save Bookmark", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        onClick = { showBookmarkDialog = false }
                    ) {
                        Text("Cancel", color = GlassTextSecondary)
                    }
                },
                containerColor = Color(0xFF151F32)
            )
        }

        // Chapters Drawer Overlay
        AnimatedVisibility(
            visible = showChaptersDrawer,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showChaptersDrawer = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clickable(enabled = false) {}, // Intercept click
                    backgroundColor = Color(0xFF0F172A),
                    borderColor = GlassBorder
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Chapter",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary
                            )
                            IconButton(onClick = { showChaptersDrawer = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(chapters) { idx, chapter ->
                                val isSelected = book!!.currentChapterIndex == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            viewModel.skipToChapter(idx)
                                            showChaptersDrawer = false
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = if (isSelected) NeonCyan else GlassTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = chapter.title,
                                        fontSize = 15.sp,
                                        color = if (isSelected) NeonCyan else GlassTextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
