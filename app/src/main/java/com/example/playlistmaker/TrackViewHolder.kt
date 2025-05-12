package com.example.playlistmaker

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import java.util.Locale

class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val trackImage: ImageView = itemView.findViewById(R.id.trackImage)
    private val trackName: TextView   = itemView.findViewById(R.id.trackName)
    private val artistName: TextView  = itemView.findViewById(R.id.artistName)
    private val trackDurationText: TextView =
        itemView.findViewById(R.id.trackTime)


    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    fun bind(track: Track) {
        trackName.text  = track.trackName
        artistName.text = track.artistName

        val minutes = track.trackTimeMillis / 60_000
        val seconds = (track.trackTimeMillis % 60_000) / 1000
        trackDurationText.text =
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val url = track.artworkUrl100
        val padding = 5.dpToPx(itemView.context)
        val cornerRadius = 8.dpToPx(itemView.context)

        if (url.isNullOrBlank()) {
            trackImage.setPadding(padding, padding, padding, padding)
            trackImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
            Glide.with(itemView)
                .load(R.drawable.ic_placeholder)
                .into(trackImage)
        } else {
            trackImage.setPadding(0, 0, 0, 0)
            trackImage.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(itemView)
                .load(url)
                .transform(RoundedCorners(cornerRadius))
                .placeholder(R.drawable.ic_placeholder)
                .into(trackImage)
        }
    }
}
