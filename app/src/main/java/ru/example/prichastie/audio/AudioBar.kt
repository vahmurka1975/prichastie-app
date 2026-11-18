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

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f)

/**
 * Запись шага из res/raw: «<textKey>_mod» или «<textKey>_csl» (точки и дефисы в ключе
 * заменяются на «_»). Если файла нет, берётся заглушка stub_mod / stub_csl.
 */
fun rawAudioId(context: Context, textKey: String, language: Language): Int {
    val suffix = if (language == Language.MODERN) "mod" else "csl"
    val base = textKey.lowercase().replace(Regex("[^a-z0-9_]"), "_")
    val res = context.resources
    val own = res.getIdentifier("${base}_$suffix", "raw", context.packageName)
    return if (own != 0) own else res.getIdentifier("stub_$suffix", "raw", context.packageName)
}

/**
 * Режим «Слушать»: карточка с временем, ссылка «Следить по тексту» и плеер внизу.
 * onProgress вызывается раз в секунду (доля 0..0.99), когда запись трогали.
 * onFinished вызывается, когда запись дослушана до конца.
 */
@Composable
fun ColumnScope.AudioMode(
    textKey: String,
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
    val resId = remember(textKey, language) { rawAudioId(context, textKey, language) }
    if (resId == 0) {
        Column(Modifier.weight(1f).padding(14.dp)) {
            AppCard { Note("Для этого шага нет записи. Положите файл в res/raw (например, ${textKey}_mod).") }
        }
        return
    }

    val progressCb by rememberUpdatedState(onProgress)
    val finishedCb by rememberUpdatedState(onFinished)

    val controller = remember(textKey, language) {
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

    LaunchedEffect(resId, title) {
        controller.load(
            Uri.parse("android.resource://${context.packageName}/$resId"), title, startFraction
        )
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

    Column(Modifier.fillMaxWidth().background(p.card)) {
        HLine()
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stepLabel, color = p.mut, fontFamily = Sans, fontSize = 11.sp)
                Text("×${s.speed}", color = p.mut, fontFamily = Sans, fontSize = 11.sp)
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
                KeyButton("×${s.speed}", {
                    val next = SPEEDS[(SPEEDS.indexOf(s.speed).coerceAtLeast(0) + 1) % SPEEDS.size]
                    controller.setSpeed(next)
                })
            }
        }
    }
}

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
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = p.ink, fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}