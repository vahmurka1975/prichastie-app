package ru.example.prichastie

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.example.prichastie.model.Language
import ru.example.prichastie.model.ReadingMode
import ru.example.prichastie.model.StepProgress

private val Context.dataStore by preferencesDataStore(name = "progress")

data class SavedState(
    val fullMode: Boolean = true,
    val started: Boolean = false,
    val doneIds: Set<String> = emptySet(),
    val progress: Map<String, StepProgress> = emptyMap(),
    val language: Language = Language.MODERN,
    val defaultMode: ReadingMode = ReadingMode.TEXT
)

class ProgressRepository(private val context: Context) {

    private object Keys {
        val FULL = booleanPreferencesKey("full_mode")
        val STARTED = booleanPreferencesKey("started")
        val DONE = stringSetPreferencesKey("done_ids")
        val PROGRESS = stringPreferencesKey("step_progress")
        val LANG = stringPreferencesKey("language")
        val DMODE = stringPreferencesKey("default_mode")
    }

    /** Шаг считается начатым, когда у него появился прогресс. */
    private companion object {
        const val START_THRESHOLD = 0.02f
    }

    val state: Flow<SavedState> = context.dataStore.data.map { p ->
        val done = p[Keys.DONE] ?: emptySet()
        val all = HashMap<String, StepProgress>()
        decode(p[Keys.PROGRESS]).forEach { (id, sp) -> all[id] = sp.copy(done = id in done) }
        done.forEach { id -> if (id !in all) all[id] = StepProgress(id, done = true) }
        SavedState(
            fullMode = p[Keys.FULL] ?: true,
            started = p[Keys.STARTED] ?: false,
            doneIds = done,
            progress = all,
            language = if (p[Keys.LANG] == "c") Language.CHURCH_SLAVONIC else Language.MODERN,
            defaultMode = if (p[Keys.DMODE] == "A") ReadingMode.AUDIO else ReadingMode.TEXT
        )
    }

    suspend fun setFullMode(value: Boolean) {
        context.dataStore.edit { it[Keys.FULL] = value }
    }

    suspend fun markStarted() {
        context.dataStore.edit { it[Keys.STARTED] = true }
    }

    suspend fun setLanguage(value: Language) {
        context.dataStore.edit { it[Keys.LANG] = if (value == Language.CHURCH_SLAVONIC) "c" else "m" }
    }

    suspend fun setDefaultMode(value: ReadingMode) {
        context.dataStore.edit { it[Keys.DMODE] = if (value == ReadingMode.AUDIO) "A" else "T" }
    }

    /** Прокрутка текста: запоминаем максимум. Игнорируем, если шаг идёт в режиме аудио или уже пройден. */
    suspend fun recordRead(id: String, fraction: Float) {
        context.dataStore.edit { p ->
            if (id in (p[Keys.DONE] ?: emptySet())) return@edit
            val map = decode(p[Keys.PROGRESS]).toMutableMap()
            val cur = map[id] ?: StepProgress(id)
            if (cur.mode == ReadingMode.AUDIO) return@edit
            val f = fraction.coerceIn(0f, 1f)
            if (cur.mode == null && f < START_THRESHOLD) return@edit
            if (cur.mode == ReadingMode.TEXT && f <= cur.readFraction) return@edit
            map[id] = cur.copy(mode = ReadingMode.TEXT, readFraction = f)
            p[Keys.PROGRESS] = encode(map)
            p[Keys.STARTED] = true
        }
    }

    /** Позиция записи: хранится текущая (перемотка назад разрешена). */
    suspend fun recordAudio(id: String, fraction: Float, language: Language) {
        context.dataStore.edit { p ->
            if (id in (p[Keys.DONE] ?: emptySet())) return@edit
            val map = decode(p[Keys.PROGRESS]).toMutableMap()
            val cur = map[id] ?: StepProgress(id)
            if (cur.mode == ReadingMode.TEXT) return@edit
            map[id] = cur.copy(
                mode = ReadingMode.AUDIO,
                language = cur.language ?: language,
                audioFraction = fraction.coerceIn(0f, 0.99f)
            )
            p[Keys.PROGRESS] = encode(map)
            p[Keys.STARTED] = true
        }
    }

    /** Шаг пройден. mode = null: отметка без чтения (Причастие, «Приидите, ядите»). */
    suspend fun finish(id: String, mode: ReadingMode?, language: Language?) {
        context.dataStore.edit { p ->
            val map = decode(p[Keys.PROGRESS]).toMutableMap()
            val cur = map[id] ?: StepProgress(id)
            val m = mode ?: cur.mode
            map[id] = cur.copy(
                mode = m,
                language = cur.language ?: language,
                readFraction = if (m == ReadingMode.TEXT) 1f else cur.readFraction,
                audioFraction = if (m == ReadingMode.AUDIO) 1f else cur.audioFraction
            )
            p[Keys.PROGRESS] = encode(map)
            p[Keys.DONE] = (p[Keys.DONE] ?: emptySet()) + id
            p[Keys.STARTED] = true
        }
    }

    suspend fun setDone(id: String, done: Boolean) {
        if (done) finish(id, null, null) else resetStep(id)
    }

    /** Один шаг начинается сначала. */
    suspend fun resetStep(id: String) = clearDone(setOf(id))

    /** «Этап сначала»: снимает отметки и прогресс с перечисленных шагов. */
    suspend fun clearDone(ids: Set<String>) {
        context.dataStore.edit { p ->
            val map = decode(p[Keys.PROGRESS]).toMutableMap()
            ids.forEach { map.remove(it) }
            val left = (p[Keys.DONE] ?: emptySet()) - ids
            p[Keys.DONE] = left
            p[Keys.PROGRESS] = encode(map)
            p[Keys.STARTED] = left.isNotEmpty() || map.isNotEmpty()
        }
    }

    /** «Начать всё сначала»: сбрасывает отметки и прогресс, снимает блокировку режима. */
    suspend fun reset() {
        context.dataStore.edit {
            it[Keys.STARTED] = false
            it[Keys.DONE] = emptySet()
            it[Keys.PROGRESS] = ""
        }
    }

    // Запись: id|T или A или пусто|m или c или пусто|чтение|аудио, по строке на шаг
    private fun encode(m: Map<String, StepProgress>): String =
        m.values.joinToString("\n") { sp ->
            listOf(
                sp.stepId,
                when (sp.mode) { ReadingMode.TEXT -> "T"; ReadingMode.AUDIO -> "A"; null -> "" },
                when (sp.language) {
                    Language.MODERN -> "m"; Language.CHURCH_SLAVONIC -> "c"; null -> ""
                },
                sp.readFraction.toString(),
                sp.audioFraction.toString()
            ).joinToString("|")
        }

    private fun decode(s: String?): Map<String, StepProgress> {
        if (s.isNullOrBlank()) return emptyMap()
        val out = LinkedHashMap<String, StepProgress>()
        s.split("\n").forEach { line ->
            val a = line.split("|")
            if (a.size >= 5 && a[0].isNotEmpty()) {
                out[a[0]] = StepProgress(
                    stepId = a[0],
                    mode = when (a[1]) { "T" -> ReadingMode.TEXT; "A" -> ReadingMode.AUDIO; else -> null },
                    language = when (a[2]) {
                        "m" -> Language.MODERN; "c" -> Language.CHURCH_SLAVONIC; else -> null
                    },
                    readFraction = a[3].toFloatOrNull() ?: 0f,
                    audioFraction = a[4].toFloatOrNull() ?: 0f
                )
            }
        }
        return out
    }
}