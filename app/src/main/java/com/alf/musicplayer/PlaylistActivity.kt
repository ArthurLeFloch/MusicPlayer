package com.alf.musicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alf.musicplayer.databinding.ActivityPlaylistBinding
import com.alf.musicplayer.service.PlaylistPlayerManager

private const val TAG = "PlaylistActivityTAG"

class PlaylistActivity : AppCompatActivity() {

    private lateinit var appFolder: String
    private lateinit var musicFolder: String

    private lateinit var playlist: Playlist
    private lateinit var musicList: MutableList<Music>

    private lateinit var binding: ActivityPlaylistBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appFolder = this.filesDir.path

        musicFolder = intent?.extras?.getString("path").toString()
        playlist = Playlist.load(musicFolder, appFolder)
        musicList = playlist.musicList.toMutableList()

        val actionBar: ActionBar? = supportActionBar
        actionBar?.title = playlist.name
        actionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = MusicAdapter(musicList)

        binding.shuffle.setOnClickListener {
            val intent = Intent(this, PlayingActivity::class.java)
            intent.putExtra("playlist_data", playlist.toString())
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (PlaylistPlayerManager.isPlaying()) {
            PlayerHandler.setup(this, PlaylistPlayerManager.playlist)
            binding.include.root.visibility = View.VISIBLE
        } else binding.include.root.visibility = View.GONE

        playlist = Playlist.load(musicFolder, appFolder)
        if (playlist.musicList.isEmpty()) {
            Log.e(TAG, "Music folder empty")
            finish()
        }
        musicList.clear()
        for (music in playlist.musicList) {
            musicList += music
        }
        binding.recyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        if (PlaylistPlayerManager.isPlaying()) PlaylistPlayerManager.savePlaylist()
        else playlist.save()
        if (PlaylistPlayerManager.isPlaying()) PlayerHandler.destroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}