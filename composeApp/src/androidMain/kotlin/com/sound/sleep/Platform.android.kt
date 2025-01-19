package com.sound.sleep

import android.os.Build
import android.app.Activity
import android.content.Context

import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import java.io.File
import java.io.FileOutputStream

//-----------------------------------------
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}
actual fun getPlatform(): Platform = AndroidPlatform()
//-----------------------------------------

// Implement AudioPlayer for Android using MediaPlayer
actual fun createAudioPlayer(filePath: String): AudioPlayer {
    return AndroidAudioPlayer(filePath)
}

class AndroidAudioPlayer(private val filePath: String) : AudioPlayer {
    private val mediaPlayer: MediaPlayer = MediaPlayer().apply {
        setDataSource(filePath)
        prepare()
    }

    override fun play() {
        mediaPlayer.start()
    }

    override fun pause() {
        mediaPlayer.pause()
    }

    override fun seekTo(ms: Long) {
        mediaPlayer.seekTo(ms.toInt())
    }

    override val currentPosition: Long
        get() = mediaPlayer.currentPosition.toLong()

    override val duration: Long
        get() = mediaPlayer.duration.toLong()

    // Clean up MediaPlayer resources
    fun release() {
        mediaPlayer.release()
    }
}

// Persistence using SharedPreferences
actual fun loadSavedState(): SavedState? {
    // Assuming you have access to a Context
    val context = getAppContext() ?: return null
    val prefs = context.getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
    val filePath = prefs.getString("filePath", null) ?: return null
    val position = prefs.getLong("position", 0L)
    return SavedState(filePath, position)
}

actual fun saveState(state: SavedState) {
    val context = getAppContext() ?: return
    val prefs = context.getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)
    with(prefs.edit()) {
        putString("filePath", state.filePath)
        putLong("position", state.position)
        apply()
    }
}

// File Picker Implementation
private var filePickerLauncher: ActivityResultLauncher<String>? = null
private var currentActivity: Activity? = null

actual suspend fun pickFile(initialDir: String?): String? {
    val activity = currentActivity ?: return null
    // Cast activity to MainActivity to access pickAudioFile
    return if (activity is MainActivity) {
        activity.pickAudioFile()
    } else {
        null
    }
}

// Helper to convert Uri to File Path (Simplified)
fun uriToFilePath(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    cursor.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex == -1) return null  // Ensure the column exists

            val displayName = it.getString(nameIndex)
            val file = File(context.cacheDir, displayName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file.absolutePath
        }
    }
    return null
}


// Mock function to get application context
fun getAppContext(): Context? {
    // Implement a way to provide application context, e.g., via a singleton or dependency injection
    return null // Replace with actual context retrieval
}
