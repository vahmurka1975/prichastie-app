package ru.example.prichastie.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import java.util.UUID

data class AudioUiState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
)

/** Пульт к плееру, который живёт в PlaybackService. */
class StepAudioController(context: Context) {

    companion object {
        private const val CONTINUE_WINDOW_MS = 15_000L

        @Volatile
        private var continueUntil = 0L

        /** Запись дослушана до конца: следующий шаг должен сам начать играть. */
        fun markContinue() {
            continueUntil = SystemClock.elapsedRealtime() + CONTINUE_WINDOW_MS
        }

        /** Забирает флаг «продолжить». Срабатывает один раз и только в течение 15 секунд. */
        fun consumeContinue(): Boolean {
            val ok = SystemClock.elapsedRealtime() < continueUntil
            continueUntil = 0L
            return ok
        }
    }

    private val appContext = context.applicationContext
    private val itemId = UUID.randomUUID().toString()

    private var controller: MediaController? = null
    private var pending: MediaItem? = null
    private var released = false
    private var applied = false

    private var startFraction = 0f
    private var startPending = false
    private var autoPlay = false
    private var autoPlayPending = false

    /** Пользователь нажимал «играть», двигал ползунок или шаг запущен автоматически. */
    var touched = false
        private set

    /** Запись дослушана до конца. */
    var finished = false
        private set

    /** Вызывается один раз, когда запись закончилась. */
    var onEnded: () -> Unit = {}

    var state by mutableStateOf(AudioUiState())
        private set

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // События старой записи общего плеера этого шага не касаются
            val mine = applied && player.currentMediaItem?.mediaId == itemId
            if (mine) {
                val ready = player.playbackState == Player.STATE_READY
                if (startPending && ready && player.duration > 0) {
                    startPending = false
                    if (startFraction > 0f) player.seekTo((player.duration * startFraction).toLong())
                }
                if (autoPlayPending && ready) {
                    autoPlayPending = false
                    if (!player.isPlaying) player.play()
                }
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) &&
                    player.playbackState == Player.STATE_ENDED && !finished
                ) {
                    finished = true
                    markContinue()
                    player.pause()
                    onEnded()
                }
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
        applied = true
        c.setMediaItem(item)
        c.prepare()
        if (autoPlay) {
            autoPlayPending = true
            c.play()
        }
    }

    /**
     * startFraction: с какого места продолжить (0..1) внутри отрезка.
     * autoPlay: сразу начать играть, без нажатия ▶.
     * clipStartMs / clipEndMs: отрезок общей записи.
     */
    fun load(
        uri: Uri,
        title: String,
        startFraction: Float = 0f,
        autoPlay: Boolean = false,
        clipStartMs: Long = 0L,
        clipEndMs: Long? = null,
    ) {
        this.autoPlay = autoPlay
        if (autoPlay) touched = true
        finished = false
        this.startFraction = startFraction.coerceIn(0f, 0.99f)
        startPending = this.startFraction > 0f

        val builder = MediaItem.Builder()
            .setMediaId(itemId)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist("Последование ко Причащению")
                    .build()
            )
        if (clipStartMs > 0L || clipEndMs != null) {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clipStartMs)
                    .setEndPositionMs(clipEndMs ?: C.TIME_END_OF_SOURCE)
                    .build()
            )
        }
        val item = builder.build()
        val c = controller
        if (c != null) applyItem(c, item) else pending = item
        refresh()
    }

    fun toggle() {
        val c = controller ?: return
        touched = true
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(ms: Long) {
        touched = true
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