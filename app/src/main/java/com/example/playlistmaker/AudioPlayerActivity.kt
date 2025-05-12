package com.example.playlistmaker

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide

class AudioPlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val track = intent.getParcelableExtra<Track>("track")
            ?: run {
                finish()
                return
            }

        val coverView      = findViewById<ImageView>(R.id.playerCover)
        val titleView      = findViewById<TextView>(R.id.playerTrackName)
        val artistView     = findViewById<TextView>(R.id.playerArtistName)
        val progressView   = findViewById<TextView>(R.id.playerProgress)
        val btnPlay        = findViewById<ImageButton>(R.id.btnPlay)
        val btnAddToPlaylist = findViewById<ImageButton>(R.id.btnAddToPlaylist)
        val btnFavorite    = findViewById<ImageButton>(R.id.btnFavorite)
        val durationValue  = findViewById<TextView>(R.id.rowDurationValue)
        val albumValue     = findViewById<TextView>(R.id.rowAlbumValue)
        val yearValue      = findViewById<TextView>(R.id.rowYearValue)
        val genreValue     = findViewById<TextView>(R.id.rowGenreValue)
        val countryValue   = findViewById<TextView>(R.id.rowCountryValue)



        titleView.text  = track.trackName
        artistView.text = track.artistName

        track.collectionName?.let {
            albumValue.visibility = View.VISIBLE
            albumValue.text       = it
        } ?: run {
            albumValue.visibility = View.GONE
        }

        track.releaseDate?.let {
            yearValue .visibility = View.VISIBLE
            yearValue .text       = it.take(4)
        } ?: run {
            yearValue .visibility = View.GONE
        }


        track.primaryGenreName?.let {
            genreValue .visibility = View.VISIBLE
            genreValue .text       = it
        } ?: run {
            genreValue .visibility = View.GONE
        }

        track.country?.let {
            countryValue .visibility = View.VISIBLE
            countryValue .text       = it
        } ?: run {
            countryValue .visibility = View.GONE
        }


        val minutes = track.trackTimeMillis / 60_000
        val seconds = (track.trackTimeMillis % 60_000) / 1000
        val durationText = String.format("%02d:%02d", minutes, seconds)

        progressView.text = durationText

        durationValue.text = durationText


        Glide.with(this)
            .load(track.getCoverArtwork())
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(coverView)


    }
}
