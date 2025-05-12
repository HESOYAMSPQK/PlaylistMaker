package com.example.playlistmaker

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistory(private val prefs: SharedPreferences) {
    companion object {
        private const val PREF_KEY = "search_history"
        private const val MAX_SIZE = 10
    }

    private val gson = Gson()
    private val typeToken = object : TypeToken<MutableList<Track>>() {}.type

    fun getAll(): List<Track> {
        val json = prefs.getString(PREF_KEY, null) ?: return emptyList()
        return gson.fromJson(json, typeToken)
    }


    fun add(track: Track) {
        val list = getAll().toMutableList()
        list.removeAll { it.trackId == track.trackId }
        list.add(0, track)
        if (list.size > 10) list.subList(10, list.size).clear()
        prefs.edit()
            .putString(PREF_KEY, gson.toJson(list))
            .apply()
    }



    fun clear() {
        prefs.edit()
            .remove(PREF_KEY)
            .apply()
    }
}
