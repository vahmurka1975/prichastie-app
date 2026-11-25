package ru.example.prichastie.updates

internal object ReleaseVersion {
    private val format = Regex("[vV]?[0-9]+(?:\\.[0-9]+){0,3}")
    private fun parse(value: String): List<Long>? {
        val text = value.trim()
        if (!format.matches(text)) return null
        return text.removePrefix("v").removePrefix("V").split('.').map {
            it.toLongOrNull() ?: return null
        }
    }
    fun isNewer(candidate: String, installed: String): Boolean? {
        val a = parse(candidate) ?: return null
        val b = parse(installed) ?: return null
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0L }
            val y = b.getOrElse(i) { 0L }
            if (x != y) return x > y
        }
        return false
    }
}
