package com.alf.musicplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "PlaylistBroadcastReceiverTAG"

class PlaylistBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == null) return

        when (intent.action) {
            Action.RESET -> {
                PlaylistPlayerManager.seekTo(0)
            }

            Action.UPDATE -> {
                if (intent.hasExtra("set_time")) {
                    PlaylistPlayerManager.seekTo(intent.getIntExtra("set_time", 0))
                }
            }

            Action.PLAY -> {
                PlaylistPlayerManager.playMusic()
            }

            Action.PAUSE -> {
                PlaylistPlayerManager.pauseMusic()
            }

            Action.UNDO_LOVE -> {
                PlaylistPlayerManager.undoLove()
                PlaylistPlayerManager.isFavourite = false
            }

            Action.LOVE -> {
                PlaylistPlayerManager.loveMusic()
                PlaylistPlayerManager.isFavourite = true
            }

            Action.SKIP -> {
                PlaylistPlayerManager.skipMusic(context)
                PlaylistPlayerManager.savePlaylist()
            }

            Action.LIKE -> {
                PlaylistPlayerManager.likeMusic()
                PlaylistPlayerManager.isLiked = true
            }

            Action.UNDO_LIKE -> {
                PlaylistPlayerManager.undoLike()
                PlaylistPlayerManager.isLiked = false
            }

            Action.DISLIKE -> {
                PlaylistPlayerManager.dislikeMusic()
                PlaylistPlayerManager.skipMusic(context)
            }

            Action.STOP -> {
            }
        }
        Log.d(TAG, "Received ${intent.action} command")
        if (intent.action != Action.STOP) {
            PlaylistPlayerManager.createNotification(context)
            context.sendBroadcast(Intent(Action.UPDATE))
        }
    }
}