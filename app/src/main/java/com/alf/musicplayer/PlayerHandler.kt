package com.alf.musicplayer

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.alf.musicplayer.service.Action
import com.alf.musicplayer.service.PlaylistBroadcastReceiver
import com.alf.musicplayer.service.PlaylistPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object PlayerHandler {

    private lateinit var playlistBroadcastReceiver: BroadcastReceiver
    private lateinit var sliderUpdateJob: Job

    private var isLandscape = false

    fun setup(activity: Activity, playlist: Playlist) {
        isLandscape =
            (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        if (isLandscape) {
            setupLandscape(activity)
        } else {
            setupPortrait(activity)
        }

        val layout: View = activity.findViewById(R.id.include)
        layout.setOnClickListener {
            val intent = Intent(activity, PlayingActivity::class.java)
            intent.putExtra("playlist_data", playlist.toString())
            activity.startActivity(intent)
        }

        startUpdatingSlider(activity)

        val playlistText: TextView =
            if (isLandscape) activity.findViewById(R.id.landPlaylistName) else activity.findViewById(
                R.id.playlistName
            )
        playlistText.text = PlaylistPlayerManager.getPlaylistName()

        val musicText: TextView =
            if (isLandscape) activity.findViewById(R.id.landMusicName) else activity.findViewById(R.id.musicName)
        musicText.text = PlaylistPlayerManager.getMusicName()
        musicText.isSelected = true

        val playPauseButton: ImageButton =
            if (isLandscape) activity.findViewById(R.id.landPlayPauseButton) else activity.findViewById(
                R.id.playPauseButton
            )

        val likeButton: ImageButton? =
            if (isLandscape) activity.findViewById(R.id.landLikeButton) else null
        val favouriteButton: ImageButton? =
            if (isLandscape) activity.findViewById(R.id.landFavouriteButton) else null

        playlistBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != null) {
                    musicText.text = PlaylistPlayerManager.getMusicName()
                }
                if (isLandscape) {
                    likeButton?.setImageResource(if (PlaylistPlayerManager.canBeLiked()) R.drawable.ic_like_empty else R.drawable.ic_like)
                    favouriteButton?.setImageResource(if (PlaylistPlayerManager.canBeFavourite()) R.drawable.ic_love_empty else R.drawable.ic_love)
                }
                playPauseButton.setImageResource(if (PlaylistPlayerManager.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play)
                updateSlider(activity)
                if (PlaylistPlayerManager.isPlaying()) {
                    if (!sliderUpdateJob.isActive) startUpdatingSlider(activity)
                } else {
                    if (sliderUpdateJob.isActive) stopUpdatingSlider()
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(Action.UPDATE)
            addAction(Action.STOP)
        }
        activity.registerReceiver(playlistBroadcastReceiver, intentFilter)
    }

    private fun setupLandscape(activity: Activity) {
        val favouriteButton: ImageButton = activity.findViewById(R.id.landFavouriteButton)
        val previousButton: ImageButton = activity.findViewById(R.id.landPreviousButton)
        val likeButton: ImageButton = activity.findViewById(R.id.landLikeButton)
        val playPauseButton: ImageButton = activity.findViewById(R.id.landPlayPauseButton)
        val dislikeButton: ImageButton = activity.findViewById(R.id.landDislikeButton)
        val skipButton: ImageButton = activity.findViewById(R.id.landSkipButton)

        val seekBar: SeekBar = activity.findViewById(R.id.seekBar)

        likeButton.setImageResource(if (PlaylistPlayerManager.canBeLiked()) R.drawable.ic_like_empty else R.drawable.ic_like)

        favouriteButton.setImageResource(if (PlaylistPlayerManager.canBeFavourite()) R.drawable.ic_love_empty else R.drawable.ic_love)

        playPauseButton.setOnClickListener {
            val action = if (PlaylistPlayerManager.isPlaying()) Action.PAUSE else Action.PLAY
            activity.sendBroadcast(
                Intent(activity, PlaylistBroadcastReceiver::class.java).setAction(action)
            )
        }
        skipButton.setOnClickListener {
            activity.sendBroadcast(
                Intent(activity, PlaylistBroadcastReceiver::class.java).setAction(Action.SKIP)
            )
        }
        favouriteButton.setOnClickListener {
            activity.sendBroadcast(
                Intent(
                    activity, PlaylistBroadcastReceiver::class.java
                ).setAction(if (!PlaylistPlayerManager.canBeFavourite()) Action.UNDO_LOVE else Action.LOVE)
            )
        }
        likeButton.setOnClickListener {
            activity.sendBroadcast(
                Intent(
                    activity, PlaylistBroadcastReceiver::class.java
                ).setAction(if (!PlaylistPlayerManager.canBeLiked()) Action.UNDO_LIKE else Action.LIKE)
            )
        }
        dislikeButton.setOnClickListener {
            activity.sendBroadcast(
                Intent(
                    activity, PlaylistBroadcastReceiver::class.java
                ).setAction(Action.DISLIKE)
            )
        }
        previousButton.setOnClickListener {
            activity.sendBroadcast(
                Intent(
                    activity, PlaylistBroadcastReceiver::class.java
                ).setAction(Action.RESET)
            )
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val updateIntent =
                    Intent(activity, PlaylistBroadcastReceiver::class.java).setAction(
                        Action.UPDATE
                    ).putExtra("set_time", seekBar.progress)
                activity.sendBroadcast(updateIntent)
            }
        })

        startUpdatingSlider(activity)
    }

    private fun setupPortrait(activity: Activity) {
        val previousButton: ImageButton = activity.findViewById(R.id.previousButton)
        val playPauseButton: ImageButton = activity.findViewById(R.id.playPauseButton)
        val skipButton: ImageButton = activity.findViewById(R.id.skipButton)
        val musicName: TextView = activity.findViewById(R.id.musicName)
        musicName.isSelected = true

        playPauseButton.setOnClickListener {
            val action = if (PlaylistPlayerManager.isPlaying()) Action.PAUSE else Action.PLAY
            activity.sendBroadcast(
                Intent(activity, PlaylistBroadcastReceiver::class.java).setAction(action)
            )
        }
        skipButton.setOnClickListener {
            activity.sendBroadcast(
                Intent(activity, PlaylistBroadcastReceiver::class.java).setAction(Action.SKIP)
            )
        }
        previousButton.setOnClickListener {
            activity.sendBroadcast(
                Intent(activity, PlaylistBroadcastReceiver::class.java).setAction(Action.RESET)
            )
        }
    }

    private fun updateSlider(activity: Activity) {
        if (isLandscape) {
            val seekBar: SeekBar = activity.findViewById(R.id.seekBar) ?: return
            seekBar.max = PlaylistPlayerManager.getDuration()
            seekBar.progress = PlaylistPlayerManager.getTime()
        } else {
            val progressBar: ProgressBar = activity.findViewById(R.id.progressBar) ?: return
            progressBar.max = PlaylistPlayerManager.getDuration()
            progressBar.progress = PlaylistPlayerManager.getTime()
        }
    }

    private fun startUpdatingSlider(activity: Activity) {
        sliderUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateSlider(activity)
                delay(1000L)
            }
        }
    }

    private fun stopUpdatingSlider() {
        sliderUpdateJob.cancel()
    }

    fun destroy() {
        stopUpdatingSlider()
    }
}