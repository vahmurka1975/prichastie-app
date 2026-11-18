package ru.example.prichastie

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.example.prichastie.model.Catalog
import ru.example.prichastie.model.Phase
import ru.example.prichastie.ui.AppBar
import ru.example.prichastie.ui.AppButton
import ru.example.prichastie.ui.AppCard
import ru.example.prichastie.ui.Note
import ru.example.prichastie.ui.PBar
import ru.example.prichastie.ui.Ring
import ru.example.prichastie.ui.Sans
import ru.example.prichastie.ui.Serif
import ru.example.prichastie.ui.StepRow
import ru.example.prichastie.ui.theme.palette

@Composable
fun SummaryScreen(
    ui: UiState,
    scroll: ScrollState,
    onOpenStep: (String) -> Unit,
    onRestartAll: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = palette
    val before = ui.phaseSteps(Phase.BEFORE)
    val after = ui.phaseSteps(Phase.AFTER)
    val all = before + after
    val comm = ui.communionDone
    val numbers = ui.phaseNumbers()
    val allPct = all.percent()
    val fin = all.isNotEmpty() && all.all { it.done } && comm
    val doneSec = all.filter { it.done }.sumOf { it.step.audioSeconds }
    val left = before.filter { !it.done } + (if (comm) after.filter { !it.done } else emptyList())

    Column(modifier.fillMaxSize()) {
        AppBar("Итоги", "Что пройдено за подготовку и благодарение")
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard(borderColor = if (fin) p.ok else null) {
                Text(if (fin) "🕊" else "✦", fontSize = 34.sp, modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
                Text(
                    if (fin) "Чин совершён. Слава Богу за всё!"
                    else if (allPct > 0) "Пройдено $allPct% чина" else "Вы ещё не начинали",
                    color = p.ink, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
                Note(
                    if (fin) "Пусть Причастие будет во исцеление души и тела."
                    else "Незавершённые шаги перечислены ниже. Их можно пройти позже или пропустить: решение за вами.",
                    Modifier.fillMaxWidth(), TextAlign.Center
                )
            }

            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Ring(before.percent(), 84.dp, p.gold)
                        Note("До Причастия\n${before.count { it.done }} из ${before.size}", align = TextAlign.Center)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Ring(after.percent(), 84.dp, p.ok)
                        Note("После Причастия\n${after.count { it.done }} из ${after.size}", align = TextAlign.Center)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Молитвенное время", "≈ " + fmtMin(doneSec))
                Stat("Шагов пройдено", "${all.count { it.done }} из ${all.size}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Причастие", if (comm) "отмечено ✓" else "не отмечено")
                Stat("Режим", if (ui.fullMode) "С тремя канонами" else "Без канонов")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("📖 Прочитано шагов", "${all.mix().read}")
                Stat("🎧 Прослушано шагов", "${all.mix().heard}")
            }

            AppCard {
                Text("По разделам", color = p.ink, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Catalog.groups.filter { it.phase != Phase.COMMUNION }.forEach { g ->
                    val list = ui.steps.filter { it.included && it.step.groupId == g.id && it.step.audioSeconds > 0 }
                    if (list.isNotEmpty()) {
                        val pct = list.percent()
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(g.title, color = p.ink, fontFamily = Sans, fontSize = 12.sp,
                                modifier = Modifier.weight(1f))
                            Text("${list.count { it.done }}/${list.size} · $pct%", color = p.mut,
                                fontFamily = Sans, fontSize = 12.sp)
                        }
                        PBar(pct / 100f, ok = pct == 100, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }

            if (left.isNotEmpty()) {
                AppCard {
                    Text("Не пройдено (${left.size})", color = p.ink, fontFamily = Sans,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    left.forEach { s ->
                        StepRow(
                            number = numbers[s.step.id] ?: 0,
                            title = s.step.title,
                            sub = (if (s.step.phase == Phase.BEFORE) "До Причастия" else "После Причастия") +
                            (if (s.frac > 0f) " · начато, ${s.pct()}%" else ""),
                            done = false, locked = false, trailing = "", fraction = s.frac,
                            onClick = { onOpenStep(s.step.id) }
                        )
                    }
                }
            }

            AppButton("↺ Начать всё сначала", onRestartAll)
            AppButton("На главную", onGoHome, outlined = true)
        }
    }
}

@Composable
private fun RowScope.Stat(label: String, value: String) {
    val p = palette
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .weight(1f)
            .background(p.card, shape)
            .border(1.dp, p.line, shape)
            .padding(10.dp)
    ) {
        Text(label, color = p.mut, fontFamily = Sans, fontSize = 12.sp)
        Text(value, color = p.ink, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            modifier = Modifier.padding(top = 2.dp))
    }
}