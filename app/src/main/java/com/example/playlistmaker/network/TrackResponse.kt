package com.example.playlistmaker.network

data class TrackResponse(
    val resultCount: Int,
    val results: List<TrackDto>
)

data class TrackDto(
    val trackName: String?,
    val artistName: String?,
    val trackTimeMillis: Long?,
    val artworkUrl100: String?
)
