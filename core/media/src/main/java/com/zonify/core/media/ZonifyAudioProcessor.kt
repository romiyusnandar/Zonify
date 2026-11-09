package com.zonify.core.media

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.zonify.core.audio.AudioEngineBridge
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Media3 AudioProcessor bridging decoded audio into the C++ DSP & Equalizer engine.
 * Includes direct zero-overhead passthrough when DSP is inactive and safe format handling.
 */
class ZonifyAudioProcessor : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT || inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            AudioEngineBridge.initialize(
                sampleRate = inputAudioFormat.sampleRate,
                channels = inputAudioFormat.channelCount,
                bitPerfect = AudioEngineBridge.isBitPerfectActive.value
            )
            // Output format matches input format
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)

        // When Equalizer is disabled, pass audio straight through without any transformation
        if (!AudioEngineBridge.isEqualizerActive.value) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val channels = inputAudioFormat.channelCount

        if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            val numFrames = remaining / (channels * 4)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            if (numFrames > 0) {
                AudioEngineBridge.processPcmFloat(outputBuffer, numFrames, channels)
            }
        } else if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            val numFrames = remaining / (channels * 2)
            val totalSamples = numFrames * channels

            if (totalSamples > 0) {
                val floatArray = FloatArray(totalSamples)
                for (i in 0 until totalSamples) {
                    floatArray[i] = inputBuffer.getShort() / 32768.0f
                }

                // Temporary direct float buffer for native C++ DSP
                val directBuffer = ByteBuffer.allocateDirect(totalSamples * 4)
                    .order(ByteOrder.nativeOrder())
                directBuffer.asFloatBuffer().put(floatArray)

                AudioEngineBridge.processPcmFloat(directBuffer, numFrames, channels)
                directBuffer.rewind()

                val processedFloats = FloatArray(totalSamples)
                directBuffer.asFloatBuffer().get(processedFloats)

                for (i in 0 until totalSamples) {
                    val sample = (processedFloats[i] * 32767.0f).toInt().coerceIn(-32768, 32767)
                    outputBuffer.putShort(sample.toShort())
                }
                outputBuffer.flip()
            } else {
                outputBuffer.put(inputBuffer)
                outputBuffer.flip()
            }
        } else {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
        }
        inputBuffer.position(inputBuffer.limit())
    }
}
