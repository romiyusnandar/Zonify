package com.zonify.feature.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zonify.core.model.LyricLine
import com.zonify.core.model.Track
import com.zonify.core.model.TrackLyric
import com.zonify.core.ui.components.AppleMusicBlurBackground
import com.zonify.core.ui.components.AudiophileBadge
import com.zonify.core.ui.components.TrackArtworkImage
import com.zonify.core.ui.theme.AppleMusicPink
import com.zonify.core.ui.theme.AppleMusicRed
import com.zonify.core.ui.theme.AppleMusicTextSecondary
import com.zonify.core.ui.theme.BitPerfectCyan
import com.zonify.feature.player.lyrics.LrcParser
import com.zonify.feature.player.lyrics.LyricsView
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(
    currentTrack: Track,
    isPlaying: Boolean,
    isBitPerfect: Boolean,
    currentPositionMs: Long = 0L,
    durationMs: Long = currentTrack.durationMs,
    onSeekTo: (Long) -> Unit = {},
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onDismiss: () -> Unit,
    onEqualizerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTechDialog by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "DragOffset"
    )

    val effectivePosition = if (isDraggingSlider) sliderPosition.toLong() else currentPositionMs
    val effectiveDuration = if (durationMs > 0) durationMs else currentTrack.durationMs.coerceAtLeast(1L)

    // Load local LRC file or show demo synced lyrics ONLY for Hotel California demo track
    val trackLyrics = remember(currentTrack.id, currentTrack.title) {
        val localLrc = LrcParser.findLocalLrcFile(currentTrack.uri.path)
        if (localLrc != null) {
            LrcParser.parse(localLrc, currentTrack.id)
        } else if (currentTrack.title.contains("Hotel California", ignoreCase = true)) {
            // High-quality sample synced lyrics demo ONLY for demo song
            TrackLyric(
                trackId = currentTrack.id,
                isSynced = true,
                lines = listOf(
                    LyricLine(0L, "On a dark desert highway, cool wind in my hair"),
                    LyricLine(7000L, "Warm smell of colitas, rising up through the air"),
                    LyricLine(14000L, "Up ahead in the distance, I saw a shimmering light"),
                    LyricLine(21000L, "My head grew heavy and my sight grew dim"),
                    LyricLine(28000L, "I had to stop for the night"),
                    LyricLine(35000L, "There she stood in the doorway"),
                    LyricLine(39000L, "I heard the mission bell"),
                    LyricLine(42000L, "And I was thinkin' to myself"),
                    LyricLine(46000L, "'This could be heaven or this could be hell'"),
                    LyricLine(51000L, "Then she lit up a candle, and she showed me the way"),
                    LyricLine(58000L, "There were voices down the corridor"),
                    LyricLine(63000L, "I thought I heard them say..."),
                    LyricLine(68000L, "Welcome to the Hotel California"),
                    LyricLine(76000L, "Such a lovely place (Such a lovely place)"),
                    LyricLine(81000L, "Such a lovely face")
                )
            )
        } else {
            null
        }
    }

    // Apple Music scale effect: artwork grows slightly when playing
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.85f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "ArtworkScale"
    )

    AppleMusicBlurBackground(
        primaryColor = AppleMusicRed,
        secondaryColor = AppleMusicPink,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, animatedOffsetY.roundToInt().coerceAtLeast(0)) }
                .pointerInput(showLyrics) {
                    // Swiping down anywhere on player sheet dismisses it when not browsing lyrics
                    if (!showLyrics) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0 || dragOffsetY > 0) {
                                    dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                if (dragOffsetY > 130f) {
                                    onDismiss()
                                }
                                dragOffsetY = 0f
                            },
                            onDragCancel = { dragOffsetY = 0f }
                        )
                    }
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar (Dismiss handle / menu) with dedicated swipe down detection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0 || dragOffsetY > 0) {
                                    dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                if (dragOffsetY > 100f) {
                                    onDismiss()
                                }
                                dragOffsetY = 0f
                            },
                            onDragCancel = { dragOffsetY = 0f }
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Apple Music Grabber indicator bar
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )

                IconButton(onClick = { /* More actions */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center View: Artwork or Time-Synced Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = showLyrics, label = "ArtworkLyricsCrossfade") { isLyricsActive ->
                    if (isLyricsActive) {
                        LyricsView(
                            lyrics = trackLyrics,
                            currentPositionMs = effectivePosition,
                            onSeekTo = onSeekTo,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .aspectRatio(1f)
                                    .scale(artworkScale)
                                    .shadow(elevation = 28.dp, shape = RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                            ) {
                                TrackArtworkImage(
                                    track = currentTrack,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(20.dp),
                                    iconSize = 64.dp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track Title & Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentTrack.artist,
                        color = AppleMusicTextSecondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Audiophile Badge
                AudiophileBadge(
                    qualityBadge = currentTrack.audioFormat.qualityBadge,
                    isBitPerfect = isBitPerfect,
                    onClick = { showTechDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Apple Music Fluid Scrubber
            Slider(
                value = effectivePosition.toFloat().coerceIn(0f, effectiveDuration.toFloat()),
                onValueChange = {
                    isDraggingSlider = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    onSeekTo(sliderPosition.toLong())
                },
                valueRange = 0f..effectiveDuration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Progress labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(effectivePosition),
                    fontSize = 12.sp,
                    color = AppleMusicTextSecondary
                )
                Text(
                    text = "-" + formatMs((effectiveDuration - effectivePosition).coerceAtLeast(0L)),
                    fontSize = 12.sp,
                    color = AppleMusicTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousClick) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Tools: Lyrics, Equalizer, Repeat/Shuffle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showLyrics = !showLyrics },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showLyrics) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) AppleMusicPink else Color.White.copy(alpha = 0.8f)
                    )
                }

                IconButton(onClick = onEqualizerClick) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Equalizer & DSP",
                        tint = if (isBitPerfect) BitPerfectCyan else Color.White
                    )
                }

                IconButton(onClick = { /* Shuffle/Repeat */ }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Audiophile Technical Metadata Dialog
    if (showTechDialog) {
        AlertDialog(
            onDismissRequest = { showTechDialog = false },
            title = {
                Text(
                    text = "Audiophile Stream Info",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text("Codec: ${currentTrack.audioFormat.codec.displayName}", color = Color.White)
                    Text("Sample Rate: ${currentTrack.audioFormat.sampleRateHz} Hz", color = Color.White)
                    Text("Bit Depth: ${currentTrack.audioFormat.bitDepth}-bit", color = Color.White)
                    Text("Channels: ${currentTrack.audioFormat.channels} (Stereo)", color = Color.White)
                    Text(
                        "Bit-Perfect State: ${if (isBitPerfect) "ACTIVE (Bypassing AudioFlinger)" else "Android Standard Mixer"}",
                        color = if (isBitPerfect) BitPerfectCyan else Color.Yellow,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTechDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
