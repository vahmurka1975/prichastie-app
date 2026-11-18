package ru.example.prichastie

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.example.prichastie.model.Phase
import ru.example.prichastie.ui.AppBar
import ru.example.prichastie.ui.AppButton
import ru.example.prichastie.ui.AppCard
import ru.example.prichastie.ui.Note
import ru.example.prichastie.ui.PBar
import ru.example.prichastie.ui.PhaseTag
import ru.example.prichastie.ui.Ring
import ru.example.prichastie.ui.Sans
import ru.example.prichastie.ui.Serif
import ru.example.prichastie.ui.StepRow
import ru.example.prichastie.ui.theme.palette

@Composable
fun HomeScreen(
    ui: UiState,
    scroll: ScrollState,
    onOpenStep: (String) -> Unit,
    onSetFullMode: (Boolean) -> Unit,
    onCommunion: (Boolean) -> Unit,
    onRestartPhase: (Phase) -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = palette
    val before = ui.phaseSteps(Phase.BEFORE)
    val after = ui.phaseSteps(Phase.AFTER)
    val comm = ui.communionDone
    val numbers = ui.phaseNumbers()
    val bPct = before.percent()
    val aPct = after.percent()
    val nb = before.firstOrNull { !it.done }
    val na = after.firstOrNull { !it.done }
    val all = before + after
    val allPct = all.percent()
    val small = 13.sp

    Column(modifier.fillMaxSize()) {
        AppBar("Последование ко Причащению", "Подготовка и благодарение")
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!ui.started) {
                AppCard(borderColor = p.gold) {
                    Note("⚠ Перед началом подготовки выберите режим. После первого шага изменить его нельзя.")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { onSetFullMode(!ui.fullMode) },
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(checked = ui.fullMode, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Полное прохождение", color = p.ink, fontFamily = Sans,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Note("Три канона (№1–3) входят в прогресс. Без галочки каноны в прогресс не входят.")
                        }
                    }
                }
            }

            // Этап 1
            AppCard {
                PhaseTag("Этап 1 · До Причастия")
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(bPct, 92.dp, p.gold)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Шагов: ${before.count { it.done }} из ${before.size}\n" +
                            "Осталось ≈ ${fmtMin(before.remainingSec())}",
                        color = p.mut, fontFamily = Sans, fontSize = small, lineHeight = 19.5.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (nb != null) {
                    AppButton(
                        (if (bPct > 0) "Продолжить" else "Начать") + ": " + nb.step.title,
                        { onOpenStep(nb.step.id) }
                    )
                } else {
                    Text("Подготовка завершена", color = p.ok, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                }
                if (bPct > 0) {
                    Spacer(Modifier.height(8.dp))
                    AppButton("↺ Этап сначала", { onRestartPhase(Phase.BEFORE) }, outlined = true)
                }
            }

            // Причащение
            val cShape = RoundedCornerShape(14.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(p.hl, cShape)
                    .border(2.dp, p.gold, cShape)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Причащение Святых Таин", color = p.ink, fontFamily = Serif,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("«Тело Христово приимите…»", color = p.mut, fontFamily = Sans, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                if (comm) {
                    AppButton("Причастие отмечено ✓ (отменить)", { onCommunion(false) }, outlined = true)
                } else {
                    AppButton("Готово: причастился(ась)", { onCommunion(true) })
                }
            }

            // Этап 2
            AppCard(modifier = Modifier.alpha(if (comm) 1f else 0.45f)) {
                PhaseTag("Этап 2 · После Причастия", after = true)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(aPct, 92.dp, p.ok)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        if (comm) {
                            "Шагов: ${after.count { it.done }} из ${after.size}\n" +
                                "Осталось ≈ ${fmtMin(after.remainingSec())}"
                        } else {
                            "Благодарственные молитвы\nоткроются после отметки\nо Причастии"
                        },
                        color = p.mut, fontFamily = Sans, fontSize = small, lineHeight = 19.5.sp
                    )
                }
                if (comm && na != null) {
                    Spacer(Modifier.height(12.dp))
                    AppButton(
                        (if (aPct > 0) "Продолжить" else "Начать") + ": " + na.step.title,
                        { onOpenStep(na.step.id) }
                    )
                } else if (comm) {
                    Text("Благодарение завершено. Слава Богу за всё!", color = p.ok, fontFamily = Sans,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                }
                if (comm && aPct > 0) {
                    Spacer(Modifier.height(8.dp))
                    AppButton("↺ Этап сначала", { onRestartPhase(Phase.AFTER) }, outlined = true)
                }
            }

            // Весь чин
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Весь чин", color = p.ink, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = small)
                    Text("${all.count { it.done }} из ${all.size} · $allPct%", color = p.mut,
                        fontFamily = Sans, fontSize = small)
                }
                PBar(allPct / 100f, ok = allPct == 100, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                PhaseChecks("До Причастия", before, bPct, numbers, false, onOpenStep)
                PhaseChecks("После Причастия", after, aPct, numbers, !comm, onOpenStep)
            }

            if (allPct > 0) AppButton("✦ Итоги", onOpenSummary, outlined = true)

            Note(
                "Один шаг — один псалом, канон или молитва. Прогресс этапа взвешен по времени " +
                    "звучания шагов. Само Причащение отмечается кнопкой «Готово»."
            )
        }
    }
}

@Composable
private fun PhaseChecks(
    title: String,
    steps: List<StepUi>,
    pct: Int,
    numbers: Map<String, Int>,
    locked: Boolean,
    onOpenStep: (String) -> Unit
) {
    val p = palette
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { open = !open }.padding(top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (open) "▾ " else "▸ ", color = p.mut, fontFamily = Sans, fontSize = 13.sp)
        Text(title, color = p.ink, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(" · ${steps.count { it.done }}/${steps.size} · $pct%", color = p.ink,
            fontFamily = Sans, fontSize = 13.sp)
    }
    if (open) {
        steps.forEach { s ->
            StepRow(
                number = numbers[s.step.id] ?: 0,
                title = s.step.title,
                sub = "",
                done = s.done,
                locked = locked,
                trailing = if (s.done) "100%" else "",
                onClick = { onOpenStep(s.step.id) },
                compact = true
            )
        }
    }
}