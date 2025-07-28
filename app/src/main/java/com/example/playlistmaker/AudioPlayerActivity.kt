package com.example.playlistmaker

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide

class AudioPlayerActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var btnPlay: ImageButton
    private lateinit var progressView: TextView
    private lateinit var durationValue: TextView
    private lateinit var handler: Handler
    private lateinit var updateRunnable: Runnable

    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val track = intent.getParcelableExtra<Track>("track")
            ?: run { finish(); return }

        val coverView    = findViewById<ImageView>(R.id.playerCover)
        val titleView    = findViewById<TextView>(R.id.playerTrackName)
        val artistView   = findViewById<TextView>(R.id.playerArtistName)
        progressView     = findViewById(R.id.playerProgress)
        btnPlay          = findViewById(R.id.btnPlay)
        durationValue    = findViewById(R.id.rowDurationValue)
        val albumValue   = findViewById<TextView>(R.id.rowAlbumValue)
        val yearValue    = findViewById<TextView>(R.id.rowYearValue)
        val genreValue   = findViewById<TextView>(R.id.rowGenreValue)
        val countryValue = findViewById<TextView>(R.id.rowCountryValue)

        titleView.text  = track.trackName
        artistView.text = track.artistName

        albumValue.apply {
            if (track.collectionName.isNullOrEmpty()) visibility = View.GONE
            else { visibility = View.VISIBLE; text = track.collectionName }
        }
        yearValue.apply {
            if (track.releaseDate.isNullOrEmpty()) visibility = View.GONE
            else { visibility = View.VISIBLE; text = track.releaseDate.take(4) }
        }
        genreValue.apply {
            if (track.primaryGenreName.isNullOrEmpty()) visibility = View.GONE
            else { visibility = View.VISIBLE; text = track.primaryGenreName }
        }
        countryValue.apply {
            if (track.country.isNullOrEmpty()) visibility = View.GONE
            else { visibility = View.VISIBLE; text = track.country }
        }

        val fullMin = track.trackTimeMillis / 60000
        val fullSec = (track.trackTimeMillis % 60000) / 1000
        durationValue.text = String.format("%02d:%02d", fullMin, fullSec)
        progressView.text = "00:00"

        Glide.with(this)
            .load(track.getCoverArtwork())
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(coverView)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(track.previewUrl)
            setOnPreparedListener {
                btnPlay.setImageResource(R.drawable.ic_play_arrow)
                btnPlay.isEnabled = true
            }
            setOnCompletionListener {
                this@AudioPlayerActivity.isPlaying = false
                btnPlay.setImageResource(R.drawable.ic_play_arrow)
                progressView.text = "00:00"
                handler.removeCallbacks(updateRunnable)
            }
            prepareAsync()
        }

        btnPlay.isEnabled = false

        handler = Handler(Looper.getMainLooper())
        updateRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    val posSec = mp.currentPosition / 1000
                    val min = posSec / 60
                    val sec = posSec % 60
                    progressView.text = String.format("%02d:%02d", min, sec)
                    handler.postDelayed(this, 1000)
                }
            }
        }

        btnPlay.setOnClickListener {
            mediaPlayer?.let { mp ->
                if (!isPlaying) {
                    mp.start()
                    isPlaying = true
                    btnPlay.setImageResource(R.drawable.ic_stop)
                    handler.post(updateRunnable)
                } else {
                    mp.pause()
                    isPlaying = false
                    btnPlay.setImageResource(R.drawable.ic_play_arrow)
                    handler.removeCallbacks(updateRunnable)
                }
            }
        }
    }

    override fun onBackPressed() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) mp.stop()
            mp.release()
        }
        handler.removeCallbacks(updateRunnable)
        mediaPlayer = null
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        isPlaying = false
        btnPlay.setImageResource(R.drawable.ic_play_arrow)
        handler.removeCallbacks(updateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacks(updateRunnable)
    }
}
