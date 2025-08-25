package com.quantumprof.edunew9.data


// Hält die Informationen für einen einzelnen Stundenplan-Eintrag.
data class TimetableEntry(
    val period: String,
    val startTime: String,
    val endTime: String,
    val subject: String,
    val teacher: String,
    val room: String,
    val type: String, // z.B. "Stunde", "Entfällt", "Änderung"
    val detectedGroup: String? = null, // "A", "B", oder null - NEU HINZUGEFÜGT
    val groupNames: List<String> = emptyList() // Original groupnames aus JSON - NEU HINZUGEFÜGT
)
