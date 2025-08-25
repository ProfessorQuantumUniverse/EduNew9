package com.quantumprof.edunew9.utils

import android.util.Log
import com.quantumprof.edunew9.data.MergedTimetableEntry
import com.quantumprof.edunew9.data.TimetableEntry

object TimetableMerger {

    /**
     * Führt aufeinanderfolgende Stunden mit identischen Eigenschaften zusammen
     */
    fun mergeConsecutiveLessons(entries: List<TimetableEntry>): List<MergedTimetableEntry> {
        if (entries.isEmpty()) return emptyList()

        // Filtere "Hauptunterricht" aus - wird komplett ignoriert
        val filteredEntries = entries.filter { entry ->
            !entry.subject.equals("Hauptunterricht", ignoreCase = true)
        }

        Log.d("TimetableMerger", "Starting merge with ${entries.size} entries (${filteredEntries.size} after filtering out Hauptunterricht)")

        if (filteredEntries.isEmpty()) return emptyList()

        val merged = mutableListOf<MergedTimetableEntry>()
        var currentGroup = mutableListOf<TimetableEntry>()

        for (entry in filteredEntries) {
            Log.d("TimetableMerger", "Processing entry: ${entry.period} - ${entry.subject} - ${entry.teacher} - ${entry.room}")

            if (currentGroup.isEmpty()) {
                // Erste Stunde der Gruppe
                currentGroup.add(entry)
                Log.d("TimetableMerger", "Started new group with period ${entry.period}")
            } else {
                val lastEntry = currentGroup.last()

                // Prüfe ob die aktuelle Stunde zur Gruppe gehört
                if (canMergeEntries(lastEntry, entry)) {
                    currentGroup.add(entry)
                    Log.d("TimetableMerger", "Added to group: ${entry.period} (group size: ${currentGroup.size})")
                } else {
                    // Schließe die aktuelle Gruppe ab und starte eine neue
                    val mergedEntry = createMergedEntry(currentGroup)
                    merged.add(mergedEntry)
                    Log.d("TimetableMerger", "Closed group with ${currentGroup.size} entries: ${mergedEntry.periodRange}")
                    currentGroup = mutableListOf(entry)
                }
            }
        }

        // Füge die letzte Gruppe hinzu
        if (currentGroup.isNotEmpty()) {
            val mergedEntry = createMergedEntry(currentGroup)
            merged.add(mergedEntry)
            Log.d("TimetableMerger", "Final group with ${currentGroup.size} entries: ${mergedEntry.periodRange}")
        }

        Log.d("TimetableMerger", "Merge complete: ${entries.size} entries -> ${merged.size} merged entries")
        val multiPeriodCount = merged.count { it.isMultiplePeriod }
        Log.d("TimetableMerger", "Multi-period entries: $multiPeriodCount")

        return merged
    }

    /**
     * Prüft ob zwei aufeinanderfolgende Stunden zusammengeführt werden können
     */
    private fun canMergeEntries(entry1: TimetableEntry, entry2: TimetableEntry): Boolean {
        val canMerge = entry1.subject == entry2.subject &&
               entry1.teacher == entry2.teacher &&
               entry1.room == entry2.room &&
               entry1.type == entry2.type &&
               entry1.detectedGroup == entry2.detectedGroup &&
               areConsecutivePeriods(entry1.period, entry2.period)

        Log.d("TimetableMerger", "Can merge ${entry1.period} -> ${entry2.period}? $canMerge")
        Log.d("TimetableMerger", "  Subject: ${entry1.subject} == ${entry2.subject} = ${entry1.subject == entry2.subject}")
        Log.d("TimetableMerger", "  Teacher: ${entry1.teacher} == ${entry2.teacher} = ${entry1.teacher == entry2.teacher}")
        Log.d("TimetableMerger", "  Room: ${entry1.room} == ${entry2.room} = ${entry1.room == entry2.room}")
        Log.d("TimetableMerger", "  Consecutive: ${areConsecutivePeriods(entry1.period, entry2.period)}")

        return canMerge
    }

    /**
     * Prüft ob zwei Stundenangaben direkt aufeinanderfolgen
     */
    private fun areConsecutivePeriods(period1: String, period2: String): Boolean {
        return try {
            // Erweiterte Bereinigung für verschiedene Periodenformate
            val cleanPeriod1 = period1.replace(Regex("[^0-9]"), "").trim()
            val cleanPeriod2 = period2.replace(Regex("[^0-9]"), "").trim()

            Log.d("TimetableMerger", "Period parsing: '$period1' -> '$cleanPeriod1', '$period2' -> '$cleanPeriod2'")

            val p1 = cleanPeriod1.toIntOrNull()
            val p2 = cleanPeriod2.toIntOrNull()

            if (p1 != null && p2 != null) {
                val consecutive = p2 == p1 + 1
                Log.d("TimetableMerger", "Period check: $period1 ($p1) + 1 == $period2 ($p2) = $consecutive")
                return consecutive
            }

            Log.d("TimetableMerger", "Could not parse periods: '$period1' -> '$cleanPeriod1' ($p1), '$period2' -> '$cleanPeriod2' ($p2)")
            false
        } catch (e: Exception) {
            Log.e("TimetableMerger", "Error checking consecutive periods: $period1, $period2", e)
            false
        }
    }

    /**
     * Erstellt einen zusammengefassten Eintrag aus einer Gruppe von Stunden
     */
    private fun createMergedEntry(entries: List<TimetableEntry>): MergedTimetableEntry {
        val firstEntry = entries.first()
        val lastEntry = entries.last()

        return MergedTimetableEntry(
            periods = entries.map { it.period },
            startTime = firstEntry.startTime,
            endTime = lastEntry.endTime,
            subject = firstEntry.subject,
            teacher = firstEntry.teacher,
            room = firstEntry.room,
            type = firstEntry.type,
            detectedGroup = firstEntry.detectedGroup,
            groupNames = firstEntry.groupNames,
            originalEntries = entries
        )
    }

    /**
     * Alternative Methode zum Testen - führt Stunden mit identischem Fach zusammen,
     * auch wenn die Perioden-Parsing-Logik nicht funktioniert
     */
    fun mergeBySubjectAndTime(entries: List<TimetableEntry>): List<MergedTimetableEntry> {
        if (entries.isEmpty()) return emptyList()

        Log.d("TimetableMerger", "Testing merge by subject and time with ${entries.size} entries")

        val merged = mutableListOf<MergedTimetableEntry>()
        var currentGroup = mutableListOf<TimetableEntry>()

        for (entry in entries) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(entry)
            } else {
                val lastEntry = currentGroup.last()

                // Vereinfachte Logik: Nur Fach, Lehrer und Raum müssen übereinstimmen
                if (lastEntry.subject == entry.subject &&
                    lastEntry.teacher == entry.teacher &&
                    lastEntry.room == entry.room &&
                    lastEntry.endTime == entry.startTime) { // Zeitlich direkt aufeinanderfolgend

                    currentGroup.add(entry)
                    Log.d("TimetableMerger", "Merged by time: ${lastEntry.subject} ${lastEntry.endTime} -> ${entry.startTime}")
                } else {
                    merged.add(createMergedEntry(currentGroup))
                    currentGroup = mutableListOf(entry)
                }
            }
        }

        if (currentGroup.isNotEmpty()) {
            merged.add(createMergedEntry(currentGroup))
        }

        val multiPeriodCount = merged.count { it.isMultiplePeriod }
        Log.d("TimetableMerger", "Alternative merge: ${entries.size} -> ${merged.size} entries ($multiPeriodCount multi-period)")

        return merged
    }
}
