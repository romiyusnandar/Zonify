package com.zonify.core.media

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.zonify.core.audio.AudioEngineBridge
import java.nio.ByteBuffer

/**
 * Media3 AudioProcessor bridging decoded audio directly into the C++ DSP & Equalizer engine.
 */
class ZonifyAudioProcessor : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Support PCM 32-bit float for maximum dynamic range without truncation
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            AudioEngineBridge.initialize(
                sampleRate = inputAudioFormat.sampleRate,
                channels = inputAudioFormat.channelCount,
                bitPerfect = AudioEngineBridge.isBitPerfectActive.value
            )
            inputAudioFormat
        } else {
            // Request ExoPlayer's resampler to feed us 32-bit Float PCM
            AudioProcessor.AudioFormat(
                inputAudioFormat.sampleRate,
                inputAudioFormat.channelCount,
                C.ENCODING_PCM_FLOAT
            )
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val bytesPerFrame = inputAudioFormat.channelCount * 4 // 4 bytes per float
        val numFrames = remaining / bytesPerFrame

        if (numFrames > 0) {
            val outputBuffer = replaceOutputBuffer(remaining)

            // Copy input into direct output buffer for in-place DSP transformation
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()

            // Run C++ Parametric Equalizer and limiter
            AudioEngineBridge.processPcmFloat(
                outputBuffer,
                numFrames,
                inputAudioFormat.channelCount
            )
        }
    }
}
