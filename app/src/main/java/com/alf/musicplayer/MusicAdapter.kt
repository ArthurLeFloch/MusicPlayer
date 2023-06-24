package com.alf.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MusicAdapter(
    private val dataset: MutableList<Music>
) : RecyclerView.Adapter<MusicAdapter.ItemViewHolder>() {

    companion object {
        private const val frequencyPrecision = 1000
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val musicName: TextView = view.findViewById(R.id.mMusicName)
        val frequencyProgressBar: ProgressBar = view.findViewById(R.id.frequencyProgressBar)
        val favouriteButton: ImageButton = view.findViewById(R.id.mFavouriteButton)
        val likeButton: ImageButton = view.findViewById(R.id.mLikeButton)
        val dislikeButton: ImageButton = view.findViewById(R.id.mDislikeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout =
            LayoutInflater.from(parent.context).inflate(R.layout.music_layout, parent, false)
        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.musicName.text = dataset[position].name
        holder.musicName.isSelected = true
        holder.frequencyProgressBar.max = frequencyToInt(Music.maxFrequency)
        updateData(holder, position)
        holder.favouriteButton.setOnClickListener {
            if (dataset[position].hasMaxFrequency()) {
                dataset[position].resetFrequency()
            } else {
                dataset[position].love()
            }
            updateData(holder, position)
        }
        holder.likeButton.setOnClickListener {
            dataset[position].like()
            updateData(holder, position)
        }
        holder.dislikeButton.setOnClickListener {
            dataset[position].dislike()
            updateData(holder, position)
        }
    }

    private fun updateData(holder: ItemViewHolder, position: Int) {
        holder.frequencyProgressBar.progress = frequencyToInt(dataset[position].frequency)
        if (dataset[position].hasMaxFrequency()) holder.favouriteButton.setImageResource(R.drawable.ic_love)
        else holder.favouriteButton.setImageResource(R.drawable.ic_love_empty)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    private fun frequencyToInt(frequency: Double): Int {
        return (frequencyPrecision * frequency).toInt()
    }
}