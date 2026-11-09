package com.zonify.core.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zonify.core.model.Track
import com.zonify.core.ui.theme.AppleMusicCardDark
import com.zonify.core.ui.theme.AppleMusicPink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Robust audio artwork loader:
 * 1. Reads MediaStore albumart URI
 * 2. On Android 10+ (API 29+), uses ContentResolver.loadThumbnail
 * 3. Reads embedded picture (ID3 / Vorbis APIC) via MediaMetadataRetriever
 * 4. Checks local folder cover.jpg / folder.jpg
 */
suspend fun loadTrackArtwork(context: Context, track: Track): Any? = withContext(Dispatchers.IO) {
    // 1. Direct artworkUri check
    val artUri = track.artworkUri
    if (artUri != null && artUri != Uri.EMPTY) {
        try {
            context.contentResolver.openInputStream(artUri)?.use {
                return@withContext artUri
            }
        } catch (_: Exception) {}
    }

    // 2. Android 10+ ContentResolver thumbnail on track.uri
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && track.uri != Uri.EMPTY && track.uri.scheme == "content") {
        try {
            val bitmap = context.contentResolver.loadThumbnail(track.uri, Size(512, 512), null)
            return@withContext bitmap
        } catch (_: Exception) {}
    }

    // 3. Embedded picture in audio file
    if (track.uri != Uri.EMPTY) {
        val retriever = MediaMetadataRetriever()
        try {
            if (track.uri.scheme == "content") {
                retriever.setDataSource(context, track.uri)
            } else if (track.uri.path != null) {
                retriever.setDataSource(track.uri.path)
            }
            val embedded = retriever.embeddedPicture
            if (embedded != null && embedded.isNotEmpty()) {
                return@withContext embedded
            }
        } catch (_: Exception) {}
        finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    // 4. Local folder cover.jpg or folder.jpg
    if (track.uri.scheme == "file" || (track.uri.path != null && track.uri.path!!.startsWith("/"))) {
        try {
            val path = track.uri.path ?: ""
            val file = File(path)
            if (file.exists() && file.parentFile != null) {
                val cover = File(file.parentFile, "cover.jpg").takeIf { it.exists() }
                    ?: File(file.parentFile, "folder.jpg").takeIf { it.exists() }
                    ?: File(file.parentFile, "album.jpg").takeIf { it.exists() }
                if (cover != null) {
                    return@withContext cover
                }
            }
        } catch (_: Exception) {}
    }

    return@withContext null
}

@Composable
fun TrackArtworkImage(
    track: Track,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(8.dp),
    iconSize: Dp = 24.dp
) {
    val context = LocalContext.current
    val artworkModel = produceState<Any?>(initialValue = track.artworkUri, key1 = track.id) {
        value = loadTrackArtwork(context, track) ?: track.artworkUri
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(AppleMusicCardDark),
        contentAlignment = Alignment.Center
    ) {
        if (artworkModel.value != null) {
            AsyncImage(
                model = artworkModel.value,
                contentDescription = "${track.title} artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            // Placeholder when no album art exists
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (track.title.isNotBlank()) {
                    Text(
                        text = track.title.take(1).uppercase(),
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = (iconSize.value * 0.8f).sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = AppleMusicPink.copy(alpha = 0.6f),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}
