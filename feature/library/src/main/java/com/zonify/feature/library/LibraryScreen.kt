package com.zonify.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zonify.core.model.Track
import com.zonify.core.ui.components.AudiophileBadge
import com.zonify.core.ui.theme.AppleMusicDarkBackground
import com.zonify.core.ui.theme.AppleMusicDivider
import com.zonify.core.ui.theme.AppleMusicPink
import com.zonify.core.ui.theme.AppleMusicRed
import com.zonify.core.ui.theme.AppleMusicTextSecondary

@Composable
fun LibraryScreen(
    recentTracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(AppleMusicDarkBackground),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large Apple Music Header Title
        item(span = { GridItemSpan(2) }) {
            Column {
                Text(
                    text = "Library",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // Apple Music Standard Navigation Categories
        item(span = { GridItemSpan(2) }) {
            Column {
                LibraryCategoryRow(icon = Icons.Default.QueueMusic, title = "Playlists") {}
                LibraryCategoryRow(icon = Icons.Default.Person, title = "Artists") {}
                LibraryCategoryRow(icon = Icons.Default.Album, title = "Albums") {}
                LibraryCategoryRow(icon = Icons.Default.MusicNote, title = "Songs") {}
                LibraryCategoryRow(icon = Icons.Default.Folder, title = "Lossless Folders") {}
            }
        }

        // Recently Added Section Header
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Recently Added",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        // 2-Column Square Album Art Grid (Apple Music signature)
        items(recentTracks) { track ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackClick(track) }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = track.title.take(1),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = track.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = AppleMusicTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                AudiophileBadge(
                    qualityBadge = track.audioFormat.qualityBadge,
                    isBitPerfect = false,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun LibraryCategoryRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppleMusicPink,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = AppleMusicTextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
        HorizontalDivider(color = AppleMusicDivider, thickness = 0.5.dp)
    }
}
