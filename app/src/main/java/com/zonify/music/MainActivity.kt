package com.zonify.music

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zonify.core.audio.AudioEngineBridge
import com.zonify.core.model.AudioCodec
import com.zonify.core.model.AudioFormatInfo
import com.zonify.core.model.DsdFormat
import com.zonify.core.model.Track
import com.zonify.core.ui.components.MiniPlayer
import com.zonify.core.ui.theme.AppleMusicDarkBackground
import com.zonify.core.ui.theme.AppleMusicPink
import com.zonify.core.ui.theme.AppleMusicSurfaceDark
import com.zonify.core.ui.theme.AppleMusicTextSecondary
import com.zonify.core.ui.theme.ZonifyTheme
import com.zonify.feature.equalizer.EqualizerScreen
import com.zonify.feature.library.LibraryScreen
import com.zonify.feature.player.PlayerScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ZonifyTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Listen Now, 1: Library, 2: Equalizer
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBitPerfect by remember { mutableStateOf(false) }

    // Audiophile sample demo tracks
    val sampleTracks = remember {
        listOf(
            Track(
                id = 1,
                title = "Hotel California (Live On MTV)",
                artist = "Eagles",
                album = "Hell Freezes Over",
                durationMs = 432000L,
                uri = Uri.EMPTY,
                audioFormat = AudioFormatInfo(
                    codec = AudioCodec.FLAC,
                    sampleRateHz = 192000,
                    bitDepth = 24,
                    bitRateKbps = 5420,
                    channels = 2,
                    isLossless = true
                )
            ),
            Track(
                id = 2,
                title = "Time",
                artist = "Pink Floyd",
                album = "The Dark Side of the Moon",
                durationMs = 425000L,
                uri = Uri.EMPTY,
                audioFormat = AudioFormatInfo(
                    codec = AudioCodec.DSD,
                    sampleRateHz = 2822400,
                    bitDepth = 1,
                    channels = 2,
                    isLossless = true,
                    isDsd = true,
                    dsdFormat = DsdFormat.DSD64
                )
            ),
            Track(
                id = 3,
                title = "Get Lucky (feat. Pharrell Williams)",
                artist = "Daft Punk",
                album = "Random Access Memories",
                durationMs = 369000L,
                uri = Uri.EMPTY,
                audioFormat = AudioFormatInfo(
                    codec = AudioCodec.ALAC,
                    sampleRateHz = 96000,
                    bitDepth = 24,
                    channels = 2,
                    isLossless = true
                )
            )
        )
    }

    var currentTrack by remember { mutableStateOf<Track?>(sampleTracks.first()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = AppleMusicSurfaceDark.copy(alpha = 0.95f),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Listen Now") },
                        label = { Text("Listen Now") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppleMusicPink,
                            selectedTextColor = AppleMusicPink,
                            unselectedIconColor = AppleMusicTextSecondary,
                            unselectedTextColor = AppleMusicTextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppleMusicPink,
                            selectedTextColor = AppleMusicPink,
                            unselectedIconColor = AppleMusicTextSecondary,
                            unselectedTextColor = AppleMusicTextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer") },
                        label = { Text("DSP & EQ") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppleMusicPink,
                            selectedTextColor = AppleMusicPink,
                            unselectedIconColor = AppleMusicTextSecondary,
                            unselectedTextColor = AppleMusicTextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            },
            containerColor = AppleMusicDarkBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0, 1 -> {
                        LibraryScreen(
                            recentTracks = sampleTracks,
                            onTrackClick = { track ->
                                currentTrack = track
                                isPlaying = true
                                AudioEngineBridge.start()
                            }
                        )
                    }
                    2 -> {
                        EqualizerScreen(
                            onNavigateBack = { selectedTab = 1 }
                        )
                    }
                }

                // Apple Music Floating Mini Player sitting right above the bottom bar
                if (currentTrack != null) {
                    MiniPlayer(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        progress = 0.35f,
                        onPlayPauseClick = {
                            isPlaying = !isPlaying
                            if (isPlaying) AudioEngineBridge.start() else AudioEngineBridge.pause()
                        },
                        onSkipNextClick = {
                            val nextIndex = (sampleTracks.indexOf(currentTrack) + 1) % sampleTracks.size
                            currentTrack = sampleTracks[nextIndex]
                        },
                        onClick = { isPlayerExpanded = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }
            }
        }

        // Animated Full-Screen Apple Music Player Modal
        AnimatedVisibility(
            visible = isPlayerExpanded && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            currentTrack?.let { track ->
                PlayerScreen(
                    currentTrack = track,
                    isPlaying = isPlaying,
                    isBitPerfect = isBitPerfect,
                    onPlayPauseClick = {
                        isPlaying = !isPlaying
                        if (isPlaying) AudioEngineBridge.start() else AudioEngineBridge.pause()
                    },
                    onPreviousClick = {
                        val prevIndex = (sampleTracks.indexOf(currentTrack) - 1 + sampleTracks.size) % sampleTracks.size
                        currentTrack = sampleTracks[prevIndex]
                    },
                    onNextClick = {
                        val nextIndex = (sampleTracks.indexOf(currentTrack) + 1) % sampleTracks.size
                        currentTrack = sampleTracks[nextIndex]
                    },
                    onDismiss = { isPlayerExpanded = false },
                    onEqualizerClick = {
                        isPlayerExpanded = false
                        selectedTab = 2
                    }
                )
            }
        }
    }
}
