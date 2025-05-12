package com.example.playlistmaker

data class Track(
    val trackName:String,
    val artistName:String,
    val artworkUrl100:String?,
    val trackId:Long,
    val trackTimeMillis:Long
)
