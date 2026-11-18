package ru.example.prichastie.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

data class AudioUiState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
)

/** Пульт к плееру, который живёт в PlaybackService. */
class StepAudioController(context: Context) {

    private val appContext = context.applicationContext
    private var controller: MediaController? = null
    private var pending: MediaItem? = null
    private var released = false

    var state by mutableStateOf(AudioUiState())
        private set

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) &&
                player.playbackState == Player.STATE_ENDED
            ) {
                player.pause()
                player.seekTo(0)
            }
            refresh()
        }
    }

    private val future = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    ).buildAsync()

    init {
        future.addListener({ onConnected() }, ContextCompat.getMainExecutor(appContext))
    }

    private fun onConnected() {
        if (released) return
        val c = try {
            future.get()
        } catch (e: Exception) {
            return
        }
        controller = c
        c.addListener(listener)
        pending?.let { applyItem(c, it) }
        pending = null
        refresh()
    }

    private fun applyItem(c: MediaController, item: MediaItem) {
        c.setMediaItem(item)
        c.prepare()
    }

    fun load(uri: Uri, title: String) {
        val item = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist("Последование ко Причащению")
                    .build()
            )
            .build()
        val c = controller
        if (c != null) applyItem(c, item) else pending = item
        refresh()
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceAtLeast(0L))
        refresh()
    }

    fun skip(deltaMs: Long) {
        val c = controller ?: return
        val end = if (c.duration > 0) c.duration else Long.MAX_VALUE
        seekTo((c.currentPosition + deltaMs).coerceIn(0L, end))
    }

    fun setSpeed(speed: Float) {
        controller?.playbackParameters = PlaybackParameters(speed)
        refresh()
    }

    fun refresh() {
        val c = controller ?: return
        state = AudioUiState(
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition,
            durationMs = c.duration.coerceAtLeast(0L),
            speed = c.playbackParameters.speed,
        )
    }

    /** Уход с экрана шага: останавливаем звук, убираем уведомление, отключаемся. */
    fun release() {
        released = true
        controller?.let {
            it.removeListener(listener)
            it.stop()
            it.clearMediaItems()
        }
        controller = null
        MediaController.releaseFuture(future)
    }
}