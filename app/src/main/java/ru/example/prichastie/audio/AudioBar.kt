package ru.example.prichastie.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import ru.example.prichastie.model.Language
import ru.example.prichastie.model.ProgressMath
import ru.example.prichastie.ui.AppCard
import ru.example.prichastie.ui.HLine
import ru.example.prichastie.ui.LinkText
import ru.example.prichastie.ui.Note
import ru.example.prichastie.ui.Sans
import ru.example.prichastie.ui.theme.palette
import kotlin.math.abs
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import java.io.IOException
import ru.example.prichastie.Timings

/** Порядок как в макете: 1 → 1.25 → 1.5 → 0.75 → 1. */
private val SPEEDS = listOf(1f, 1.25f, 1.5f, 0.75f)

private fun fmtSpeed(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()

/**
 * Запись из assets/audio/<audioKey>.<mod|csl>.mp3, например audio/b04.mod.mp3.
 * Если файла нет, берётся заглушка res/raw/stub_mod или stub_csl. Если нет и её, возвращается null.
 */
fun audioUri(context: Context, audioKey: String?, language: Language): Uri? {
    val suffix = if (language == Language.MODERN) "mod" else "csl"
    if (audioKey != null) {
        val path = "audio/$audioKey.$suffix.mp3"
        val exists = try {
            context.assets.open(path).close(); true
        } catch (e: IOException) {
            false
        }
        if (exists) return Uri.parse("asset:///$path")
    }
    val stub = context.resources.getIdentifier("stub_$suffix", "raw", context.packageName)
    return if (stub != 0) Uri.parse("android.resource://${context.packageName}/$stub") else null
}

/**
 * Режим «Слушать»: карточка с временем, ссылка «Следить по тексту» и плеер внизу.
 * onProgress вызывается раз в секунду (доля 0..0.99), когда запись трогали.
 * onFinished вызывается, когда запись дослушана до конца.
 */
@Composable
fun ColumnScope.AudioMode(
    audioKey: String?,
	timingKey: String,
    shared: Boolean,
    title: String,
    language: Language,
    stepLabel: String,
    startFraction: Float,
    onProgress: (Float) -> Unit,
    onFinished: () -> Unit,
    followContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val p = palette
        val uri = remember(audioKey, language) { audioUri(context, audioKey, language) }
    if (uri == null) {
        Column(Modifier.weight(1f).padding(14.dp)) {
            AppCard { Note("Для этого шага нет записи. Положите файл в assets/audio (например, $audioKey.mod.mp3).") }
        }
        return
    }
    
	val langCode = if (language == Language.MODERN) "mod" else "csl"
    // Отрезок берём только у настоящего файла (не у заглушки)
    val clip = remember(audioKey, langCode, timingKey, uri) {
        if (audioKey != null && uri.scheme == "asset")
            Timings.segment(audioKey, langCode, timingKey)
        else null
    }
    // У шага общей записи без тайм-кода целиком играть нельзя: засчитается чужое
    if (shared && clip == null) {
        Column(Modifier.weight(1f).padding(14.dp)) {
            AppCard {
                Note("Для этого шага нет тайм-кода в timings.json ($audioKey / $langCode / $timingKey) или нет файла записи.")
            }
        }
        return
    }

    val progressCb by rememberUpdatedState(onProgress)
    val finishedCb by rememberUpdatedState(onFinished)

    val controller = remember(audioKey, language) {
        StepAudioController(context).also { it.onEnded = { finishedCb() } }
    }
    DisposableEffect(controller) {
        onDispose {
            val st = controller.state
            if (controller.touched && !controller.finished && st.durationMs > 0) {
                progressCb(ProgressMath.audioFraction(st.positionMs, st.durationMs).coerceAtMost(0.99f))
            }
            controller.release()
        }
    }

    LaunchedEffect(uri, title, clip) {
    controller.load(uri, title, startFraction, clip?.startMs ?: 0L, clip?.endMs)
    }
    // Положение ползунка обновляем только пока идёт звук
    LaunchedEffect(controller) {
        while (true) {
            if (controller.state.isPlaying) controller.refresh()
            delay(250)
        }
    }
    // Раз в секунду сообщаем прогресс, пока запись трогали
    LaunchedEffect(controller) {
        var sent = -1f
        while (true) {
            delay(1000)
            val st = controller.state
            if (controller.touched && !controller.finished && st.durationMs > 0) {
                val f = ProgressMath.audioFraction(st.positionMs, st.durationMs).coerceAtMost(0.99f)
                if (abs(f - sent) >= 0.003f || sent < 0f) {
                    sent = f
                    progressCb(f)
                }
            }
        }
    }

    // Android 13+: без этого разрешения звук играет, но плашки в шторке не будет
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val s = controller.state
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var follow by rememberSaveable { mutableStateOf(false) }
    val duration = s.durationMs.coerceAtLeast(1L).toFloat()
    val shown = if (dragging) dragValue else s.positionMs.toFloat().coerceIn(0f, duration)

    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppCard {
            Text("🎧", fontSize = 44.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Note(
                "Запись · " + if (language == Language.MODERN) "современный русский" else "церковнославянский",
                Modifier.fillMaxWidth(), TextAlign.Center
            )
            Text(
                "${formatTime(shown.toLong())} / ${formatTime(s.durationMs)}",
                color = p.ink, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            )
            Note("Чтобы шаг засчитался, запись нужно дослушать до конца", Modifier.fillMaxWidth(), TextAlign.Center)
        }
        LinkText(if (follow) "Скрыть текст" else "Следить по тексту") { follow = !follow }
        Note("Просмотр текста во время слушания не засчитывается как чтение.")
        if (follow) followContent()
    }

    Column(Modifier.fillMaxWidth().background(p.card).navigationBarsPadding()) {
        HLine()
        Column(
            Modifier.fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stepLabel, color = p.mut, fontFamily = Sans, fontSize = 11.sp)
                Text("×${fmtSpeed(s.speed)}", color = p.mut, fontFamily = Sans, fontSize = 11.sp)
            }
            Slider(
                value = shown,
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    controller.seekTo(dragValue.toLong())
                    dragging = false
                },
                valueRange = 0f..duration,
                colors = SliderDefaults.colors(
                    thumbColor = p.acc, activeTrackColor = p.acc, inactiveTrackColor = p.line
                )
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KeyButton("−15 с", { controller.skip(-15_000) })
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(p.acc)
                        .clickable {
                            if (!s.isPlaying && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            controller.toggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (s.isPlaying) "❚❚" else "▶", color = p.onAcc, fontSize = 20.sp)
                }
                KeyButton("+15 с", { controller.skip(15_000) })
                SpeedButton("×${fmtSpeed(s.speed)}") {
                    val next = SPEEDS[(SPEEDS.indexOf(s.speed).coerceAtLeast(0) + 1) % SPEEDS.size]
                    controller.setSpeed(next)
                }
            }
        }
    }
}

/** Кнопки «−15 с» / «+15 с» (.pc .sm): 44×40, радиус 20. */
@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    val p = palette
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .height(40.dp)
            .widthIn(min = 44.dp)
            .clip(shape)
            .border(1.dp, p.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = p.ink, fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

/** Кнопка скорости (.pc .sp): радиус 10, отступы 8×10. */
@Composable
private fun SpeedButton(label: String, onClick: () -> Unit) {
    val p = palette
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .clip(shape)
            .border(1.dp, p.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = p.ink, fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}