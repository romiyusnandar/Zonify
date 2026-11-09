package com.zonify.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["isFavorite"])
    ]
)
data class TrackEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val durationMs: Long,
    val uriString: String,
    val artworkUriString: String? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int? = null,
    val genre: String? = null,
    val codec: String,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val bitRateKbps: Int = 0,
    val channels: Int = 2,
    val isLossless: Boolean = true,
    val isDsd: Boolean = false,
    val dsdFormat: String? = null,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
