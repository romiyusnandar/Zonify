package com.zonify.core.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Generates an offline high-quality sample lossless audio preview (44.1kHz 16-bit stereo WAV)
 * so users can test audio playback and the parametric DSP equalizer immediately, even
 * before scanning local device music.
 */
object DemoAudioGenerator {

    private const val SAMPLE_RATE = 44100
    private const val NUM_CHANNELS = 2
    private const val DURATION_SECONDS = 5

    @Synchronized
    fun getOrCreateSampleUri(context: Context): Uri {
        val sampleFile = File(context.cacheDir, "zonify_audiophile_preview.wav")
        if (sampleFile.exists() && sampleFile.length() > 44) {
            return Uri.fromFile(sampleFile)
        }

        try {
            val totalFrames = SAMPLE_RATE * DURATION_SECONDS
            val totalAudioBytes = totalFrames * NUM_CHANNELS * 2
            val totalDataLen = totalAudioBytes + 36

            FileOutputStream(sampleFile).use { fos ->
                // Write 44-byte RIFF WAV header
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray())
                header.putInt(totalDataLen)
                header.put("WAVE".toByteArray())
                header.put("fmt ".toByteArray())
                header.putInt(16) // Subchunk1Size for PCM
                header.putShort(1.toShort()) // AudioFormat 1 = PCM
                header.putShort(NUM_CHANNELS.toShort())
                header.putInt(SAMPLE_RATE)
                header.putInt(SAMPLE_RATE * NUM_CHANNELS * 2) // ByteRate
                header.putShort((NUM_CHANNELS * 2).toShort()) // BlockAlign
                header.putShort(16.toShort()) // BitsPerSample
                header.put("data".toByteArray())
                header.putInt(totalAudioBytes)

                fos.write(header.array())

                // Generate a rich, harmonic musical chord (C major 9: C4, E4, G4, B4, D5)
                val chordFreqs = doubleArrayOf(261.63, 329.63, 392.00, 493.88, 587.33)
                val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)

                for (frame in 0 until totalFrames) {
                    val t = frame.toDouble() / SAMPLE_RATE
                    // Attack (0.05s) and gentle decay envelope
                    val attack = min(1.0, t / 0.05)
                    val decay = exp(-t / 2.0)
                    val envelope = attack * decay

                    var leftSignal = 0.0
                    var rightSignal = 0.0

                    for (idx in chordFreqs.indices) {
                        val freq = chordFreqs[idx]
                        val phase = 2.0 * PI * freq * t
                        val component = sin(phase)
                        // Slight stereo spread across harmonic voices
                        val panL = 0.4 + 0.2 * (idx % 2)
                        val panR = 0.4 + 0.2 * ((idx + 1) % 2)
                        leftSignal += component * panL
                        rightSignal += component * panR
                    }

                    val scale = 0.18 * envelope * 32767.0
                    val leftSample = (leftSignal * scale).toInt().coerceIn(-32768, 32767).toShort()
                    val rightSample = (rightSignal * scale).toInt().coerceIn(-32768, 32767).toShort()

                    if (buffer.remaining() < 4) {
                        fos.write(buffer.array(), 0, buffer.position())
                        buffer.clear()
                    }

                    buffer.putShort(leftSample)
                    buffer.putShort(rightSample)
                }

                if (buffer.position() > 0) {
                    fos.write(buffer.array(), 0, buffer.position())
                }
            }

            return Uri.fromFile(sampleFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return Uri.EMPTY
        }
    }
}
