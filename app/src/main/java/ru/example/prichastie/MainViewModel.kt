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
import ru.example.prichastie.model.Language
import ru.example.prichastie.model.Phase
import ru.example.prichastie.model.ReadingMode
import ru.example.prichastie.model.Step
import ru.example.prichastie.model.StepProgress

data class StepUi(
    val step: Step,
    val number: Int?,        // null, если шаг не входит в выбранный режим
    val included: Boolean,
    val done: Boolean,
    val progress: StepProgress = StepProgress(step.id)
)

data class UiState(
    val fullMode: Boolean = true,
    val started: Boolean = false,
    val steps: List<StepUi> = emptyList(),
    val progress: Float = 0f,
    val progressBefore: Float = 0f,
    val progressAfter: Float = 0f,
    val language: Language = Language.MODERN,
    val defaultMode: ReadingMode = ReadingMode.TEXT
) {
    /** Чекбокс «Полное прохождение» блокируется, когда подготовка началась. */
    val modeLocked: Boolean get() = started
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProgressRepository(app)
	private var autoPlayFor: String? = null

fun requestAutoPlay(id: String) { autoPlayFor = id }

fun takeAutoPlay(id: String): Boolean {
    val r = autoPlayFor == id
    if (r) autoPlayFor = null
    return r
}

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
            val done = included && st.id in s.doneIds
            val pr = (s.progress[st.id] ?: StepProgress(st.id)).copy(done = done)
            StepUi(
                step = st,
                number = if (included) ++counter else null,
                included = included,
                done = done,
                progress = pr
            )
        }
        return UiState(
            fullMode = s.fullMode,
            started = s.started,
            steps = steps,
            progress = weighted(steps),
            progressBefore = weighted(steps.filter { it.step.phase == Phase.BEFORE }),
            progressAfter = weighted(steps.filter { it.step.phase == Phase.AFTER }),
            language = s.language,
            defaultMode = s.defaultMode
        )
    }

    private fun weighted(list: List<StepUi>): Float {
        val inc = list.filter { it.included }
        val total = inc.sumOf { it.step.audioSeconds.toLong() }
        if (total == 0L) return 0f
        val done = inc.sumOf { it.step.audioSeconds * it.frac.toDouble() }
        return (done / total).toFloat()
    }

    fun setFullMode(value: Boolean) = viewModelScope.launch {
        if (!ui.value.started) repo.setFullMode(value)   // после старта режим менять нельзя
    }

    fun start() = viewModelScope.launch { repo.markStarted() }

    fun setLanguage(value: Language) = viewModelScope.launch { repo.setLanguage(value) }

    fun setDefaultMode(value: ReadingMode) = viewModelScope.launch { repo.setDefaultMode(value) }

    fun recordRead(id: String, fraction: Float) =
        viewModelScope.launch { repo.recordRead(id, fraction) }

    fun recordAudio(id: String, fraction: Float, language: Language) =
        viewModelScope.launch { repo.recordAudio(id, fraction, language) }

    /** Шаг пройден: mode = TEXT / AUDIO, а для отметок без чтения null. */
    fun finish(id: String, mode: ReadingMode?, language: Language?) =
        viewModelScope.launch { repo.finish(id, mode, language) }

    /** Для Причастия и шагов-отметок. */
    fun setDone(id: String, done: Boolean) = viewModelScope.launch { repo.setDone(id, done) }

    fun resetStep(id: String) = viewModelScope.launch { repo.resetStep(id) }

    /** «Этап сначала»: отметка о Причастии (фаза COMMUNION) не затрагивается. */
    fun restartPhase(phase: Phase) = viewModelScope.launch {
        val ids = Catalog.steps.filter { it.phase == phase }.map { it.id }.toSet()
        repo.clearDone(ids)
    }

    fun restart() = viewModelScope.launch { repo.reset() }
}