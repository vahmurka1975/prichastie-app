package ru.example.prichastie.audio

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f)

@Composable
fun AudioBar(stepId: String, modifier: Modifier = Modifier, title: String = "") {
    val context = LocalContext.current
    val resName = stepId.replace('-', '_').lowercase()
    val resId = remember(resName) {
        context.resources.getIdentifier(resName, "raw", context.packageName)
    }
    if (resId == 0) return // для этого шага аудио нет

    val controller = remember { StepAudioController(context) }
    DisposableEffect(Unit) { onDispose { controller.release() } }

    LaunchedEffect(resId, title) {
        controller.load(Uri.parse("android.resource://${context.packageName}/$resId"), title)
    }
    // Положение ползунка обновляем только пока идёт звук
    LaunchedEffect(controller) {
        while (true) {
            if (controller.state.isPlaying) controller.refresh()
            delay(250)
        }
    }

    // Android 13+: без этого разрешения звук играет, но плашки в шторке не будет
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val s = controller.state
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = s.durationMs.coerceAtLeast(1L).toFloat()
    val shown = if (dragging) dragValue else s.positionMs.toFloat().coerceIn(0f, duration)

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Slider(
            value = shown,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                controller.seekTo(dragValue.toLong())
                dragging = false
            },
            valueRange = 0f..duration,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shown.toLong()), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(s.durationMs), style = MaterialTheme.typography.labelMedium)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { controller.skip(-10_000) }) { Text("−10 с") }
            Button(onClick = {
                if (!s.isPlaying &&
                    Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                controller.toggle()
            }) {
                Text(if (s.isPlaying) "Пауза" else "Слушать")
            }
            TextButton(onClick = { controller.skip(10_000) }) { Text("+10 с") }
            OutlinedButton(onClick = {
                val next = SPEEDS[(SPEEDS.indexOf(s.speed).coerceAtLeast(0) + 1) % SPEEDS.size]
                controller.setSpeed(next)
            }) { Text("${s.speed}×") }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}