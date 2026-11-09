package com.zonify.feature.library.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.zonify.core.model.AudioCodec
import com.zonify.core.model.AudioFormatInfo
import com.zonify.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Robust audio file scanner combining Android MediaStore with direct filesystem
 * scanning to index FLAC, ALAC, DSD (DSF/DFF), WAV, Opus, and MP3 files.
 */
class AudioFileScanner(private val context: Context) {

    private val supportedExtensions = setOf("flac", "alac", "m4a", "dsf", "dff", "wav", "opus", "mp3", "ape", "wv")

    fun scanAllAudio(): Flow<List<Track>> = flow {
        val tracksMap = mutableMapOf<String, Track>() // keyed by normalized file path

        // 1. Scan Android MediaStore
        val mediaStoreTracks = scanMediaStore()
        for (track in mediaStoreTracks) {
            tracksMap[track.uri.toString()] = track
        }
        emit(tracksMap.values.toList())

        // 2. Scan Common Music & Download directories for DSD and unindexed Hi-Res files
        val musicDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )

        for (dir in musicDirs) {
            if (dir.exists() && dir.isDirectory) {
                scanDirectory(dir, tracksMap)
                emit(tracksMap.values.toList())
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun scanMediaStore(): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val durationMs = cursor.getLong(durationCol)
                    val path = cursor.getString(dataCol) ?: ""
                    val albumId = cursor.getLong(albumIdCol)
                    val trackNumber = cursor.getInt(trackCol)
                    val year = cursor.getInt(yearCol).takeIf { it > 0 }
                    val mimeType = cursor.getString(mimeCol) ?: ""

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    val audioFormat = determineAudioFormat(path, mimeType)

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            uri = contentUri,
                            artworkUri = artworkUri,
                            trackNumber = trackNumber,
                            year = year,
                            audioFormat = audioFormat
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tracks
    }

    private fun scanDirectory(dir: File, tracksMap: MutableMap<String, Track>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectory(file, tracksMap)
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in supportedExtensions) {
                    val fileUriString = Uri.fromFile(file).toString()
                    if (!tracksMap.containsKey(fileUriString)) {
                        val parsedTrack = parseFileDirectly(file)
                        if (parsedTrack != null) {
                            tracksMap[fileUriString] = parsedTrack
                        }
                    }
                }
            }
        }
    }

    private fun parseFileDirectly(file: File): Track? {
        val ext = file.extension.lowercase()
        return when (ext) {
            "dsf" -> DsdMetadataExtractor.parseDsfFile(file)
            "flac" -> FlacMetadataExtractor.parseFlacFile(file)
            else -> parseWithMediaMetadataRetriever(file)
        }
    }

    private fun parseWithMediaMetadataRetriever(file: File): Track? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull() ?: 0
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()

            // Check if folder contains cover.jpg or folder.jpg
            val localCover = File(file.parentFile, "cover.jpg").takeIf { it.exists() }
                ?: File(file.parentFile, "folder.jpg").takeIf { it.exists() }

            val format = determineAudioFormat(file.absolutePath, mimeType)

            Track(
                id = file.absolutePath.hashCode().toLong(),
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                uri = Uri.fromFile(file),
                artworkUri = localCover?.let { Uri.fromFile(it) },
                trackNumber = trackNumber,
                year = year,
                audioFormat = format
            )
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun determineAudioFormat(path: String, mimeType: String): AudioFormatInfo {
        val ext = path.substringAfterLast('.', "").lowercase()
        return when {
            ext == "flac" || mimeType.contains("flac") -> {
                AudioFormatInfo(AudioCodec.FLAC, 96000, 24, isLossless = true)
            }
            ext == "dsf" || ext == "dff" || mimeType.contains("dsd") -> {
                AudioFormatInfo(AudioCodec.DSD, 2822400, 1, isLossless = true, isDsd = true)
            }
            ext == "m4a" || ext == "alac" || mimeType.contains("alac") -> {
                // If it's lossless m4a (ALAC)
                AudioFormatInfo(AudioCodec.ALAC, 44100, 16, isLossless = true)
            }
            ext == "wav" || mimeType.contains("wav") -> {
                AudioFormatInfo(AudioCodec.WAV, 44100, 16, isLossless = true)
            }
            ext == "opus" || mimeType.contains("opus") -> {
                AudioFormatInfo(AudioCodec.OPUS, 48000, 16, isLossless = false)
            }
            else -> {
                AudioFormatInfo(AudioCodec.MP3, 44100, 16, isLossless = false)
            }
        }
    }
}
