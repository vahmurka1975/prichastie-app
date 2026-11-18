package ru.example.prichastie

import android.content.Context
import java.io.IOException
import ru.example.prichastie.model.Language

object TextLoader {

    fun load(context: Context, textKey: String, language: Language): String {
        val suffix = if (language == Language.MODERN) "mod" else "csl"
        return read(context, "text/$textKey.$suffix.txt")
            ?: read(context, "text/stub.$suffix.txt")
            ?: "Текст не найден."
    }

    private fun read(context: Context, path: String): String? =
        try {
            context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
                .removePrefix("\uFEFF")   // на случай BOM, если файл сохранён «с сигнатурой»
                .trim()
        } catch (e: IOException) {
            null
        }
}