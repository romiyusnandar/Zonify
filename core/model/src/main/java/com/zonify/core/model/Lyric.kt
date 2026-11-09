package com.zonify.core.model

/**
 * Single line in a time-synced lyric.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/**
 * Complete lyric model representing synced or plain lyrics.
 */
data class TrackLyric(
    val trackId: Long,
    val isSynced: Boolean = true,
    val lines: List<LyricLine> = emptyList(),
    val plainText: String? = null
)
