package ru.example.prichastie

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ru.example.prichastie.ui.GroupHeader
import ru.example.prichastie.ui.LinkText
import ru.example.prichastie.ui.Note
import ru.example.prichastie.ui.PBar
import ru.example.prichastie.ui.PhaseHeader
import ru.example.prichastie.ui.Sans
import ru.example.prichastie.ui.Serif
import ru.example.prichastie.ui.StepRow
import ru.example.prichastie.ui.theme.palette

@Composable
fun StepListScreen(
    ui: UiState,
    listState: LazyListState,
    onOpenStep: (String) -> Unit,
    onRestartPhase: (Phase) -> Unit,
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = palette
    val before = ui.phaseSteps(Phase.BEFORE)
    val after = ui.phaseSteps(Phase.AFTER)
    val comm = ui.communionDone
    val numbers = ui.phaseNumbers()
    val bPct = before.percent()
    val aPct = after.percent()

    Column(modifier.fillMaxSize()) {
        AppBar("Все шаги", "До Причастия $bPct% · После $aPct%")
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp)
        ) {
            item(key = "mode") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Note(
                        "Режим: " + (if (ui.fullMode) "полное прохождение (с тремя канонами)"
                        else "без канонов №1–3") + (if (ui.started) " · 🔒" else " ·")
                    )
                    if (!ui.started) LinkText("изменить", onGoSettings)
                }
            }
            item(key = "hdr_before") {
                PhaseHeader("До Причастия · $bPct% · ${before.count { it.done }}/${before.size}") {
                    if (bPct > 0) LinkText("↺ сначала") { onRestartPhase(Phase.BEFORE) }
                }
            }

            Catalog.groups.forEach { g ->
                if (g.phase == Phase.COMMUNION) {
                    item(key = "comm") {
                        val shape = RoundedCornerShape(14.dp)
                        val cs = ui.steps.firstOrNull { it.step.id == "communion" }
                        Column(
                            Modifier
                                .padding(vertical = 14.dp)
                                .fillMaxWidth()
                                .background(p.hl, shape)
                                .border(2.dp, p.gold, shape)
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Причащение Святых Таин", color = p.ink, fontFamily = Serif,
                                fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(cs?.step?.subtitle ?: "", color = p.mut, fontFamily = Sans, fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                            AppButton(
                                if (comm) "Причастие отмечено ✓" else "Перейти к отметке",
                                { onOpenStep("communion") }, outlined = comm
                            )
                        }
                    }
                    item(key = "hdr_after") {
                        PhaseHeader(
                            "После Причастия · " +
                                if (comm) "$aPct% · ${after.count { it.done }}/${after.size}"
                                else "закрыто до отметки"
                        ) {
                            if (comm && aPct > 0) LinkText("↺ сначала") { onRestartPhase(Phase.AFTER) }
                        }
                    }
                } else {
                    val active = ui.steps.filter { it.included && it.step.groupId == g.id }
                    if (active.isEmpty()) {
                        item(key = "off_${g.id}") {
                            GroupHeader(g.title, "не входят")
                            Note(
                                "Режим «без канонов»: каноны №1–3 не входят в прогресс. " +
                                    "Включить их можно в настройках, пока подготовка не начата.",
                                Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        val counted = active.filter { it.step.audioSeconds > 0 }
                        val gPct = counted.percent()
                        item(key = "gh_${g.id}") {
                            GroupHeader(g.title, "$gPct% · ≈ ${fmtMin(counted.sumOf { it.step.audioSeconds })}")
                            Note(g.subtitle, Modifier.padding(horizontal = 2.dp))
                            Spacer(Modifier.height(6.dp))
                            PBar(gPct / 100f, ok = gPct == 100, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        items(active, key = { it.step.id }) { s ->
                            val locked = s.step.phase == Phase.AFTER && !comm
                            StepRow(
                                number = numbers[s.step.id] ?: 0,
                                title = s.step.title,
                                sub = s.step.subtitle,
                                done = s.done,
                                locked = locked,
                                trailing = s.rowTrailing(),
                                fraction = s.frac,
                                onClick = { onOpenStep(s.step.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}