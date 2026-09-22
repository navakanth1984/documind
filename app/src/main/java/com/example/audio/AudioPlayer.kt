package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var isPlaying: Boolean = false
        private set

    suspend fun playBase64Audio(
        base64Audio: String,
        mimeType: String? = null,
        onCompletion: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        stop()

        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val suffix = when {
                mimeType?.contains("mp3") == true -> ".mp3"
                mimeType?.contains("ogg") == true -> ".ogg"
                else -> ".wav"
            }
            val tempFile = File(context.cacheDir, "gemini_tts_temp$suffix")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            withContext(Dispatchers.Main) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        this@AudioPlayer.isPlaying = false
                        onCompletion()
                    }
                    start()
                }
                this@AudioPlayer.isPlaying = true
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing audio", e)
            isPlaying = false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping audio", e)
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }
}
