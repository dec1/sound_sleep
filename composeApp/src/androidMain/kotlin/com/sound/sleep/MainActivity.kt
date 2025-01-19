package com.sound.sleep

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    private lateinit var filePickerLauncher: ActivityResultLauncher<String>
    private var filePickContinuation: (String?) -> Unit = {}

    companion object {
        var instance: MainActivity? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            val filePath = uri?.let { uriToFilePath(this, it) }
            filePickContinuation(filePath)  // resume the suspended function
        }

        // Initialize Compose UI
        setContent {
            App()
        }
    }

    suspend fun pickAudioFile(): String? = suspendCancellableCoroutine { cont ->
        filePickContinuation = { result -> cont.resume(result) }
        filePickerLauncher.launch("audio/*")
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()  // Call the composable function defined in App.kt
}


