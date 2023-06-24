package com.alf.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alf.musicplayer.databinding.ActivityMenuBinding
import com.alf.musicplayer.service.PlaylistPlayerManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

private const val TAG = "MenuActivityTAG"
private const val PERMISSION_REQUEST_CODE = 100

private val allowedExtension = listOf("mp3", "mp4")

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private lateinit var appFolder: String

    private lateinit var folderList: MutableList<Playlist>

    private var permissionDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appFolder = this.filesDir.path
        Log.i(TAG, "App folder : $appFolder")

        folderList = mutableListOf()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = FolderAdapter(folderList,
            { position -> onPlayClicked(position) },
            { position -> onPlaylistSelection(position) })
    }

    override fun onResume() {
        super.onResume()

        if (PlaylistPlayerManager.isPlaying()) {
            PlayerHandler.setup(this, PlaylistPlayerManager.playlist)
            binding.include.root.visibility = View.VISIBLE
        } else binding.include.root.visibility = View.GONE

        Log.d(TAG, "onResume called")
        if ((ActivityCompat.checkSelfPermission(
                this, permission()
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            Log.d(TAG, "Has permission")
            findPlaylists()
        } else if (shouldShowRequestPermissionRationale(permission())) {
            Log.d(TAG, "Already denied permission request, asking for manual intervention")
            requestSettingsEdit()
        } else {
            Log.d(TAG, "Doesn't have permission, asking built-in permission request")
            ActivityCompat.requestPermissions(
                this, arrayOf(permission()), PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (PlaylistPlayerManager.isPlaying()) PlayerHandler.destroy()
    }

    private fun onPlayClicked(position: Int) {
        val intent = Intent(this, PlayingActivity::class.java)
        intent.putExtra("playlist_data", folderList[position].toString())
        startActivity(intent)
    }

    private fun onPlaylistSelection(position: Int) {
        val intent = Intent(this, PlaylistActivity::class.java)
        intent.putExtra("path", folderList[position].filePath)
        startActivity(intent)
    }

    private fun permission(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return Manifest.permission.READ_MEDIA_AUDIO
        return Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private fun requestManualSettingsChange() {
        Log.d(TAG, "Manual permission request")
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun requestSettingsEdit() {
        Log.d(TAG, "Displaying AlertDialog")
        permissionDialog?.dismiss()
        permissionDialog = MaterialAlertDialogBuilder(this).setTitle("Permission non-accordée")
            .setMessage("Cette permission est nécessaire, puisque l'application a besoin d'accéder aux musiques de l'appareil.")
            .setNegativeButton("Quitter") { _, _ ->
                finish()
            }.setPositiveButton("Accéder aux paramètres") { _, _ ->
                requestManualSettingsChange()
            }.setCancelable(false).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    findPlaylists()
                } else {
                    requestSettingsEdit()
                }
            }

            else -> requestSettingsEdit()
        }
    }

    private fun findPlaylists() {
        folderList.clear()
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).path
        val files = File(path).listFiles() ?: return
        for (file in files) {
            if (isMusicDirectory(file.path)) folderList += Playlist.load(file.path, appFolder)
        }
        if (folderList.isEmpty()) binding.noPlaylist.visibility = View.VISIBLE
        else binding.noPlaylist.visibility = View.INVISIBLE
        binding.recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun isMusicDirectory(path: String): Boolean {
        val files = File(path).listFiles()
        Log.d(TAG, "Checking $path")

        if (files == null || files.isEmpty()) return false

        return files.fold(true) { acc, file ->
            // Log.d(TAG, "-> Found ${file.name} in directory $path")
            if (file.isDirectory || file.extension !in allowedExtension) false
            else acc
        }
    }
}
