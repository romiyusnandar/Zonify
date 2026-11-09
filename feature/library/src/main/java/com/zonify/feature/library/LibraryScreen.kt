package com.zonify.feature.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zonify.core.database.entity.PlaylistEntity
import com.zonify.core.model.Track
import com.zonify.core.ui.components.AudiophileBadge
import com.zonify.core.ui.theme.AppleMusicCardDark
import com.zonify.core.ui.theme.AppleMusicDarkBackground
import com.zonify.core.ui.theme.AppleMusicDivider
import com.zonify.core.ui.theme.AppleMusicPink
import com.zonify.core.ui.theme.AppleMusicTextSecondary

enum class LibraryViewCategory {
    MAIN,
    PLAYLISTS,
    ARTISTS,
    ALBUMS,
    SONGS,
    FOLDERS,
    DETAIL_FILTER
}

@Composable
fun LibraryScreen(
    recentTracks: List<Track>,
    playlists: List<PlaylistEntity> = emptyList(),
    isScanning: Boolean = false,
    onScanClick: () -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableStateOf(LibraryViewCategory.MAIN) }
    var filterTitle by remember { mutableStateOf("") }
    var filteredTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    BackHandler(enabled = activeCategory != LibraryViewCategory.MAIN) {
        activeCategory = LibraryViewCategory.MAIN
    }

    when (activeCategory) {
        LibraryViewCategory.MAIN -> {
            MainLibraryGrid(
                recentTracks = recentTracks,
                isScanning = isScanning,
                onScanClick = onScanClick,
                onCategoryClick = { category ->
                    activeCategory = category
                },
                onTrackClick = onTrackClick,
                modifier = modifier
            )
        }

        LibraryViewCategory.PLAYLISTS -> {
            PlaylistsView(
                playlists = playlists,
                onBack = { activeCategory = LibraryViewCategory.MAIN },
                onCreatePlaylistClick = { showCreatePlaylistDialog = true }
            )
        }

        LibraryViewCategory.ALBUMS -> {
            AlbumsView(
                tracks = recentTracks,
                onBack = { activeCategory = LibraryViewCategory.MAIN },
                onAlbumClick = { albumName, albumTracks ->
                    filterTitle = albumName
                    filteredTracks = albumTracks
                    activeCategory = LibraryViewCategory.DETAIL_FILTER
                }
            )
        }

        LibraryViewCategory.ARTISTS -> {
            ArtistsView(
                tracks = recentTracks,
                onBack = { activeCategory = LibraryViewCategory.MAIN },
                onArtistClick = { artistName, artistTracks ->
                    filterTitle = artistName
                    filteredTracks = artistTracks
                    activeCategory = LibraryViewCategory.DETAIL_FILTER
                }
            )
        }

        LibraryViewCategory.SONGS -> {
            SongsView(
                tracks = recentTracks,
                title = "All Songs",
                onBack = { activeCategory = LibraryViewCategory.MAIN },
                onTrackClick = onTrackClick
            )
        }

        LibraryViewCategory.FOLDERS -> {
            FoldersView(
                tracks = recentTracks,
                onBack = { activeCategory = LibraryViewCategory.MAIN },
                onFolderClick = { folderName, folderTracks ->
                    filterTitle = folderName
                    filteredTracks = folderTracks
                    activeCategory = LibraryViewCategory.DETAIL_FILTER
                }
            )
        }

        LibraryViewCategory.DETAIL_FILTER -> {
            SongsView(
                tracks = filteredTracks,
                title = filterTitle,
                onBack = { activeCategory = LibraryViewCategory.MAIN },
                onTrackClick = onTrackClick
            )
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleMusicPink)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCreatePlaylistDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun MainLibraryGrid(
    recentTracks: List<Track>,
    isScanning: Boolean,
    onScanClick: () -> Unit,
    onCategoryClick: (LibraryViewCategory) -> Unit,
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
        // Large Header Title with Scan / Refresh Button
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AppleMusicPink,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onScanClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Media",
                            tint = AppleMusicPink
                        )
                    }
                }
            }
        }

        // Apple Music Standard Navigation Categories
        item(span = { GridItemSpan(2) }) {
            Column {
                LibraryCategoryRow(icon = Icons.AutoMirrored.Filled.QueueMusic, title = "Playlists") {
                    onCategoryClick(LibraryViewCategory.PLAYLISTS)
                }
                LibraryCategoryRow(icon = Icons.Default.Person, title = "Artists") {
                    onCategoryClick(LibraryViewCategory.ARTISTS)
                }
                LibraryCategoryRow(icon = Icons.Default.Album, title = "Albums") {
                    onCategoryClick(LibraryViewCategory.ALBUMS)
                }
                LibraryCategoryRow(icon = Icons.Default.MusicNote, title = "Songs") {
                    onCategoryClick(LibraryViewCategory.SONGS)
                }
                LibraryCategoryRow(icon = Icons.Default.Folder, title = "Lossless Folders") {
                    onCategoryClick(LibraryViewCategory.FOLDERS)
                }
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
                    colors = CardDefaults.cardColors(containerColor = AppleMusicCardDark)
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
private fun AlbumsView(
    tracks: List<Track>,
    onBack: () -> Unit,
    onAlbumClick: (String, List<Track>) -> Unit
) {
    val albums = remember(tracks) {
        tracks.groupBy { it.album }.toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleMusicDarkBackground)
            .padding(16.dp)
    ) {
        CategoryHeader(title = "Albums", onBack = onBack)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums) { (albumName, albumTracks) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(albumName, albumTracks) }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppleMusicCardDark)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(albumName.take(1), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(albumName, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(albumTracks.firstOrNull()?.artist ?: "", color = AppleMusicTextSecondary, fontSize = 13.sp, maxLines = 1)
                    Text("${albumTracks.size} songs", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ArtistsView(
    tracks: List<Track>,
    onBack: () -> Unit,
    onArtistClick: (String, List<Track>) -> Unit
) {
    val artists = remember(tracks) {
        tracks.groupBy { it.artist }.toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleMusicDarkBackground)
            .padding(16.dp)
    ) {
        CategoryHeader(title = "Artists", onBack = onBack)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(artists) { (artistName, artistTracks) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artistName, artistTracks) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(AppleMusicCardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(artistName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artistName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("${artistTracks.size} songs", color = AppleMusicTextSecondary, fontSize = 13.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
                HorizontalDivider(color = AppleMusicDivider, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun SongsView(
    tracks: List<Track>,
    title: String,
    onBack: () -> Unit,
    onTrackClick: (Track) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleMusicDarkBackground)
            .padding(16.dp)
    ) {
        CategoryHeader(title = title, onBack = onBack)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tracks) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTrackClick(track) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppleMusicCardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(track.title.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${track.artist} • ${track.album}", color = AppleMusicTextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    AudiophileBadge(qualityBadge = track.audioFormat.qualityBadge, isBitPerfect = false, onClick = {})
                }
                HorizontalDivider(color = AppleMusicDivider, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun FoldersView(
    tracks: List<Track>,
    onBack: () -> Unit,
    onFolderClick: (String, List<Track>) -> Unit
) {
    val folders = remember(tracks) {
        tracks.groupBy { track ->
            track.uri.path?.substringBeforeLast('/', "")?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Music"
        }.toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleMusicDarkBackground)
            .padding(16.dp)
    ) {
        CategoryHeader(title = "Lossless Folders", onBack = onBack)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(folders) { (folderName, folderTracks) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFolderClick(folderName, folderTracks) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = AppleMusicPink, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folderName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("${folderTracks.size} audio files", color = AppleMusicTextSecondary, fontSize = 13.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
                HorizontalDivider(color = AppleMusicDivider, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun PlaylistsView(
    playlists: List<PlaylistEntity>,
    onBack: () -> Unit,
    onCreatePlaylistClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleMusicDarkBackground)
            .padding(16.dp)
    ) {
        CategoryHeader(
            title = "Playlists",
            onBack = onBack,
            trailingContent = {
                IconButton(onClick = onCreatePlaylistClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = AppleMusicPink)
                }
            }
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Default Favorites Playlist
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppleMusicPink.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = AppleMusicPink)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Favorite Songs", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Auto-generated playlist", color = AppleMusicTextSecondary, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = AppleMusicDivider, thickness = 0.5.dp)
            }

            items(playlists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppleMusicCardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Custom Playlist", color = AppleMusicTextSecondary, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = AppleMusicDivider, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        trailingContent?.invoke()
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
