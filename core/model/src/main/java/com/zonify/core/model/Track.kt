package com.zonify.core.model

import android.net.Uri

/**
 * Domain model representing an audio track with audiophile metadata.
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val durationMs: Long,
    val uri: Uri,
    val artworkUri: Uri? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int? = null,
    val genre: String? = null,
    val audioFormat: AudioFormatInfo
)

/**
 * Detailed technical audio metadata for audiophiles.
 */
data class AudioFormatInfo(
    val codec: AudioCodec,
    val sampleRateHz: Int,        // e.g. 44100, 96000, 192000, 384000
    val bitDepth: Int,            // e.g. 16, 24, 32
    val bitRateKbps: Int = 0,     // e.g. 900+ kbps for FLAC
    val channels: Int = 2,        // Stereo = 2, Mono = 1, 5.1 = 6
    val isLossless: Boolean = true,
    val isDsd: Boolean = false,   // Direct Stream Digital (DSD64, DSD128, etc.)
    val dsdFormat: DsdFormat? = null
) {
    val qualityBadge: AudioQualityBadge
        get() = when {
            isDsd -> AudioQualityBadge.DSD
            sampleRateHz > 48000 || bitDepth > 16 -> AudioQualityBadge.HI_RES_LOSSLESS
            isLossless -> AudioQualityBadge.LOSSLESS
            else -> AudioQualityBadge.HIGH_QUALITY
        }

    val displaySummary: String
        get() = buildString {
            append(codec.displayName)
            append(" • ")
            if (isDsd) {
                append(dsdFormat?.displayName ?: "DSD")
            } else {
                append("${bitDepth}-bit / ${sampleRateHz / 1000.0} kHz")
            }
        }
}

enum class AudioCodec(val displayName: String) {
    FLAC("FLAC"),
    ALAC("Apple Lossless (ALAC)"),
    WAV("WAV"),
    AIFF("AIFF"),
    DSD("DSD"),
    WAVPACK("WavPack"),
    APE("Monkey's Audio"),
    OPUS("Opus"),
    MP3("MP3"),
    AAC("AAC"),
    UNKNOWN("Unknown")
}

enum class DsdFormat(val displayName: String) {
    DSD64("DSD 64 (2.8 MHz)"),
    DSD128("DSD 128 (5.6 MHz)"),
    DSD256("DSD 256 (11.2 MHz)"),
    DSD512("DSD 512 (22.5 MHz)")
}

enum class AudioQualityBadge(val label: String) {
    LOSSLESS("Lossless"),
    HI_RES_LOSSLESS("Hi-Res Lossless"),
    DSD("DSD Native"),
    HIGH_QUALITY("High Quality")
}
