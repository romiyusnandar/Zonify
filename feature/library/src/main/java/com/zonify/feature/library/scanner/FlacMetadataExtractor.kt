package com.zonify.feature.library.scanner

import android.net.Uri
import com.zonify.core.model.AudioCodec
import com.zonify.core.model.AudioFormatInfo
import com.zonify.core.model.Track
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High performance FLAC metadata and STREAMINFO header extractor.
 */
object FlacMetadataExtractor {

    private const val FLAC_MAGIC = "fLaC"

    fun parseFlacFile(file: File): Track? {
        if (!file.exists() || !file.name.endsWith(".flac", ignoreCase = true)) {
            return null
        }

        try {
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                if (String(magic, Charsets.US_ASCII) != FLAC_MAGIC) return null

                var isLastBlock = false
                var sampleRate = 44100
                var channels = 2
                var bitDepth = 16
                var totalSamples = 0L

                var title = file.nameWithoutExtension
                var artist = "Unknown Artist"
                var album = "Unknown Album"
                var trackNumber = 0
                var year: Int? = null

                while (!isLastBlock) {
                    val headerByte = raf.read()
                    if (headerByte == -1) break
                    isLastBlock = (headerByte and 0x80) != 0
                    val blockType = headerByte and 0x7F

                    val lengthBytes = ByteArray(3)
                    raf.readFully(lengthBytes)
                    val blockLength = ((lengthBytes[0].toInt() and 0xFF) shl 16) or
                            ((lengthBytes[1].toInt() and 0xFF) shl 8) or
                            (lengthBytes[2].toInt() and 0xFF)

                    when (blockType) {
                        0 -> {
                            // STREAMINFO (34 bytes)
                            val streamInfo = ByteArray(blockLength)
                            raf.readFully(streamInfo)

                            // Bytes 10 to 17 contain sample rate, channels, bit depth, total samples
                            val b10 = streamInfo[10].toInt() and 0xFF
                            val b11 = streamInfo[11].toInt() and 0xFF
                            val b12 = streamInfo[12].toInt() and 0xFF
                            val b13 = streamInfo[13].toInt() and 0xFF
                            val b14 = streamInfo[14].toInt() and 0xFF
                            val b15 = streamInfo[15].toInt() and 0xFF
                            val b16 = streamInfo[16].toInt() and 0xFF
                            val b17 = streamInfo[17].toInt() and 0xFF

                            sampleRate = (b10 shl 12) or (b11 shl 4) or (b12 ushr 4)
                            channels = ((b12 ushr 1) and 0x07) + 1
                            bitDepth = (((b12 and 0x01) shl 4) or (b13 ushr 4)) + 1
                            val totalSamplesHigh = (b13 and 0x0F).toLong()
                            val totalSamplesLow = ((b14.toLong() and 0xFF) shl 24) or
                                    ((b15.toLong() and 0xFF) shl 16) or
                                    ((b16.toLong() and 0xFF) shl 8) or
                                    (b17.toLong() and 0xFF)
                            totalSamples = (totalSamplesHigh shl 32) or totalSamplesLow
                        }
                        4 -> {
                            // VORBIS_COMMENT
                            val commentBytes = ByteArray(blockLength)
                            raf.readFully(commentBytes)
                            val comments = parseVorbisComments(commentBytes)

                            comments["TITLE"]?.let { title = it }
                            comments["ARTIST"]?.let { artist = it }
                            comments["ALBUM"]?.let { album = it }
                            comments["TRACKNUMBER"]?.let { trackNumber = it.split("/").firstOrNull()?.toIntOrNull() ?: 0 }
                            comments["DATE"]?.let { year = it.take(4).toIntOrNull() }
                        }
                        else -> {
                            // Skip other blocks (PICTURE, PADDING, CUESHEET, etc.)
                            raf.skipBytes(blockLength)
                        }
                    }
                }

                val durationMs = if (sampleRate > 0) (totalSamples * 1000L) / sampleRate else 0L
                val bitrateKbps = if (durationMs > 0) ((file.length() * 8L) / durationMs).toInt() else 0

                val audioFormat = AudioFormatInfo(
                    codec = AudioCodec.FLAC,
                    sampleRateHz = sampleRate,
                    bitDepth = bitDepth,
                    bitRateKbps = bitrateKbps,
                    channels = channels,
                    isLossless = true
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

    private fun parseVorbisComments(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val vendorLength = buf.int
            buf.position(buf.position() + vendorLength) // skip vendor string

            val commentCount = buf.int
            for (i in 0 until commentCount) {
                if (buf.remaining() < 4) break
                val length = buf.int
                if (length <= 0 || buf.remaining() < length) break

                val commentBytes = ByteArray(length)
                buf.get(commentBytes)
                val commentStr = String(commentBytes, Charsets.UTF_8)
                val equalIndex = commentStr.indexOf('=')
                if (equalIndex > 0) {
                    val key = commentStr.substring(0, equalIndex).uppercase()
                    val value = commentStr.substring(equalIndex + 1)
                    result[key] = value
                }
            }
        } catch (e: Exception) {
            // Ignore corrupted comment block
        }
        return result
    }
}
