package com.alf.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FolderAdapter(
    private val dataset: MutableList<Playlist>,
    private val onPlayClick: (Int) -> Unit,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<FolderAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val folderName: TextView = view.findViewById(R.id.folderName)
        val musicCount: TextView = view.findViewById(R.id.musicCount)
        val player: ImageButton = view.findViewById(R.id.launchPlaylist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout =
            LayoutInflater.from(parent.context).inflate(R.layout.folder_layout, parent, false)

        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.folderName.text = dataset[position].name
        holder.musicCount.text = dataset[position].musicList.size.toString()
        val musicCount = dataset[position].musicList.size
        if (musicCount > 1) holder.musicCount.text = "$musicCount musiques"
        else holder.musicCount.text = "$musicCount musique"
        holder.player.setOnClickListener { onPlayClick(position) }
        holder.itemView.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}