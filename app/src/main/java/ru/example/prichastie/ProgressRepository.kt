package ru.example.prichastie

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "progress")

data class SavedState(
    val fullMode: Boolean = true,
    val started: Boolean = false,
    val doneIds: Set<String> = emptySet()
)

class ProgressRepository(private val context: Context) {

    private object Keys {
        val FULL = booleanPreferencesKey("full_mode")
        val STARTED = booleanPreferencesKey("started")
        val DONE = stringSetPreferencesKey("done_ids")
    }

    val state: Flow<SavedState> = context.dataStore.data.map { p ->
        SavedState(
            fullMode = p[Keys.FULL] ?: true,
            started = p[Keys.STARTED] ?: false,
            doneIds = p[Keys.DONE] ?: emptySet()
        )
    }

    suspend fun setFullMode(value: Boolean) {
        context.dataStore.edit { it[Keys.FULL] = value }
    }

    suspend fun markStarted() {
        context.dataStore.edit { it[Keys.STARTED] = true }
    }

    suspend fun setDone(id: String, done: Boolean) {
        context.dataStore.edit { p ->
            val cur = p[Keys.DONE] ?: emptySet()
            p[Keys.DONE] = if (done) cur + id else cur - id
            p[Keys.STARTED] = true
        }
    }

    /** «Этап сначала»: снимает отметки с перечисленных шагов. Если ничего не осталось, режим снова можно менять. */
    suspend fun clearDone(ids: Set<String>) {
        context.dataStore.edit { p ->
            val left = (p[Keys.DONE] ?: emptySet()) - ids
            p[Keys.DONE] = left
            if (left.isEmpty()) p[Keys.STARTED] = false
        }
    }

    /** «Начать всё сначала»: сбрасывает отметки и снимает блокировку режима. */
    suspend fun reset() {
        context.dataStore.edit {
            it[Keys.STARTED] = false
            it[Keys.DONE] = emptySet()
        }
    }
}