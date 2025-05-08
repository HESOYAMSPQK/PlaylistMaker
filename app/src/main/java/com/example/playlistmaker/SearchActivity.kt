package com.example.playlistmaker

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.playlistmaker.network.RetrofitClient
import com.example.playlistmaker.network.TrackResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var clearButton: ImageButton
    private lateinit var tracksRecyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var placeholderImage: ImageView
    private lateinit var placeholderText: TextView
    private lateinit var retryButton: Button

    private var lastSearchText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }


        searchEditText = findViewById(R.id.searchEditText)
        clearButton = findViewById(R.id.clearButton)
        tracksRecyclerView = findViewById(R.id.tracksRecyclerView)
        placeholderImage = findViewById(R.id.placeholderImage)
        placeholderText = findViewById(R.id.placeholderText)
        retryButton = findViewById(R.id.placeholderRetryButton)

        trackAdapter = TrackAdapter()
        tracksRecyclerView.layoutManager = LinearLayoutManager(this)
        tracksRecyclerView.adapter = trackAdapter

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchTracks(query)
                }
                true
            } else {
                false
            }
        }

        clearButton.setOnClickListener {
            searchEditText.text.clear()
            clearButton.isVisible = false
            trackAdapter.updateTracks(emptyList())
            tracksRecyclerView.isVisible = false
            placeholderImage.isVisible = false
            placeholderText.isVisible = false
            retryButton.isVisible = false
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                clearButton.isVisible = s?.isNotEmpty() == true
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        retryButton.setOnClickListener {
            if (lastSearchText.isNotEmpty()) {
                searchTracks(lastSearchText)
            }
        }
    }

    private fun searchTracks(query: String) {
        lastSearchText = query
        RetrofitClient.api.searchTracks(query)
            .enqueue(object : Callback<TrackResponse> {
                override fun onResponse(call: Call<TrackResponse>, response: Response<TrackResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val results = response.body()!!.results
                        if (results.isEmpty()) {
                            showPlaceholder(
                                getDrawableId("placeholder_no_results"),
                                getString(R.string.no_results),
                                null,
                                false
                            )
                        } else {
                            val tracks = results.mapNotNull { dto ->
                                if (dto.trackName != null && dto.artistName != null && dto.trackTimeMillis != null && dto.artworkUrl100 != null) {
                                    Track(
                                        dto.trackName,
                                        dto.artistName,
                                        SimpleDateFormat("mm:ss", Locale.getDefault()).format(dto.trackTimeMillis),
                                        dto.artworkUrl100
                                    )
                                } else null
                            }
                            trackAdapter.updateTracks(tracks)
                            showResults()
                        }
                    } else {
                        val title = getString(R.string.connection_error_title)
                        val subtitle = getString(R.string.connection_error_subtitle)
                        showPlaceholder(
                            getDrawableId("placeholder_error"),
                            title,
                            subtitle,
                            true
                        )
                    }
                }

                override fun onFailure(call: Call<TrackResponse>, t: Throwable) {
                    val title = getString(R.string.connection_error_title)
                    val subtitle = getString(R.string.connection_error_subtitle)
                    showPlaceholder(
                        getDrawableId("placeholder_error"),
                        title,
                        subtitle,
                        true
                    )
                }
            })
    }

    private fun showPlaceholder(imageResId: Int, title: String, subtitle: String?, showRetry: Boolean) {
        tracksRecyclerView.isVisible = false
        placeholderImage.setImageResource(imageResId)
        placeholderImage.isVisible = true
        placeholderText.text = if (subtitle != null) "$title\n$subtitle" else title
        placeholderText.isVisible = true
        retryButton.isVisible = showRetry
    }

    private fun showResults() {
        placeholderImage.isVisible = false
        placeholderText.isVisible = false
        retryButton.isVisible = false
        tracksRecyclerView.isVisible = true
    }

    private fun getDrawableId(namePrefix: String): Int {
        val isDarkTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val suffix = if (isDarkTheme) "dark_mode" else "light_mode"
        val resName = "${namePrefix}_$suffix"
        return resources.getIdentifier(resName, "drawable", packageName)
    }
}
