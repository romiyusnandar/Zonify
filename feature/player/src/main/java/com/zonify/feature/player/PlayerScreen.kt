package com.zonify.feature.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zonify.core.model.Track
import com.zonify.core.ui.components.AppleMusicBlurBackground
import com.zonify.core.ui.components.AudiophileBadge
import com.zonify.core.ui.theme.AppleMusicPink
import com.zonify.core.ui.theme.AppleMusicRed
import com.zonify.core.ui.theme.AppleMusicTextSecondary
import com.zonify.core.ui.theme.BitPerfectCyan

@Composable
fun PlayerScreen(
    currentTrack: Track,
    isPlaying: Boolean,
    isBitPerfect: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onDismiss: () -> Unit,
    onEqualizerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTechDialog by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableFloatStateOf(0f) }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar (Dismiss handle / menu)
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                // Top drag grabber
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
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

            // Artwork (Curved, drop shadow, dynamic scale)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentTrack.title.take(1),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                value = currentPositionMs,
                onValueChange = { currentPositionMs = it },
                valueRange = 0f..currentTrack.durationMs.toFloat(),
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
                    text = formatMs(currentPositionMs.toLong()),
                    fontSize = 12.sp,
                    color = AppleMusicTextSecondary
                )
                Text(
                    text = "-" + formatMs(currentTrack.durationMs - currentPositionMs.toLong()),
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
                IconButton(onClick = { /* Lyrics Sheet */ }) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.8f)
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
