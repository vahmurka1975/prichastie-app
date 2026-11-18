package ru.example.prichastie.audio

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerController(context: Context) {

    private val app = context.applicationContext
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    /** id шага, который сейчас загружен в плеер. */
    private val _currentId = MutableStateFlow<String?>(null)
    val currentId: StateFlow<String?> = _currentId

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playing.value = isPlaying
        }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            _currentId.value = item?.mediaId
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val f = MediaController.Builder(app, token).buildAsync()
        future = f
        f.addListener({
            val c = f.get()
            controller = c
            c.addListener(listener)
            _playing.value = c.isPlaying
            _currentId.value = c.currentMediaItem?.mediaId
        }, ContextCompat.getMainExecutor(app))
    }

    /** Запустить аудио шага из res/raw. */
    fun play(stepId: String, rawResId: Int, title: String) {
        val c = controller ?: return
        val item = MediaItem.Builder()
            .setMediaId(stepId)
            .setUri(RawResourceDataSource.buildRawResourceUri(rawResId))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist("Последование ко Причащению")
                    .build()
            )
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    fun pause() { controller?.pause() }
    fun resume() { controller?.play() }
    fun seekBy(ms: Long) {
        val c = controller ?: return
        c.seekTo((c.currentPosition + ms).coerceAtLeast(0))
    }

    fun release() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        controller = null
        future = null
    }
}