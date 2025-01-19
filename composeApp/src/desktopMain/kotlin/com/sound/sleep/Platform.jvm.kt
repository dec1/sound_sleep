package com.sound.sleep

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.*
import javax.swing.JFileChooser

//-----------------------------------------
class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}
actual fun getPlatform(): Platform = JVMPlatform()
//-----------------------------------------
actual suspend fun pickFile(initialDir: String?): String? {
    return withContext(Dispatchers.IO) {
        val fileChooser = JFileChooser(initialDir ?: ".")
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile.absolutePath
        } else {
            null
        }
    }
}



class DesktopAudioPlayer(private val filePath: String) : AudioPlayer {
    private val clip: Clip
    private val audioStream: AudioInputStream

    init {
        val file = File(filePath)
        audioStream = AudioSystem.getAudioInputStream(file)
        val format = audioStream.format
        val info = DataLine.Info(Clip::class.java, format)
        clip = AudioSystem.getLine(info) as Clip
        clip.open(audioStream)
    }

    override fun play() {
        clip.start()
    }

    override fun pause() {
        clip.stop()
    }

    override fun seekTo(ms: Long) {
        // Calculate frame position and seek
        val frameRate = clip.format.frameRate
        val frame = (ms * frameRate / 1000).toInt()
        clip.framePosition = frame.coerceIn(0, clip.frameLength)
    }

    override val currentPosition: Long
        get() = clip.microsecondPosition / 1000

    override val duration: Long
        get() = clip.microsecondLength / 1000

    fun close() {
        clip.close()
        audioStream.close()
    }
}

actual fun createAudioPlayer(filePath: String): AudioPlayer {
    return DesktopAudioPlayer(filePath)
}

actual fun releasePlayer(player: AudioPlayer?) {
    // No-op for JVM
}

actual fun loadSavedState(): SavedState? {
    val stateFile = File(System.getProperty("user.home"), ".sound_sleep_state")
    if (!stateFile.exists()) return null
    val lines = stateFile.readLines()
    if (lines.size >= 2) {
        val path = lines[0]
        val pos = lines[1].toLongOrNull() ?: 0L
        return SavedState(path, pos)
    }
    return null
}

actual fun saveState(state: SavedState) {
    val stateFile = File(System.getProperty("user.home"), ".sound_sleep_state")
    stateFile.writeText("${state.filePath}\n${state.position}")
}
