package com.zonify.feature.player.lyrics

import com.zonify.core.model.LyricLine
import com.zonify.core.model.TrackLyric
import java.io.File
import java.util.regex.Pattern

object LrcParser {

    private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?]")
    private val OFFSET_PATTERN = Pattern.compile("\\[offset:([+-]?\\d+)]")

    /**
     * Parses an LRC string into a TrackLyric object.
     */
    fun parse(lrcContent: String, trackId: Long = 0L): TrackLyric {
        var offsetMs = 0L
        val linesList = mutableListOf<LyricLine>()

        val rawLines = lrcContent.lines()
        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Check for offset tag
            val offsetMatcher = OFFSET_PATTERN.matcher(trimmed)
            if (offsetMatcher.find()) {
                offsetMs = offsetMatcher.group(1)?.toLongOrNull() ?: 0L
                continue
            }

            // Extract all timestamps in the line
            val timeMatcher = TIME_TAG_PATTERN.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastIndex = 0

            while (timeMatcher.find()) {
                val min = timeMatcher.group(1)?.toLongOrNull() ?: 0L
                val sec = timeMatcher.group(2)?.toLongOrNull() ?: 0L
                val msStr = timeMatcher.group(3)
                val millis = when {
                    msStr == null -> 0L
                    msStr.length == 2 -> msStr.toLong() * 10L
                    else -> msStr.take(3).padEnd(3, '0').toLong()
                }

                val totalMs = min * 60 * 1000L + sec * 1000L + millis + offsetMs
                timestamps.add(totalMs.coerceAtLeast(0L))
                lastIndex = timeMatcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val lyricText = trimmed.substring(lastIndex).trim()
                if (lyricText.isNotEmpty()) {
                    for (ts in timestamps) {
                        linesList.add(LyricLine(timeMs = ts, text = lyricText))
                    }
                }
            }
        }

        // Sort chronologically by timestamp
        val sortedLines = linesList.sortedBy { it.timeMs }

        return TrackLyric(
            trackId = trackId,
            isSynced = sortedLines.isNotEmpty(),
            lines = sortedLines,
            plainText = if (sortedLines.isEmpty()) lrcContent else null
        )
    }

    /**
     * Attempts to find a matching .lrc file in the same folder as the audio file.
     */
    fun findLocalLrcFile(audioFilePath: String?): String? {
        if (audioFilePath.isNullOrBlank()) return null
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) return null

        val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
        return if (lrcFile.exists() && lrcFile.isFile) {
            try {
                lrcFile.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
