package ru.example.prichastie.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "prichastie")

data class UserState(
    /** Идентификаторы шагов, отмеченных выполненными. */
    val done: Set<String> = emptySet(),
    val nightMode: Boolean = false,
    /** Читать ли три канона (необязательная группа). */
    val canonsEnabled: Boolean = true
)

class ProgressRepository(private val context: Context) {

    private object Keys {
        val DONE = stringSetPreferencesKey("done")
        val NIGHT = booleanPreferencesKey("night")
        val CANONS = booleanPreferencesKey("canons")
    }

    val state: Flow<UserState> = context.store.data.map { p: Preferences ->
        UserState(
            done = p[Keys.DONE] ?: emptySet(),
            nightMode = p[Keys.NIGHT] ?: false,
            canonsEnabled = p[Keys.CANONS] ?: true
        )
    }

    suspend fun setDone(stepId: String, done: Boolean) {
        context.store.edit { p ->
            val current = p[Keys.DONE] ?: emptySet()
            p[Keys.DONE] = if (done) current + stepId else current - stepId
        }
    }

    suspend fun setNightMode(on: Boolean) {
        context.store.edit { it[Keys.NIGHT] = on }
    }

    suspend fun setCanonsEnabled(on: Boolean) {
        context.store.edit { it[Keys.CANONS] = on }
    }

    suspend fun resetProgress() {
        context.store.edit { it.remove(Keys.DONE) }
    }
}