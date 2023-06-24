package com.alf.musicplayer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.alf.musicplayer.Playlist


private const val TAG = "PlayerServiceTAG"

class PlayerService : Service() {

    lateinit var playlist: Playlist

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == null) {
            val json = intent?.getStringExtra("playlist_data")!!
            playlist = Playlist.readJson(json)
            if (PlaylistPlayerManager.isSamePlaylist(playlist)) PlaylistPlayerManager.updateContext(
                this
            )
            else PlaylistPlayerManager.initialize(this, playlist)

            this.sendBroadcast(
                Intent(this, PlaylistBroadcastReceiver::class.java).setAction(
                    Action.UPDATE
                )
            )
        } else if (intent.action == Action.STOP) {
            Log.d(TAG, "Called with ACTION_STOP")
            onDestroy()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        playlist.save()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopService(Intent(this, PlayerService::class.java))
        PlaylistPlayerManager.stop()
    }

}