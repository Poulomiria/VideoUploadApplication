package com.example.videouploadapplication
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.viewModelScope


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoFrameExtractorScreen()
        }
    }
}

@Composable
fun VideoFrameExtractorScreen() {
    val viewModel = remember { VideoFrameExtractorViewModel() }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { viewModel.extractFrames(context) },
            enabled = !viewModel.isExtractingFrames && !viewModel.isVideoSelected
        ) {
            Text("Select Video")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isExtractingFrames) {
            LinearProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Extracting Frames...")
        }

        viewModel.framesDirectory?.let { framesDir ->
            Text(
                text = "Frames saved at: ${framesDir.absolutePath}",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
@Preview
fun PreviewVideoFrameExtractorScreen() {
    VideoFrameExtractorScreen()
}

class VideoFrameExtractorViewModel : ViewModel() {
    var isVideoSelected by mutableStateOf(false)
    var isExtractingFrames by mutableStateOf(false)
    var framesDirectory: File? = null

    fun extractFrames(context: Context) {
        viewModelScope.launch {
            isExtractingFrames = true
            framesDirectory = convertVideoToFrames(context)
            isExtractingFrames = false
        }
    }

    private suspend fun convertVideoToFrames(context: Context): File {
        val framesDirectory = createFramesDirectory(context)

        // Implement frame extraction logic
        // Example: using MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        try{
            val frameFiles = mutableListOf<File>()
            retriever.setDataSource("/storage/emulated/0/Android/data/com.example.videouploadapplication/files/frames/Mutton.mp4") // Replace videoUri with your video URI

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLong() ?: 0
            val frameInterval = duration / NUM_FRAMES

            for (i in 0 until NUM_FRAMES) {
                val frameTime = i * frameInterval * 1000 // Convert to microseconds
                val frameBitmap = retriever.getFrameAtTime(frameTime, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                val frameFile = File(framesDirectory, "frame_$i.jpg")
                // Save the frame bitmap to the file
                frameBitmap?.let { saveFrameToFile(it, frameFile) }

                // Add the file to the list
                frameFile?.let { frameFiles.add(it) }
                //frameBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(frameFile))
            }

            retriever.release()

        }catch(ex : Exception)
        {
            println("err")
            //Text("error")
        }

        return framesDirectory
    }
    private fun saveFrameToFile(frameBitmap: Bitmap, frameFile: File) {
        try {
            FileOutputStream(frameFile).use { outputStream ->
                frameBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        } catch (e: Exception) {
            // Log the exception
            e.printStackTrace()
        }
    }


    private fun createFramesDirectory(context: Context): File {
        val storageDir = File(context.getExternalFilesDir(null), "frames").apply { mkdirs() }
        return storageDir
    }

    companion object {
        private const val NUM_FRAMES = 1440 // Number of frames to extract from the video
    }
}
