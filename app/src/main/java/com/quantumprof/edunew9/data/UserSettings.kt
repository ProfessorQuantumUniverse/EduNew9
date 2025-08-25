package com.quantumprof.edunew9.data

data class UserSettings(
    val selectedGroup: String? = null, // "A", "B", null = alle anzeigen
    val autoFilterGroups: Boolean = true
)