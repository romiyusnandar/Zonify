package com.zonify.core.database.mapper

import android.net.Uri
import com.zonify.core.database.entity.TrackEntity
import com.zonify.core.model.AudioCodec
import com.zonify.core.model.AudioFormatInfo
import com.zonify.core.model.DsdFormat
import com.zonify.core.model.Track

object TrackMapper {

    fun toDomain(entity: TrackEntity): Track {
        val codec = try {
            AudioCodec.valueOf(entity.codec)
        } catch (_: Exception) {
            AudioCodec.UNKNOWN
        }

        val dsdFormat = entity.dsdFormat?.let {
            try {
                DsdFormat.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }

        val format = AudioFormatInfo(
            codec = codec,
            sampleRateHz = entity.sampleRateHz,
            bitDepth = entity.bitDepth,
            bitRateKbps = entity.bitRateKbps,
            channels = entity.channels,
            isLossless = entity.isLossless,
            isDsd = entity.isDsd,
            dsdFormat = dsdFormat
        )

        return Track(
            id = entity.id,
            title = entity.title,
            artist = entity.artist,
            album = entity.album,
            albumArtist = entity.albumArtist,
            durationMs = entity.durationMs,
            uri = Uri.parse(entity.uriString),
            artworkUri = entity.artworkUriString?.let { Uri.parse(it) },
            trackNumber = entity.trackNumber,
            discNumber = entity.discNumber,
            year = entity.year,
            genre = entity.genre,
            audioFormat = format
        )
    }

    fun toEntity(domain: Track, isFavorite: Boolean = false): TrackEntity {
        return TrackEntity(
            id = domain.id,
            title = domain.title,
            artist = domain.artist,
            album = domain.album,
            albumArtist = domain.albumArtist,
            durationMs = domain.durationMs,
            uriString = domain.uri.toString(),
            artworkUriString = domain.artworkUri?.toString(),
            trackNumber = domain.trackNumber,
            discNumber = domain.discNumber,
            year = domain.year,
            genre = domain.genre,
            codec = domain.audioFormat.codec.name,
            sampleRateHz = domain.audioFormat.sampleRateHz,
            bitDepth = domain.audioFormat.bitDepth,
            bitRateKbps = domain.audioFormat.bitRateKbps,
            channels = domain.audioFormat.channels,
            isLossless = domain.audioFormat.isLossless,
            isDsd = domain.audioFormat.isDsd,
            dsdFormat = domain.audioFormat.dsdFormat?.name,
            isFavorite = isFavorite
        )
    }
}
