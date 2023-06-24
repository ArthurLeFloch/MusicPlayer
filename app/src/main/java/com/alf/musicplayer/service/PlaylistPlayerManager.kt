package com.alf.musicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.alf.musicplayer.MenuActivity
import com.alf.musicplayer.Playlist
import com.alf.musicplayer.R
import java.util.Random

private const val TAG = "PlaylistPlayerManagerTAG"

object PlaylistPlayerManager {
    lateinit var playlist: Playlist
    private lateinit var random: Random

    private var maxFrequency: Double = -1.0
    private var currentId = -1

    var isLiked = false
    var isFavourite = false

    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private val mediaPlayer = MediaPlayer()

    private const val NOTIFICATION_CHANNEL_ID = "playlist_player_channel"
    private const val NOTIFICATION_ID = 1

    fun initialize(context: Context, musicList: Playlist) {
        Log.d(TAG, "Initialize called")
        random = Random()
        playlist = musicList
        maxFrequency = playlist.musicList.sumOf { it.frequency }
        mediaPlayer.reset()
        skipMusic(context)
        updateContext(context)
    }

    fun updateContext(context: Context) {
        initializeNotification(context)
        initializeMediaSession(context)
        (context as Service).startForeground(NOTIFICATION_ID, createNotification(context))
    }

    fun stop() {
        Log.d(TAG, "Stop called")
        mediaPlayer.reset()
        stopNotification()
        stopMediaSession()
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    fun isSamePlaylist(newPlaylist: Playlist): Boolean {
        if (!PlaylistPlayerManager::playlist.isInitialized) return false
        return (newPlaylist.name == playlist.name)
    }

    fun skipMusic(context: Context) {
        isLiked = false
        isFavourite = false
        savePlaylist()
        currentId = nextMusicId()
        mediaPlayer.reset()
        mediaPlayer.setDataSource(playlist.musicList[currentId].filePath)
        mediaPlayer.prepare()
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            Log.d(TAG, "Music ended")
            context.sendBroadcast(
                Intent(context, PlaylistBroadcastReceiver::class.java).setAction(Action.SKIP)
            )
        }
    }

    fun playMusic() {
        mediaPlayer.start()
    }

    fun pauseMusic() {
        mediaPlayer.pause()
    }

    fun seekTo(pos: Int) {
        mediaPlayer.seekTo(pos)
    }

    private fun initializeNotification(context: Context) {
        val name = "Playback"
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun createNotification(context: Context): Notification {
        Log.d(TAG, "createNotification called")

        updateMediaSession()

        val openIntent = Intent(
            context, MenuActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val (icon, name, pendingIntent) = if (mediaPlayer.isPlaying) {
            Triple(R.drawable.ic_pause, "Pause", sendAction(context, Action.PAUSE))
        } else {
            Triple(R.drawable.ic_play, "Play", sendAction(context, Action.PLAY))
        }

        val likeIcon = if (!canBeLiked()) R.drawable.ic_like else R.drawable.ic_like_empty
        val likeIntent = if (!canBeLiked()) sendAction(context, Action.UNDO_LIKE) else sendAction(
            context, Action.LIKE
        )

        val builder = PlaybackStateCompat.Builder()
        builder.setState(
            (if (mediaPlayer.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED).toInt(),
            getTime().toLong(),
            1.0f
        )
        builder.setActions(
            PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SEEK_TO
        ).addCustomAction(Action.LIKE, "Like", likeIcon)
            .addCustomAction(Action.DISLIKE, "Dislike", R.drawable.ic_dislike)

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                val action = mediaButtonEvent.action
                if (Intent.ACTION_MEDIA_BUTTON == action) {
                    val event =
                        mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as? KeyEvent
                    if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                        when (event.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY -> onPlay()
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> onPause()
                            KeyEvent.KEYCODE_MEDIA_NEXT -> onSkipToNext()
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> onSkipToPrevious()
                        }
                        return true
                    }
                }
                return false
            }

            override fun onPlay() {
                super.onPlay()
                actionPendingIntent(context, Action.PLAY).send()
            }

            override fun onPause() {
                super.onPause()
                actionPendingIntent(context, Action.PAUSE).send()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                actionPendingIntent(context, Action.SKIP).send()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                actionPendingIntent(context, Action.RESET).send()
            }

            override fun onCustomAction(action: String?, extras: Bundle?) {
                super.onCustomAction(action, extras)

                if (action == Action.DISLIKE) actionPendingIntent(context, Action.DISLIKE).send()
                if (action == Action.LIKE) likeIntent.send()
            }

            override fun onSeekTo(pos: Long) {
                context.sendBroadcast(
                    actionIntent(context, Action.UPDATE).putExtra(
                        "set_time", pos.toInt()
                    )
                )
            }
        })
        mediaSession.setPlaybackState(builder.build())

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.playlist_play).setContentTitle(playlist.name)
            .setContentIntent(openPendingIntent).setOnlyAlertOnce(true).setShowWhen(false)
            .addAction(R.drawable.ic_dislike, "Dislike", sendAction(context, Action.DISLIKE))
            .addAction(R.drawable.ic_previous, "Reset", sendAction(context, Action.RESET))
            .addAction(icon, name, pendingIntent) // Play / Pause action
            .addAction(R.drawable.ic_skip, "Skip", sendAction(context, Action.SKIP))
            .addAction(likeIcon, "Like", likeIntent)
            .setDeleteIntent(sendAction(context, Action.STOP)).setAutoCancel(true)
            .setOngoing(mediaPlayer.isPlaying).setStyle(
                MediaStyle().setShowActionsInCompactView(0, 2, 4)
                    .setMediaSession(mediaSession.sessionToken)
            ).setSilent(true).build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        return notification
    }

    private fun stopNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun initializeMediaSession(context: Context) {
        mediaSession = MediaSessionCompat(context, "PlayerService").apply {
            setCallback(MediaSessionCallback())
            isActive = true
        }
        MediaSessionCallback.listener = { pos ->
            mediaPlayer.seekTo(pos)
            updateMediaSession()
            context.sendBroadcast(
                Intent(context, PlaylistBroadcastReceiver::class.java).setAction(
                    Action.UPDATE
                )
            )
        }
    }

    private fun updateMediaSession() {
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder().setState(
                if (mediaPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                mediaPlayer.currentPosition.toLong(),
                1.0f
            ).setActions(PlaybackStateCompat.ACTION_SEEK_TO).build()
        )
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder().apply {
                // Some tricks here to force the display of playlist name
                putString(MediaMetadata.METADATA_KEY_ARTIST, playlist.name)
                putString(MediaMetadata.METADATA_KEY_TITLE, playlist.musicList[currentId].name)
                putLong(MediaMetadata.METADATA_KEY_DURATION, mediaPlayer.duration.toLong())
            }.build()
        )
    }

    private fun stopMediaSession() {
        mediaSession.release()
    }

    private fun nextMusicId(): Int {
        maxFrequency = playlist.musicList.sumOf { it.frequency }
        val randomFrequency = random.nextDouble() * maxFrequency
        var previousFrequency = 0.0
        for (i in playlist.musicList.indices) {
            if (randomFrequency - previousFrequency <= playlist.musicList[i].frequency) return i
            previousFrequency += playlist.musicList[i].frequency
        }
        Log.wtf(TAG, "nextMusicId hasn't found a new music")
        return -1
    }

    private fun sendAction(context: Context, action: String): PendingIntent {
        return toPendingIntent(context, actionIntent(context, action))
    }

    private fun toPendingIntent(context: Context, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(context: Context, action: String): Intent {
        return Intent(context, PlaylistBroadcastReceiver::class.java).setAction(action)
    }

    private fun actionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, PlaylistBroadcastReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun savePlaylist() {
        playlist.save()
    }

    fun getPlaylistName(): String {
        return if (!PlaylistPlayerManager::playlist.isInitialized) "" else playlist.name
    }

    fun getMusicName(): String {
        return if (currentId == -1) "" else playlist.musicList[currentId].name
    }

    fun getDuration(): Int {
        return mediaPlayer.duration
    }

    fun getTime(): Int {
        return mediaPlayer.currentPosition
    }

    fun undoLove() {
        playlist.musicList[currentId].undoLove()
    }

    fun loveMusic() {
        playlist.musicList[currentId].love()
    }

    fun canBeLiked(): Boolean {
        return !isLiked and !playlist.musicList[currentId].hasMaxFrequency()
    }

    fun canBeFavourite(): Boolean {
        return !playlist.musicList[currentId].hasMaxFrequency()
    }

    fun undoLike() {
        playlist.musicList[currentId].undoLike()
    }

    fun likeMusic() {
        playlist.musicList[currentId].like()
    }

    fun dislikeMusic() {
        playlist.musicList[currentId].dislike()
    }

    private class MediaSessionCallback : MediaSessionCompat.Callback() {
        companion object {
            var listener: ((Int) -> Unit)? = null
        }

        override fun onSeekTo(pos: Long) {
            listener?.let { it(pos.toInt()) }
        }
    }
}