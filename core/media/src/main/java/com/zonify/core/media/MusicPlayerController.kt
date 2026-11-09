package com.zonify.core.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.zonify.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import android.util.Log
import androidx.media3.common.PlaybackException

/**
 * Singleton client controller managing connection to ZonifyMediaService.
 * Exposes reactive StateFlow for UI and playback actions.
 */
class MusicPlayerController private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null

    private var currentPlaylist: List<Track> = emptyList()
    private var pendingPlayRequest: Pair<List<Track>, Int>? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    companion object {
        @Volatile
        private var instance: MusicPlayerController? = null

        fun getInstance(context: Context): MusicPlayerController {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerController(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, ZonifyMediaService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                pendingPlayRequest?.let { (tracks, index) ->
                    pendingPlayRequest = null
                    playQueue(tracks, index)
                }
            } catch (e: Exception) {
                Log.e("ZonifyPlayer", "Failed to connect to MediaSession: ${e.message}", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                val matched = currentPlaylist.firstOrNull { it.id.toString() == mediaId }
                if (matched != null) {
                    _currentTrack.value = matched
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("ZonifyPlayer", "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}", error)
                _isPlaying.value = false
            }
        })
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return

        // Filter playable tracks
        val playableTracks = tracks.filter { it.uri != android.net.Uri.EMPTY }
        if (playableTracks.isEmpty()) {
            Log.w("ZonifyPlayer", "Cannot play track: URI is empty. Please scan real local files.")
            _currentTrack.value = tracks.getOrNull(startIndex)
            return
        }

        currentPlaylist = playableTracks
        val controller = mediaController
        if (controller == null) {
            pendingPlayRequest = Pair(tracks, startIndex)
            return
        }

        val mediaItems = playableTracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.artworkUri)
                        .build()
                )
                .build()
        }

        val targetIndex = startIndex.coerceIn(0, mediaItems.size - 1)
        controller.setMediaItems(mediaItems, targetIndex, /* startPositionMs = */ 0L)
        controller.prepare()
        controller.play()
        _currentTrack.value = playableTracks.getOrNull(targetIndex)
    }

    fun playTrack(track: Track) {
        playQueue(listOf(track), 0)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun skipNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val pos = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
                val dur = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                _currentPositionMs.value = pos
                _durationMs.value = dur
                if (dur > 0) {
                    _progress.value = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    fun release() {
        stopProgressTracking()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
