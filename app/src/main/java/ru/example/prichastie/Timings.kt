package ru.example.prichastie

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.IOException

object Timings {

    /** Отрезок записи: начало и конец в мс. endMs == null — до конца файла. */
    data class Segment(val startMs: Long, val endMs: Long?)

    @Volatile
    private var data: JSONObject? = null

    /** Читает timings.json один раз. Вызывается при старте приложения. */
    fun load(context: Context) {
        if (data != null) return
        data = try {
            val text = context.applicationContext.assets.open("audio/timings.json")
                .bufferedReader().use { it.readText() }
            JSONObject(text)
        } catch (e: Exception) {
            Log.e("Timings", "timings.json не прочитан", e)
            JSONObject()
        }
    }

    private fun obj(file: String, lang: String): JSONObject? =
        data?.optJSONObject(file)?.optJSONObject(lang)

    /** Начало шага в миллисекундах. Если тайминга нет, вернёт null. */
    fun startMs(file: String, lang: String, stepId: String): Long? {
        val o = obj(file, lang) ?: return null
        if (!o.has(stepId)) return null
        return (o.getDouble(stepId) * 1000).toLong()
    }

    /** Конец шага: ближайшая большая метка (следующий шаг или "end"). */
    fun endMs(file: String, lang: String, stepId: String): Long? {
        val o = obj(file, lang) ?: return null
        if (!o.has(stepId)) return null
        val start = o.getDouble(stepId)
        var best: Double? = null
        val keys = o.keys()
        while (keys.hasNext()) {
            val v = o.getDouble(keys.next())
            if (v > start && (best == null || v < best)) best = v
        }
        return best?.let { (it * 1000).toLong() }
    }

    /** Отрезок шага внутри общего файла или null, если тайм-кода нет. */
    fun segment(file: String, lang: String, stepId: String): Segment? {
        val s = startMs(file, lang, stepId) ?: return null
        return Segment(s, endMs(file, lang, stepId))
    }
}