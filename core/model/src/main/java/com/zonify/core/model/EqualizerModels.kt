package com.zonify.core.model

/**
 * Filter types for Parametric Equalizer (PEQ).
 */
enum class FilterType {
    PEAKING,
    LOW_SHELF,
    HIGH_SHELF,
    LOW_PASS,
    HIGH_PASS,
    BAND_PASS,
    NOTCH
}

/**
 * Single band in a Parametric Equalizer.
 */
data class ParametricBand(
    val id: Int,
    val isEnabled: Boolean = true,
    val frequencyHz: Float,    // 20 Hz to 20,000 Hz
    val gainDb: Float,          // -15 dB to +15 dB
    val qFactor: Float = 1.414f,// 0.1 to 10.0
    val filterType: FilterType = FilterType.PEAKING
)

/**
 * Complete Equalizer Profile (supports both 10-Band Graphic and Parametric).
 */
data class EqualizerProfile(
    val id: String,
    val name: String,
    val isParametric: Boolean = false,
    val preampDb: Float = 0f,
    val graphicBands: List<Float> = List(10) { 0f }, // 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz
    val parametricBands: List<ParametricBand> = emptyList(),
    val isAutoEqPreset: Boolean = false,
    val headphoneTargetName: String? = null
)

/**
 * Audiophile playback output modes.
 */
enum class AudioOutputMode {
    STANDARD_AUDIO_TRACK,        // Default Android AudioTrack / AudioFlinger
    AAUDIO_EXCLUSIVE,            // Google AAudio Low Latency exclusive stream
    BIT_PERFECT_USB_DIRECT,      // Direct hardware bypass to USB DAC (no resampling)
    OPENSL_ES                    // Fallback low level
}

/**
 * ReplayGain mode to prevent ear-blasting or clipping across tracks.
 */
data class ReplayGainSettings(
    val isEnabled: Boolean = true,
    val mode: Mode = Mode.ALBUM_PREFERRED,
    val preAmpDb: Float = 0f,
    val preventClipping: Boolean = true
) {
    enum class Mode {
        TRACK,
        ALBUM,
        ALBUM_PREFERRED
    }
}
