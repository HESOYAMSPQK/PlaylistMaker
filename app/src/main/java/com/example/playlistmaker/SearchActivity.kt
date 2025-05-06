package com.example.playlistmaker

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var clearButton: ImageView
    private var searchQuery: String = ""

    companion object {
        private const val SEARCH_QUERY_KEY = "SEARCH_QUERY_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        searchEditText = findViewById(R.id.searchEditText)
        clearButton = findViewById(R.id.clearButton)

        if (savedInstanceState != null) {
            searchQuery = savedInstanceState.getString(SEARCH_QUERY_KEY, "")
            searchEditText.setText(searchQuery)
        }

        searchEditText.doOnTextChanged { text, _, _, _ ->
            searchQuery = text?.toString().orEmpty()
            clearButton.isVisible = searchQuery.isNotEmpty()
        }

        clearButton.setOnClickListener {
            searchEditText.text.clear()
            clearButton.isVisible = false
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SEARCH_QUERY_KEY, searchQuery)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        searchQuery = savedInstanceState.getString(SEARCH_QUERY_KEY, "")
        searchEditText.setText(searchQuery)
    }
}
