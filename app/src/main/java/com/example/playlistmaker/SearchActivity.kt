package com.example.playlistmaker

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.playlistmaker.network.TrackResponse
import com.example.playlistmaker.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var clearButton: ImageButton
    private lateinit var tracksRecyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var placeholderImage: ImageView
    private lateinit var placeholderText: TextView
    private lateinit var retryButton: Button
    private lateinit var searchProgressBar: ProgressBar

    private lateinit var searchHistory: SearchHistory
    private lateinit var historyContainer: View
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: TrackAdapter

    private var currentSearchResults: List<Track> = emptyList()
    private var currentHistoryList: List<Track> = emptyList()
    private var lastSearchText: String = ""

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val DEBOUNCE_DELAY = 2000L

    private var lastItemClickTime = 0L
    private val CLICK_DEBOUNCE_DELAY = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchEditText     = findViewById(R.id.searchEditText)
        clearButton        = findViewById(R.id.clearButton)
        tracksRecyclerView = findViewById(R.id.tracksRecyclerView)
        placeholderImage   = findViewById(R.id.placeholderImage)
        placeholderText    = findViewById(R.id.placeholderText)
        retryButton        = findViewById(R.id.placeholderRetryButton)
        searchProgressBar  = findViewById(R.id.searchProgressBar)
        historyContainer   = findViewById(R.id.historyContainer)
        historyRecyclerView= findViewById(R.id.historyRecyclerView)
        val prefs = getSharedPreferences("search_history_prefs", MODE_PRIVATE)
        searchHistory = SearchHistory(prefs)
        historyAdapter = TrackAdapter()
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = historyAdapter

        trackAdapter = TrackAdapter()
        tracksRecyclerView.layoutManager = LinearLayoutManager(this)
        tracksRecyclerView.adapter = trackAdapter

        val historyGesture = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent) = true
        })
        historyRecyclerView.addOnItemTouchListener(object: RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val child = rv.findChildViewUnder(e.x, e.y) ?: return false
                if (historyGesture.onTouchEvent(e)) {
                    val pos = rv.getChildAdapterPosition(child)
                    val clicked = currentHistoryList.getOrNull(pos) ?: return false
                    searchHistory.add(clicked)
                    startActivity(Intent(this@SearchActivity, AudioPlayerActivity::class.java).apply {
                        putExtra("track", clicked)
                    })
                    updateHistoryVisibility()
                    return true
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallow: Boolean) {}
        })

        val searchGesture = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent) = true
        })
        tracksRecyclerView.addOnItemTouchListener(object: RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val child = rv.findChildViewUnder(e.x, e.y) ?: return false
                if (searchGesture.onTouchEvent(e)) {
                    val now = System.currentTimeMillis()
                    if (now - lastItemClickTime < CLICK_DEBOUNCE_DELAY) return false
                    lastItemClickTime = now

                    val pos = rv.getChildAdapterPosition(child)
                    val clicked = currentSearchResults.getOrNull(pos) ?: return false
                    searchHistory.add(clicked)
                    startActivity(Intent(this@SearchActivity, AudioPlayerActivity::class.java).apply {
                        putExtra("track", clicked)
                    })
                    updateHistoryVisibility()
                    return true
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallow: Boolean) {}
        })

        clearButton.setOnClickListener {
            searchEditText.text.clear()
            clearButton.isVisible = false
            searchRunnable?.let { searchHandler.removeCallbacks(it) }
            trackAdapter.updateTracks(emptyList())
            tracksRecyclerView.isVisible = false
            placeholderImage.isVisible = false
            placeholderText.isVisible = false
            retryButton.isVisible = false
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    searchRunnable?.let {
                        searchHandler.removeCallbacks(it)
                        searchHandler.postDelayed(it, DEBOUNCE_DELAY)
                    } ?: run {
                        searchRunnable = Runnable { searchTracks(query) }.also {
                            searchHandler.postDelayed(it, DEBOUNCE_DELAY)
                        }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateHistoryVisibility()
            }
        })

        retryButton.setOnClickListener {
            if (lastSearchText.isNotEmpty()) searchTracks(lastSearchText)
        }

        updateHistoryVisibility()
    }

    private fun searchTracks(query: String) {
        lastSearchText = query
        searchProgressBar.isVisible = true
        tracksRecyclerView.isVisible = false
        placeholderImage.isVisible = false
        placeholderText.isVisible = false
        retryButton.isVisible = false

        RetrofitClient.api.searchTracks(query)
            .enqueue(object: Callback<TrackResponse> {
                override fun onResponse(call: Call<TrackResponse>, response: Response<TrackResponse>) {
                    searchProgressBar.isVisible = false

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
                                if (dto.trackId != null
                                    && dto.trackName != null
                                    && dto.artistName != null
                                    && dto.trackTimeMillis != null
                                    && dto.artworkUrl100 != null
                                    && dto.previewUrl != null
                                ) {
                                    Track(
                                        trackName        = dto.trackName,
                                        artistName       = dto.artistName,
                                        artworkUrl100    = dto.artworkUrl100,
                                        previewUrl       = dto.previewUrl,
                                        trackId          = dto.trackId,
                                        trackTimeMillis  = dto.trackTimeMillis,
                                        collectionName   = dto.collectionName,
                                        releaseDate      = dto.releaseDate,
                                        primaryGenreName = dto.primaryGenreName,
                                        country          = dto.country
                                    )
                                } else null
                            }
                            currentSearchResults = tracks
                            trackAdapter.updateTracks(tracks)
                            showResults()
                        }
                    } else {
                        showPlaceholder(
                            getDrawableId("placeholder_error"),
                            getString(R.string.connection_error_title),
                            getString(R.string.connection_error_subtitle),
                            true
                        )
                    }
                }
                override fun onFailure(call: Call<TrackResponse>, t: Throwable) {
                    searchProgressBar.isVisible = false
                    showPlaceholder(
                        getDrawableId("placeholder_error"),
                        getString(R.string.connection_error_title),
                        getString(R.string.connection_error_subtitle),
                        true
                    )
                }
            })
    }

    private fun showPlaceholder(
        imageResId: Int,
        title: String,
        subtitle: String?,
        showRetry: Boolean
    ) {
        searchProgressBar.isVisible = false
        tracksRecyclerView.isVisible = false
        placeholderImage.setImageResource(imageResId)
        placeholderImage.isVisible = true
        placeholderText.text = subtitle?.let { "$title\n$it" } ?: title
        placeholderText.isVisible = true
        retryButton.isVisible = showRetry
    }

    private fun showResults() {
        searchProgressBar.isVisible = false
        placeholderImage.isVisible = false
        placeholderText.isVisible = false
        retryButton.isVisible = false
        tracksRecyclerView.isVisible = true
    }

    private fun updateHistoryVisibility() {
        val historyList = searchHistory.getAll()
        currentHistoryList = historyList
        historyContainer.isVisible =
            searchEditText.hasFocus() &&
                    searchEditText.text.isEmpty() &&
                    historyList.isNotEmpty()
        if (historyContainer.isVisible) {
            historyAdapter.updateTracks(historyList)
        }
    }

    private fun getDrawableId(namePrefix: String): Int {
        val isDark = (resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val suffix = if (isDark) "dark_mode" else "light_mode"
        return resources.getIdentifier(
            "${namePrefix}_$suffix",
            "drawable",
            packageName
        )
    }
}
