package com.quantumprof.edunew9.data

/**
 * Repräsentiert eine zusammengefasste Stundengruppe (Doppel-/Dreifachstunden)
 */
data class MergedTimetableEntry(
    val periods: List<String>, // ["1", "2"] für Doppelstunde
    val startTime: String,
    val endTime: String,
    val subject: String,
    val teacher: String,
    val room: String,
    val type: String,
    val detectedGroup: String? = null,
    val groupNames: List<String> = emptyList(),
    val originalEntries: List<TimetableEntry>, // Original-Einträge für Details
    val isMultiplePeriod: Boolean = originalEntries.size > 1
) {
    val periodRange: String
        get() = if (isMultiplePeriod) {
            "${periods.first()}-${periods.last()}"
        } else {
            periods.firstOrNull() ?: ""
        }

    val durationText: String
        get() = when (originalEntries.size) {
            1 -> "Einzelstunde"
            2 -> "Doppelstunde"
            3 -> "Dreifachstunde"
            else -> "${originalEntries.size} Stunden"
        }
}
