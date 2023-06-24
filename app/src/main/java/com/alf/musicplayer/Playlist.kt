package com.alf.musicplayer

import android.util.Log
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "PlaylistTAG"

private val allowedExtension = listOf("mp3", "mp4")

private const val dataFile = "data.json"

@Serializable
data class Playlist(
    val filePath: String, val storagePath: String, val name: String, val musicList: List<Music>
) {

    companion object {
        fun readJson(json: String): Playlist {
            return Json.decodeFromString(serializer(), json)
        }

        fun load(filePath: String, storagePath: String): Playlist {
            val name = filePath.split("/").last()
            val exists = ensureFileExists(storagePath, name)
            val playlist = if (exists) {
                readJson(File("$storagePath/$name/$dataFile").readText())
            } else {
                Playlist(filePath, storagePath, name, getFileList(filePath))
            }
            playlist.setFrequencies()
            playlist.save()
            return playlist
        }

        private fun ensureFileExists(storagePath: String, name: String): Boolean {
            var res = true
            if (!File("$storagePath/$name").exists()) {
                Log.i(TAG, "Data folder not found, creating a new one")
                File("$storagePath/$name").mkdir()
                res = false
            }

            if (!File("$storagePath/$name/$dataFile").exists()) {
                Log.i(TAG, "Data file not found, creating a new one")
                File("$storagePath/$name/$dataFile").createNewFile()
                res = false
            }
            return res
        }

        private fun getFileList(filePath: String): List<Music> {
            return File(filePath).listFiles()!!.fold(listOf()) { acc, elm ->
                if (elm.extension in allowedExtension) acc + Music.load(
                    elm.nameWithoutExtension,
                    elm.path,
                    Music.defaultFrequency
                )
                else acc
            }
        }
    }

    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }

    private fun setFrequencies() {
        val result = getFileList(filePath)
        musicList.map {
            result.mapIndexed { index, music ->
                if (music.name == it.name) result[index].frequency = it.frequency
            }
        }
    }

    fun save() {
        ensureFileExists(storagePath, name)
        File("$storagePath/$name/$dataFile").writeText(this.toString())
    }

}
