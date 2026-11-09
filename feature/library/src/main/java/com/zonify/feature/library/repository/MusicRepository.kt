package com.zonify.feature.library.repository

import android.content.Context
import com.zonify.core.database.ZonifyDatabase
import com.zonify.core.database.entity.PlaylistEntity
import com.zonify.core.database.entity.PlaylistTrackCrossRef
import com.zonify.core.database.mapper.TrackMapper
import com.zonify.core.model.Track
import com.zonify.feature.library.scanner.AudioFileScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(context: Context) {

    private val db = ZonifyDatabase.getInstance(context)
    private val trackDao = db.trackDao()
    private val playlistDao = db.playlistDao()
    private val scanner = AudioFileScanner(context)

    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
        .map { list -> list.map { TrackMapper.toDomain(it) } }
        .flowOn(Dispatchers.IO)

    val favoriteTracks: Flow<List<Track>> = trackDao.getFavoriteTracks()
        .map { list -> list.map { TrackMapper.toDomain(it) } }
        .flowOn(Dispatchers.IO)

    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .flowOn(Dispatchers.IO)

    fun searchTracks(query: String): Flow<List<Track>> = trackDao.searchTracks(query)
        .map { list -> list.map { TrackMapper.toDomain(it) } }
        .flowOn(Dispatchers.IO)

    suspend fun syncWithStorage() = withContext(Dispatchers.IO) {
        scanner.scanAllAudio().collect { scannedTracks ->
            if (scannedTracks.isNotEmpty()) {
                val entities = scannedTracks.map { TrackMapper.toEntity(it) }
                trackDao.insertOrUpdateTracks(entities)

                // Clean up tracks that were deleted from storage
                val validIds = scannedTracks.map { it.id }
                trackDao.deleteRemovedTracks(validIds)
            }
        }
    }

    suspend fun setFavorite(trackId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        trackDao.setFavorite(trackId, isFavorite)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId))
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> = playlistDao.getTracksForPlaylist(playlistId)
        .map { list -> list.map { TrackMapper.toDomain(it) } }
        .flowOn(Dispatchers.IO)
}
