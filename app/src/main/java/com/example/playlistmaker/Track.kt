package com.example.playlistmaker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Track(
    val trackName:String,
    val artistName:String,
    val artworkUrl100:String?,
    val trackId:Long,
    val trackTimeMillis:Long,
    val collectionName: String?,
    val releaseDate: String?,
    val primaryGenreName: String?,
    val country: String?
) : Parcelable {

    fun getCoverArtwork(): String =
        artworkUrl100
            ?.replaceAfterLast('/', "512x512bb.jpg")
            ?: ""
}