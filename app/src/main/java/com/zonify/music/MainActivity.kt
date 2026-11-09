package com.zonify.music

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zonify.core.audio.AudioEngineBridge
import com.zonify.core.media.MusicPlayerController
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
import com.zonify.feature.library.LibraryViewModel
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
fun MainAppScreen(
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val musicController = remember { MusicPlayerController.getInstance(context) }

    val scannedTracks by libraryViewModel.tracks.collectAsState()
    val isScanning by libraryViewModel.isScanning.collectAsState()

    val activeTrack by musicController.currentTrack.collectAsState()
    val isPlaying by musicController.isPlaying.collectAsState()
    val currentPositionMs by musicController.currentPositionMs.collectAsState()
    val durationMs by musicController.durationMs.collectAsState()
    val progress by musicController.progress.collectAsState()
    val isBitPerfect by AudioEngineBridge.isBitPerfectActive.collectAsState()

    var selectedTab by remember { mutableIntStateOf(1) } // 0: Listen Now, 1: Library, 2: Equalizer
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // Fallback sample demo tracks when storage is empty
    val fallbackTracks = remember {
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

    val displayTracks = if (scannedTracks.isNotEmpty()) scannedTracks else fallbackTracks
    val currentDisplayTrack = activeTrack ?: displayTracks.firstOrNull()

    // Request permissions on launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            libraryViewModel.scanMusic()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasPermission = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermission) {
            libraryViewModel.scanMusic()
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

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
                            recentTracks = displayTracks,
                            isScanning = isScanning,
                            onScanClick = { libraryViewModel.scanMusic() },
                            onTrackClick = { track ->
                                val index = displayTracks.indexOf(track).coerceAtLeast(0)
                                musicController.playQueue(displayTracks, index)
                            }
                        )
                    }
                    2 -> {
                        EqualizerScreen(
                            onNavigateBack = { selectedTab = 1 }
                        )
                    }
                }

                // Floating Mini Player
                if (currentDisplayTrack != null) {
                    MiniPlayer(
                        currentTrack = currentDisplayTrack,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPauseClick = { musicController.togglePlayPause() },
                        onSkipNextClick = { musicController.skipNext() },
                        onClick = { isPlayerExpanded = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }
            }
        }

        // Animated Full-Screen Player Modal
        AnimatedVisibility(
            visible = isPlayerExpanded && currentDisplayTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            currentDisplayTrack?.let { track ->
                PlayerScreen(
                    currentTrack = track,
                    isPlaying = isPlaying,
                    isBitPerfect = isBitPerfect,
                    currentPositionMs = currentPositionMs,
                    durationMs = if (durationMs > 0) durationMs else track.durationMs,
                    onSeekTo = { posMs -> musicController.seekTo(posMs) },
                    onPlayPauseClick = { musicController.togglePlayPause() },
                    onPreviousClick = { musicController.skipPrevious() },
                    onNextClick = { musicController.skipNext() },
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
