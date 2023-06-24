package com.alf.musicplayer

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
data class Music(
    val name: String, val filePath: String, var frequency: Double
) {
    companion object {
        const val maxFrequency = 2.0
        const val minFrequency = 0.1
        const val defaultFrequency = 1.0

        fun load(name: String, filePath: String, frequency: Double): Music {
            return Music(name, filePath, frequency)
        }
    }

    fun hasMaxFrequency(): Boolean {
        return frequency == maxFrequency
    }

    fun undoLove() {
        frequency = defaultFrequency
    }

    fun love() {
        frequency = maxFrequency
    }

    fun resetFrequency() {
        frequency = defaultFrequency
    }

    fun like() {
        frequency = min(frequency + 0.1, maxFrequency)
    }

    fun undoLike() {
        frequency = max(minFrequency, frequency - 0.1)
    }

    fun dislike() {
        frequency = max(frequency / 1.1, minFrequency)
    }
}
