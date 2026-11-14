package com.alad.app.presentation.player

import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.alad.app.core.player.DelayedMedia3Player
import kotlinx.coroutines.delay

@Composable
fun AlsadidubPlayerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val controller = remember { DelayedMedia3Player(context) }
    var url by remember { mutableStateOf("") }
    var delaySeconds by remember { mutableFloatStateOf(10f) }
    var volume by remember { mutableFloatStateOf(1f) }
    var isLoaded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf("لینک مستقیم ویدیو را وارد کنید.") }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    LaunchedEffect(controller, isLoaded) {
        while (isLoaded) {
            position = controller.audioPlayer.currentPosition.coerceAtLeast(0L)
            duration = controller.audioPlayer.duration.takeIf { it > 0L } ?: 0L
            isPlaying = controller.audioPlayer.isPlaying || controller.audioPlayer.playWhenReady
            delay(400)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(" بازگشت")
            }
            Text("Alsadidub Player", style = MaterialTheme.typography.titleLarge)
        }

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("لینک مستقیم ویدیو") },
            placeholder = { Text("https://example.com/video.mp4") },
            singleLine = true
        )

        Button(
            onClick = {
                try {
                    controller.load(url)
                    controller.setAudioVolume(volume)
                    controller.setDelay(delaySeconds)
                    isLoaded = true
                    isPlaying = false
                    position = 0L
                    message = "ویدیو آماده پخش است."
                } catch (error: Exception) {
                    message = error.message ?: "بارگذاری ویدیو ناموفق بود."
                    isLoaded = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("بارگذاری لینک") }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    player = controller.videoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = controller.videoPlayer }
        )

        Text(message, style = MaterialTheme.typography.bodyMedium)

        Text("تأخیر تصویر: ${delaySeconds.toInt()} ثانیه")
        Slider(
            value = delaySeconds,
            onValueChange = {
                delaySeconds = it
                controller.setDelay(it)
            },
            valueRange = 0f..30f,
            enabled = isLoaded
        )

        Text("صدای اصلی: ${(volume * 100).toInt()}٪")
        Slider(
            value = volume,
            onValueChange = {
                volume = it
                controller.setAudioVolume(it)
            },
            valueRange = 0f..1f,
            enabled = isLoaded
        )

        if (duration > 0L) {
            Slider(
                value = position.coerceAtMost(duration).toFloat(),
                onValueChange = { position = it.toLong() },
                onValueChangeFinished = { controller.seekToAudioPosition(position) },
                valueRange = 0f..duration.toFloat(),
                enabled = isLoaded
            )
            Text("${formatTime(position)} / ${formatTime(duration)}")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (isPlaying) {
                        controller.pause()
                        isPlaying = false
                    } else {
                        controller.play()
                        isPlaying = true
                    }
                },
                enabled = isLoaded,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Text(if (isPlaying) "توقف موقت" else "پخش")
            }
            Button(
                onClick = {
                    controller.stop()
                    isLoaded = false
                    isPlaying = false
                    position = 0L
                    message = "پخش متوقف شد."
                },
                enabled = isLoaded,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Text("توقف")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
