package com.example.playlistmaker

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.playlistmaker.network.RetrofitClient
import com.example.playlistmaker.network.TrackResponse
import com.example.playlistmaker.SearchHistory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var clearButton: ImageButton
    private lateinit var tracksRecyclerView: RecyclerView
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var placeholderImage: ImageView
    private lateinit var placeholderText: TextView
    private lateinit var retryButton: Button

    private lateinit var searchHistory: SearchHistory
    private lateinit var historyContainer: View
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var clearHistoryButton: Button
    private lateinit var historyAdapter: TrackAdapter

    private var currentSearchResults: List<Track> = emptyList()
    private var currentHistoryList: List<Track> = emptyList()

    private var lastSearchText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)


        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        searchEditText     = findViewById(R.id.searchEditText)
        clearButton        = findViewById(R.id.clearButton)
        tracksRecyclerView = findViewById(R.id.tracksRecyclerView)
        placeholderImage   = findViewById(R.id.placeholderImage)
        placeholderText    = findViewById(R.id.placeholderText)
        retryButton        = findViewById(R.id.placeholderRetryButton)

        searchHistory      = SearchHistory(getSharedPreferences("search_history", MODE_PRIVATE))
        historyContainer   = findViewById(R.id.historyContainer)
        historyRecyclerView= findViewById(R.id.historyRecyclerView)
        clearHistoryButton = findViewById(R.id.clearHistoryButton)

        val initialHistory = searchHistory.getAll().toMutableList()
        currentHistoryList = initialHistory
        historyAdapter = TrackAdapter(initialHistory)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = historyAdapter

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
                    updateHistoryVisibility()
                    return true
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallow: Boolean) {}
        }

        )

        clearHistoryButton.setOnClickListener {
            searchHistory.clear()
            updateHistoryVisibility()
        }

        trackAdapter = TrackAdapter()
        tracksRecyclerView.layoutManager = LinearLayoutManager(this)
        tracksRecyclerView.adapter = trackAdapter

        val searchGesture = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent) = true
        })
        tracksRecyclerView.addOnItemTouchListener(object: RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val child = rv.findChildViewUnder(e.x, e.y) ?: return false
                if (searchGesture.onTouchEvent(e)) {
                    val pos = rv.getChildAdapterPosition(child)
                    val clicked = currentSearchResults.getOrNull(pos) ?: return false
                    searchHistory.add(clicked)
                    updateHistoryVisibility()
                    return true
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallow: Boolean) {}
        })

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) searchTracks(query)
                true
            } else false
        }

        clearButton.setOnClickListener {
            searchEditText.text.clear()
            clearButton.isVisible = false
            trackAdapter.updateTracks(emptyList())
            tracksRecyclerView.isVisible = false
            placeholderImage.isVisible = false
            placeholderText.isVisible = false
            retryButton.isVisible = false
            updateHistoryVisibility()
        }

        searchEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) { clearButton.isVisible = s?.isNotEmpty() == true }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { updateHistoryVisibility() }
        })

        retryButton.setOnClickListener {
            if (lastSearchText.isNotEmpty()) searchTracks(lastSearchText)
        }
        searchEditText.setOnFocusChangeListener { _, _ -> updateHistoryVisibility() }

        updateHistoryVisibility()
    }

    private fun searchTracks(query: String) {
        lastSearchText = query
        RetrofitClient.api.searchTracks(query)
            .enqueue(object: Callback<TrackResponse> {
                override fun onResponse(
                    call: Call<TrackResponse>,
                    response: Response<TrackResponse>
                ) {
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
                                ) {
                                    Track(
                                        trackName       = dto.trackName,
                                        artistName      = dto.artistName,
                                        artworkUrl100   = dto.artworkUrl100,
                                        trackId         = dto.trackId,
                                        trackTimeMillis = dto.trackTimeMillis
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
        tracksRecyclerView.isVisible = false
        placeholderImage.setImageResource(imageResId)
        placeholderImage.isVisible = true
        placeholderText.text = subtitle?.let { "$title\n$it" } ?: title
        placeholderText.isVisible = true
        retryButton.isVisible = showRetry
    }

    private fun showResults() {
        placeholderImage.isVisible = false
        placeholderText.isVisible = false
        retryButton.isVisible = false
        tracksRecyclerView.isVisible = true
    }

    private fun updateHistoryVisibility() {
        val historyList = searchHistory.getAll()
        currentHistoryList = historyList
        val shouldShow =
            searchEditText.hasFocus() &&
                    searchEditText.text.isEmpty() &&
                    historyList.isNotEmpty()

        historyContainer.isVisible = shouldShow
        if (shouldShow) {
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
