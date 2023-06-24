package com.alf.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.alf.musicplayer.databinding.ActivityPlayingBinding
import com.alf.musicplayer.service.Action
import com.alf.musicplayer.service.PlayerService
import com.alf.musicplayer.service.PlaylistBroadcastReceiver
import com.alf.musicplayer.service.PlaylistPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


private const val TAG = "PlayingActivityTAG"

class PlayingActivity : AppCompatActivity() {

    private lateinit var playlist: Playlist

    private lateinit var playlistBroadcastReceiver: BroadcastReceiver
    private lateinit var sliderUpdateJob: Job

    private lateinit var binding: ActivityPlayingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "Called onCreate")

        val json = intent.getStringExtra("playlist_data")!!
        playlist = Playlist.readJson(json)

        val actionBar: ActionBar? = supportActionBar
        actionBar?.title = playlist.name
        actionBar?.setDisplayHomeAsUpEnabled(true)

        binding.playerMusicName.isSelected = true

        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val updateIntent =
                    Intent(this@PlayingActivity, PlaylistBroadcastReceiver::class.java).setAction(
                        Action.UPDATE
                    ).putExtra("set_time", seekBar.progress)
                this@PlayingActivity.sendBroadcast(updateIntent)
            }
        })

        binding.playerPlayPauseButton.setOnClickListener {
            val action = if (PlaylistPlayerManager.isPlaying()) Action.PAUSE else Action.PLAY
            sendBroadcast(
                Intent(this, PlaylistBroadcastReceiver::class.java).setAction(action)
            )
        }
        binding.playerSkipButton.setOnClickListener {
            sendBroadcast(
                Intent(this, PlaylistBroadcastReceiver::class.java).setAction(Action.SKIP)
            )
        }
        binding.playerFavouriteButton.setOnClickListener {
            sendBroadcast(
                Intent(
                    this, PlaylistBroadcastReceiver::class.java
                ).setAction(if (!PlaylistPlayerManager.canBeFavourite()) Action.UNDO_LOVE else Action.LOVE)
            )
        }
        binding.playerLikeButton.setOnClickListener {
            sendBroadcast(
                Intent(
                    applicationContext, PlaylistBroadcastReceiver::class.java
                ).setAction(if (!PlaylistPlayerManager.canBeLiked()) Action.UNDO_LIKE else Action.LIKE)
            )
        }
        binding.playerDislikeButton.setOnClickListener {
            sendBroadcast(
                Intent(
                    applicationContext, PlaylistBroadcastReceiver::class.java
                ).setAction(Action.DISLIKE)
            )
        }

        startUpdatingSlider()

        playlistBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "Updated !")
                if (intent.action != null) {
                    binding.playerMusicName.text = PlaylistPlayerManager.getMusicName()
                    binding.playerPlaylistName.text = PlaylistPlayerManager.getPlaylistName()
                    intent.action?.let { Log.d(TAG, it) }
                }
                binding.playerLikeButton.setImageResource(if (PlaylistPlayerManager.canBeLiked()) R.drawable.ic_like_empty else R.drawable.ic_like)
                binding.playerFavouriteButton.setImageResource(if (PlaylistPlayerManager.canBeFavourite()) R.drawable.ic_love_empty else R.drawable.ic_love)
                binding.playerPlayPauseButton.setImageResource(if (PlaylistPlayerManager.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play)
                binding.duration.text = timeToText(PlaylistPlayerManager.getDuration())
                updateSlider()
                if (PlaylistPlayerManager.isPlaying()) {
                    if (!sliderUpdateJob.isActive) startUpdatingSlider()
                } else {
                    if (sliderUpdateJob.isActive) stopUpdatingSlider()
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(Action.UPDATE)
            addAction(Action.STOP)
        }
        registerReceiver(playlistBroadcastReceiver, intentFilter)

        Log.d(TAG, (playlist.name == PlaylistPlayerManager.getPlaylistName()).toString())
        val intent = Intent(this, PlayerService::class.java)
        intent.putExtra("playlist_data", playlist.toString())
        startService(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "on destroy called")
        if (sliderUpdateJob.isActive) sliderUpdateJob.cancel()
        unregisterReceiver(playlistBroadcastReceiver)
    }

    private fun updateSlider() {
        binding.seekBar.max = PlaylistPlayerManager.getDuration()
        binding.seekBar.progress = PlaylistPlayerManager.getTime()
    }

    private fun timeToText(time: Int): String {
        val value = time * 1.0f / 1000
        return value.toInt().seconds.toString()
    }

    private fun startUpdatingSlider() {
        sliderUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateSlider()
                binding.currentTime.text = timeToText(PlaylistPlayerManager.getTime())
                delay(1000L)
            }
        }
    }

    private fun stopUpdatingSlider() {
        sliderUpdateJob.cancel()
    }
}