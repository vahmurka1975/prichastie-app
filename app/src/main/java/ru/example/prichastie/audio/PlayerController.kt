package ru.example.prichastie.audio

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
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

    /** Команда, которую попросили до подключения к сервису. Выполнится сразу после подключения. */
    private var pendingAction: ((MediaController) -> Unit)? = null

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    /** id шага, который сейчас загружен в плеер. */
    private val _currentId = MutableStateFlow<String?>(null)
    val currentId: StateFlow<String?> = _currentId

    /** Текущая скорость: 0.75, 1.0, 1.25 и т.д. */
    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed

    /** true, когда запись дослушана до конца. */
    private val _ended = MutableStateFlow(false)
    val ended: StateFlow<Boolean> = _ended

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playing.value = isPlaying
        }

        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            _currentId.value = item?.mediaId
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _speed.value = playbackParameters.speed
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _ended.value = playbackState == Player.STATE_ENDED
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
            _speed.value = c.playbackParameters.speed
            _ended.value = c.playbackState == Player.STATE_ENDED
            pendingAction?.invoke(c)
            pendingAction = null
        }, ContextCompat.getMainExecutor(app))
    }

    /** Выполнить действие сейчас, а если контроллер ещё не подключён, то после подключения. */
    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else pendingAction = action
    }

    /**
     * Запустить аудио шага из res/raw.
     * Если этот шаг уже загружен, продолжает с того же места (с начала, если запись закончилась).
     * [startMs] нужен, чтобы продолжить с сохранённой позиции.
     */
    fun play(stepId: String, rawResId: Int, title: String, startMs: Long = 0L) {
        withController { c ->
            if (c.currentMediaItem?.mediaId == stepId) {
                if (c.playbackState == Player.STATE_ENDED) c.seekTo(0)
                c.play()
                return@withController
            }
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
            c.setMediaItem(item, startMs.coerceAtLeast(0L))
            c.prepare()
            c.play()
        }
    }

    fun pause() { controller?.pause() }
    fun resume() { controller?.play() }

    fun stop() {
        controller?.run {
            stop()
            clearMediaItems()
        }
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceIn(0L, durationMs().takeIf { it > 0 } ?: Long.MAX_VALUE))
    }

    fun seekBy(ms: Long) {
        val c = controller ?: return
        seekTo(c.currentPosition + ms)
    }

    fun setSpeed(value: Float) { controller?.setPlaybackSpeed(value) }

    /** Текущая позиция в миллисекундах. Вызывайте раз в 250–500 мс, пока играет звук. */
    fun positionMs(): Long = controller?.currentPosition ?: 0L

    /** Длительность записи; 0, пока она неизвестна. */
    fun durationMs(): Long {
        val d = controller?.duration ?: C.TIME_UNSET
        return if (d == C.TIME_UNSET || d < 0) 0L else d
    }

    fun release() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        controller = null
        future = null
        pendingAction = null
    }
}