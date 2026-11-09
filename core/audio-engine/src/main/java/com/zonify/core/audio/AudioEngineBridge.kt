package com.zonify.core.audio

import com.zonify.core.model.FilterType
import com.zonify.core.model.ParametricBand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kotlin singleton bridge to the high-performance C++ audio engine.
 */
object AudioEngineBridge {

    init {
        System.loadLibrary("zonify_audio_engine")
    }

    private val _isBitPerfectActive = MutableStateFlow(false)
    val isBitPerfectActive: StateFlow<Boolean> = _isBitPerfectActive.asStateFlow()

    private val _sampleRate = MutableStateFlow(44100)
    val sampleRate: StateFlow<Int> = _sampleRate.asStateFlow()

    fun initialize(sampleRate: Int = 44100, channels: Int = 2, bitPerfect: Boolean = false): Boolean {
        _sampleRate.value = sampleRate
        _isBitPerfectActive.value = bitPerfect
        return nativeInit(sampleRate, channels, bitPerfect)
    }

    fun start() = nativeStart()
    fun pause() = nativePause()
    fun stop() = nativeStop()
    fun release() = nativeRelease()

    fun setBitPerfect(enabled: Boolean) {
        _isBitPerfectActive.value = enabled
        nativeSetBitPerfect(enabled)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        nativeSetEqualizerEnabled(enabled)
    }

    fun setPreamp(preampDb: Float) {
        nativeSetPreamp(preampDb)
    }

    fun updateParametricBand(band: ParametricBand) {
        nativeUpdateBand(
            bandId = band.id,
            enabled = band.isEnabled,
            filterType = band.filterType.ordinal,
            freqHz = band.frequencyHz,
            gainDb = band.gainDb,
            q = band.qFactor
        )
    }

    fun processPcmFloat(buffer: java.nio.ByteBuffer, numFrames: Int, channels: Int) {
        nativeProcessPcmFloat(buffer, numFrames, channels)
    }

    // Native JNI functions
    private external fun nativeInit(sampleRate: Int, channels: Int, bitPerfect: Boolean): Boolean
    private external fun nativeStart()
    private external fun nativePause()
    private external fun nativeStop()
    private external fun nativeRelease()
    private external fun nativeSetBitPerfect(enabled: Boolean)
    private external fun nativeSetEqualizerEnabled(enabled: Boolean)
    private external fun nativeSetPreamp(preampDb: Float)
    private external fun nativeUpdateBand(
        bandId: Int,
        enabled: Boolean,
        filterType: Int,
        freqHz: Float,
        gainDb: Float,
        q: Float
    )
    private external fun nativeProcessPcmFloat(
        buffer: java.nio.ByteBuffer,
        numFrames: Int,
        channels: Int
    )
}
