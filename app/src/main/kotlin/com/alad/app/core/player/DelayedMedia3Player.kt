package com.alad.app.core.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays the source audio immediately and starts its video track after a configurable delay.
 * The video timeline is kept delayMs behind the audio timeline.
 */
class DelayedMedia3Player(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var delayedStartJob: Job? = null
    private var delayMs: Long = 10_000L

    val audioPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
    }

    val videoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
    }

    fun load(url: String) = load(url, url)

    fun load(audioUrl: String, videoUrl: String) {
        val cleanAudioUrl = audioUrl.trim()
        val cleanVideoUrl = videoUrl.trim()
        require(cleanAudioUrl.startsWith("https://") || cleanAudioUrl.startsWith("http://")) {
            "لینک صدای دریافت‌شده معتبر نیست."
        }
        require(cleanVideoUrl.startsWith("https://") || cleanVideoUrl.startsWith("http://")) {
            "لینک تصویر دریافت‌شده معتبر نیست."
        }
        stop()
        audioPlayer.setMediaItem(MediaItem.fromUri(cleanAudioUrl))
        videoPlayer.setMediaItem(MediaItem.fromUri(cleanVideoUrl))
        audioPlayer.prepare()
        videoPlayer.prepare()
    }

    fun setDelay(seconds: Float) {
        delayMs = (seconds.coerceIn(0f, 30f) * 1000f).toLong()
        if (audioPlayer.isPlaying || audioPlayer.playWhenReady) {
            play()
        } else {
            alignVideoToAudio()
        }
    }

    fun play() {
        delayedStartJob?.cancel()
        audioPlayer.playWhenReady = true
        val audioPosition = audioPlayer.currentPosition.coerceAtLeast(0L)
        val videoPosition = (audioPosition - delayMs).coerceAtLeast(0L)
        videoPlayer.seekTo(videoPosition)

        if (audioPosition >= delayMs) {
            videoPlayer.playWhenReady = true
        } else {
            videoPlayer.playWhenReady = false
            val remaining = delayMs - audioPosition
            delayedStartJob = scope.launch {
                delay(remaining)
                if (audioPlayer.playWhenReady) {
                    videoPlayer.seekTo(0L)
                    videoPlayer.playWhenReady = true
                }
            }
        }
    }

    fun pause() {
        delayedStartJob?.cancel()
        audioPlayer.pause()
        videoPlayer.pause()
    }

    fun seekToAudioPosition(positionMs: Long) {
        audioPlayer.seekTo(positionMs.coerceAtLeast(0L))
        val videoPosition = (positionMs - delayMs).coerceAtLeast(0L)
        videoPlayer.seekTo(videoPosition)
        if (audioPlayer.playWhenReady) play()
    }

    fun setAudioVolume(volume: Float) {
        audioPlayer.volume = volume.coerceIn(0f, 1f)
    }

    fun stop() {
        delayedStartJob?.cancel()
        audioPlayer.stop()
        videoPlayer.stop()
        audioPlayer.clearMediaItems()
        videoPlayer.clearMediaItems()
    }

    private fun alignVideoToAudio() {
        val position = (audioPlayer.currentPosition - delayMs).coerceAtLeast(0L)
        videoPlayer.seekTo(position)
    }

    fun release() {
        delayedStartJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        audioPlayer.release()
        videoPlayer.release()
    }
}
