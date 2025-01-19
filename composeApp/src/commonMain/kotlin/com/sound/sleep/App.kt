package com.sound.sleep

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class SavedState(val filePath: String, val position: Long)

interface AudioPlayer {
    fun play()
    fun pause()
    fun seekTo(ms: Long)
    val currentPosition: Long
    val duration: Long
}

expect suspend fun pickFile(initialDir: String? = null): String?
expect fun createAudioPlayer(filePath: String): AudioPlayer
expect fun loadSavedState(): SavedState?
expect fun saveState(state: SavedState)

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    var currentFile by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("") }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var sleepMinutes by remember { mutableStateOf(0) }
    var sleepSeconds by remember { mutableStateOf(0) }
    var countdown by remember { mutableStateOf<Long?>(null) }
    var player: AudioPlayer? by remember { mutableStateOf(null) }

    // Load saved state on start
    LaunchedEffect(Unit) {
        loadSavedState()?.let { saved ->
            currentFile = saved.filePath
            position = saved.position
            if(saved.filePath.isNotEmpty()) {
                player = createAudioPlayer(saved.filePath)
                player?.seekTo(position)
                fileName = saved.filePath.substringAfterLast('/')
                duration = player?.duration ?: 0L
            }
        }
    }

    // Periodic updates: progress & countdown
    LaunchedEffect(isPlaying, countdown) {
        while (true) {
            delay(1000L)
            player?.let {
                position = it.currentPosition
                duration = it.duration
                saveState(SavedState(currentFile ?: "", position))
            }
            countdown?.let {
                if(it > 0) countdown = it - 1
                else {
                    player?.pause()
                    isPlaying = false
                    countdown = null
                }
            }
        }
    }

    Column {
        Text(text = fileName)
        Slider(
            value = position.toFloat(),
            onValueChange = { newPos ->
                position = newPos.toLong()
                player?.seekTo(position)
            },
            valueRange = 0f..(duration.toFloat())
        )
        Row {
            Button(onClick = {
                player?.seekTo((position - 30000).coerceAtLeast(0L))
            }) { Text("-30s") }
            Button(onClick = {
                if(isPlaying) { player?.pause(); isPlaying = false }
                else { player?.play(); isPlaying = true }
            }) { Text(if(isPlaying) "Pause" else "Play") }
            Button(onClick = {
                player?.seekTo((position + 30000).coerceAtMost(duration))
            }) { Text("+30s") }
        }
        Button(onClick = {
            scope.launch {
                val newFile = pickFile(currentFile)
                newFile?.let {
                    currentFile = it
                    fileName = it.substringAfterLast('/')
                    player = createAudioPlayer(it)
                    position = 0L
                    player?.seekTo(position)
                    duration = player?.duration ?: 0L
                }
            }
        }) { Text("Pick File") }

        // Sleep Timer UI
        Row {
            OutlinedTextField(
                value = sleepMinutes.toString(),
                onValueChange = { sleepMinutes = it.toIntOrNull() ?: 0 },
                label = { Text("Minutes") }
            )
            OutlinedTextField(
                value = sleepSeconds.toString(),
                onValueChange = { sleepSeconds = it.toIntOrNull() ?: 0 },
                label = { Text("Seconds") }
            )
            Button(onClick = {
                countdown = (sleepMinutes * 60 + sleepSeconds).toLong()
            }) { Text("Set Sleep Timer") }
        }
        countdown?.let { timeLeft ->
            Text("Sleep in: ${timeLeft / 60}:${timeLeft % 60}")
            Button(onClick = {
                countdown = (sleepMinutes * 60 + sleepSeconds).toLong()
            }) { Text("Reset Timer") }
        }
    }
}
