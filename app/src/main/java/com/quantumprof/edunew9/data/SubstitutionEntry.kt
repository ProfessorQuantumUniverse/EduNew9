package com.quantumprof.edunew9.data


// Eine einfache Klasse, um die extrahierten Vertretungsdaten zu halten.
data class SubstitutionEntry(
    val period: String, // z.B. "1.HU - 2.HU" oder "(2.FS)"
    val info: String,   // z.B. "Geschichte Epoche - Geschichte, Dörr"
    val type: String    // z.B. "Event", "Entfällt", "Änderung"
)