package com.quantumprof.edunew9.data

/**
 * Repräsentiert einen zusammengeführten Stundenplan-Eintrag für aufeinanderfolgende Stunden
 */
data class MergedTimetableEntry(
    val periods: List<String>,
    val startTime: String,
    val endTime: String,
    val subject: String,
    val teacher: String,
    val room: String,
    val type: String, // z.B. "Stunde", "Entfällt", "Änderung"
    val detectedGroup: String? = null,
    val groupNames: List<String> = emptyList(),
    val originalEntries: List<TimetableEntry> = emptyList()
) {
    /**
     * Gibt zurück, ob dies eine Mehrfachstunde ist (Doppel-, Dreifachstunde etc.)
     */
    val isMultiplePeriod: Boolean
        get() = periods.size > 1

    /**
     * Erstellt einen lesbaren String für den Periodenbereich
     */
    val periodRange: String
        get() = if (isMultiplePeriod) {
            "${periods.first()}-${periods.last()}"
        } else {
            periods.firstOrNull() ?: ""
        }

    /**
     * Gibt die Gesamtdauer in Stunden zurück
     */
    val durationInPeriods: Int
        get() = periods.size

    /**
     * Gibt einen beschreibenden Text für die Dauer zurück
     */
    val durationDescription: String
        get() = when (periods.size) {
            1 -> "Einzelstunde"
            2 -> "Doppelstunde"
            3 -> "Dreifachstunde"
            else -> "${periods.size} Stunden"
        }
}