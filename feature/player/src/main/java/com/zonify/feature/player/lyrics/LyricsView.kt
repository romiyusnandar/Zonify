package com.zonify.feature.player.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zonify.core.model.TrackLyric

@Composable
fun LyricsView(
    lyrics: TrackLyric?,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics == null || (!lyrics.isSynced && lyrics.plainText.isNullOrBlank()) || (lyrics.isSynced && lyrics.lines.isEmpty())) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No Lyrics Available",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Place a .lrc file in the same folder as the audio track.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
        return
    }

    val plain = lyrics.plainText
    if (!lyrics.isSynced && !plain.isNullOrBlank()) {
        // Plain unsynced lyrics display
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = plain,
                fontSize = 20.sp,
                lineHeight = 32.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Normal
            )
        }
        return
    }

    val lines = lyrics.lines
    val lazyListState = rememberLazyListState()

    // Determine current active lyric line
    val activeLineIndex = remember(currentPositionMs, lines) {
        val index = lines.indexOfLast { it.timeMs <= currentPositionMs }
        if (index >= 0) index else 0
    }

    // Auto-scroll to center the active line
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex in lines.indices) {
            lazyListState.animateScrollToItem(
                index = (activeLineIndex - 2).coerceAtLeast(0),
                scrollOffset = -120
            )
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(vertical = 140.dp, horizontal = 24.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeLineIndex
            val isPast = index < activeLineIndex

            val textColor by animateColorAsState(
                targetValue = when {
                    isActive -> Color.White
                    isPast -> Color.White.copy(alpha = 0.35f)
                    else -> Color.White.copy(alpha = 0.55f)
                },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "LyricTextColor"
            )

            val textScale by animateFloatAsState(
                targetValue = if (isActive) 1.05f else 0.95f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "LyricTextScale"
            )

            Text(
                text = line.text,
                fontSize = if (isActive) 26.sp else 22.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor,
                lineHeight = 38.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .scale(textScale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onSeekTo(line.timeMs)
                    }
            )
        }
    }
}
