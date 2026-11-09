package com.zonify.feature.library.scanner

import android.net.Uri
import com.zonify.core.model.AudioCodec
import com.zonify.core.model.AudioFormatInfo
import com.zonify.core.model.DsdFormat
import com.zonify.core.model.Track
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Dedicated parser for Direct Stream Digital (DSF) audio files.
 * Extracts DSD sample rate, channel count, duration, and ID3v2 tags.
 */
object DsdMetadataExtractor {

    private const val DSF_MAGIC = "DSD "
    private const val FMT_MAGIC = "fmt "

    fun parseDsfFile(file: File): Track? {
        if (!file.exists() || !file.name.endsWith(".dsf", ignoreCase = true)) {
            return null
        }

        try {
            RandomAccessFile(file, "r").use { raf ->
                val headerBytes = ByteArray(28)
                raf.readFully(headerBytes)

                val magic = String(headerBytes, 0, 4, Charsets.US_ASCII)
                if (magic != DSF_MAGIC) return null

                val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
                buffer.position(4)
                val dsdChunkSize = buffer.long
                val totalFileSize = buffer.long
                val metadataOffset = buffer.long

                // Read fmt chunk
                val fmtHeader = ByteArray(52)
                raf.readFully(fmtHeader)

                val fmtMagic = String(fmtHeader, 0, 4, Charsets.US_ASCII)
                if (fmtMagic != FMT_MAGIC) return null

                val fmtBuffer = ByteBuffer.wrap(fmtHeader).order(ByteOrder.LITTLE_ENDIAN)
                fmtBuffer.position(4)
                val fmtChunkSize = fmtBuffer.long
                val formatVersion = fmtBuffer.int
                val formatId = fmtBuffer.int
                val channelType = fmtBuffer.int
                val channelNum = fmtBuffer.int
                val samplingFreq = fmtBuffer.int
                val bitsPerSample = fmtBuffer.int
                val sampleCount = fmtBuffer.long

                val dsdFormat = when (samplingFreq) {
                    2822400 -> DsdFormat.DSD64
                    5644800 -> DsdFormat.DSD128
                    11289600 -> DsdFormat.DSD256
                    22579200 -> DsdFormat.DSD512
                    else -> DsdFormat.DSD64
                }

                val durationMs = if (samplingFreq > 0) {
                    (sampleCount * 1000L) / samplingFreq
                } else {
                    0L
                }

                // Parse ID3v2 tag at metadataOffset if present
                var title = file.nameWithoutExtension
                var artist = "Unknown Artist"
                var album = "Unknown Album"
                var trackNumber = 0
                var year: Int? = null

                if (metadataOffset in 28..<totalFileSize) {
                    raf.seek(metadataOffset)
                    val id3Header = ByteArray(10)
                    if (raf.read(id3Header) == 10 && id3Header[0] == 'I'.code.toByte() &&
                        id3Header[1] == 'D'.code.toByte() && id3Header[2] == '3'.code.toByte()
                    ) {
                        // Calculate ID3v2 synchsafe size
                        val tagSize = ((id3Header[6].toInt() and 0x7F) shl 21) or
                                ((id3Header[7].toInt() and 0x7F) shl 14) or
                                ((id3Header[8].toInt() and 0x7F) shl 7) or
                                (id3Header[9].toInt() and 0x7F)

                        val tagBytes = ByteArray(tagSize.coerceAtMost(1024 * 128))
                        raf.readFully(tagBytes)
                        val id3Map = parseId3Frames(tagBytes)

                        id3Map["TIT2"]?.let { title = it }
                        id3Map["TPE1"]?.let { artist = it }
                        id3Map["TALB"]?.let { album = it }
                        id3Map["TRCK"]?.let { trackNumber = it.split("/").firstOrNull()?.toIntOrNull() ?: 0 }
                        id3Map["TYER"]?.let { year = it.toIntOrNull() }
                    }
                }

                val audioFormat = AudioFormatInfo(
                    codec = AudioCodec.DSD,
                    sampleRateHz = samplingFreq,
                    bitDepth = bitsPerSample,
                    bitRateKbps = ((samplingFreq.toLong() * channelNum * bitsPerSample) / 1000).toInt(),
                    channels = channelNum,
                    isLossless = true,
                    isDsd = true,
                    dsdFormat = dsdFormat
                )

                return Track(
                    id = file.absolutePath.hashCode().toLong(),
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = durationMs,
                    uri = Uri.fromFile(file),
                    artworkUri = null,
                    trackNumber = trackNumber,
                    year = year,
                    audioFormat = audioFormat
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun parseId3Frames(tagBytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var offset = 0
        while (offset + 10 < tagBytes.size) {
            val frameId = String(tagBytes, offset, 4, Charsets.US_ASCII)
            if (frameId.all { it.isLetterOrDigit() }) {
                val frameSize = ((tagBytes[offset + 4].toInt() and 0xFF) shl 24) or
                        ((tagBytes[offset + 5].toInt() and 0xFF) shl 16) or
                        ((tagBytes[offset + 6].toInt() and 0xFF) shl 8) or
                        (tagBytes[offset + 7].toInt() and 0xFF)

                if (frameSize <= 0 || offset + 10 + frameSize > tagBytes.size) break

                val encoding = tagBytes[offset + 10].toInt()
                val textBytes = tagBytes.copyOfRange(offset + 11, offset + 10 + frameSize)
                val charset = when (encoding) {
                    1 -> Charsets.UTF_16
                    2 -> Charsets.UTF_16BE
                    3 -> Charsets.UTF_8
                    else -> Charsets.ISO_8859_1
                }
                val textValue = String(textBytes, charset).trim { it <= ' ' || it == '\u0000' }
                result[frameId] = textValue
                offset += 10 + frameSize
            } else {
                break
            }
        }
        return result
    }
}
