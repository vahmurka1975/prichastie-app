package ru.example.prichastie

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.example.prichastie.model.Catalog
import ru.example.prichastie.model.Phase
import ru.example.prichastie.model.Step

data class StepUi(
    val step: Step,
    val number: Int?,        // null, если шаг не входит в выбранный режим
    val included: Boolean,
    val done: Boolean
)

data class UiState(
    val fullMode: Boolean = true,
    val started: Boolean = false,
    val steps: List<StepUi> = emptyList(),
    val progress: Float = 0f,
    val progressBefore: Float = 0f,
    val progressAfter: Float = 0f
) {
    /** Чекбокс «Полное прохождение» блокируется, когда подготовка началась. */
    val modeLocked: Boolean get() = started
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProgressRepository(app)

    /** Группы, которые можно не читать (сейчас это «Три канона»). */
    private val optionalGroupIds: Set<String> =
        Catalog.groups.filter { it.optional }.map { it.id }.toSet()

    val ui: StateFlow<UiState> = repo.state
        .map { buildUi(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun buildUi(s: SavedState): UiState {
        var counter = 0
        val steps = Catalog.steps.map { st ->
            val included = s.fullMode || st.groupId !in optionalGroupIds
            StepUi(
                step = st,
                number = if (included) ++counter else null,
                included = included,
                done = included && st.id in s.doneIds
            )
        }
        return UiState(
            fullMode = s.fullMode,
            started = s.started,
            steps = steps,
            progress = weighted(steps),
            progressBefore = weighted(steps.filter { it.step.phase == Phase.BEFORE }),
            progressAfter = weighted(steps.filter { it.step.phase == Phase.AFTER })
        )
    }

    private fun weighted(list: List<StepUi>): Float {
        val inc = list.filter { it.included }
        val total = inc.sumOf { it.step.audioSeconds.toLong() }
        if (total == 0L) return 0f
        val done = inc.filter { it.done }.sumOf { it.step.audioSeconds.toLong() }
        return done.toFloat() / total
    }

    fun setFullMode(value: Boolean) = viewModelScope.launch {
        if (!ui.value.started) repo.setFullMode(value)   // после старта режим менять нельзя
    }

    fun start() = viewModelScope.launch { repo.markStarted() }

    /** Для шагов с текстом, а также для «Готово» у Причастия. */
    fun setDone(id: String, done: Boolean) = viewModelScope.launch { repo.setDone(id, done) }

    /** «Этап сначала»: отметка о Причастии (фаза COMMUNION) не затрагивается. */
    fun restartPhase(phase: Phase) = viewModelScope.launch {
        val ids = Catalog.steps.filter { it.phase == phase }.map { it.id }.toSet()
        repo.clearDone(ids)
    }

    fun restart() = viewModelScope.launch { repo.reset() }
}