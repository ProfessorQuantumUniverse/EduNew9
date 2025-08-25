package com.quantumprof.edunew9.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.quantumprof.edunew9.network.ApiClient
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import com.google.gson.Gson
import retrofit2.Response
import okhttp3.ResponseBody

class EdupageRepository private constructor(private val context: Context? = null) {
    private val apiService = ApiClient.instance

    private var loggedInHtmlCache: String? = null

    // **SINGLETON PATTERN**
    companion object {
        @Volatile
        private var INSTANCE: EdupageRepository? = null

        fun getInstance(context: Context? = null): EdupageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EdupageRepository(context).also { INSTANCE = it }
            }
        }

        fun getInstance(): EdupageRepository {
            return INSTANCE ?: throw IllegalStateException("EdupageRepository muss zuerst mit Context initialisiert werden")
        }
    }

    suspend fun login(user: String, pass: String): Result<Boolean> {
        return try {
            Log.d("EdupageLogin", "=== LOGIN GESTARTET ===")

            val pageResponse = apiService.getLoginPage()
            if (!pageResponse.isSuccessful || pageResponse.body() == null) {
                return Result.failure(Exception("Konnte Login-Seite nicht laden: ${pageResponse.code()}"))
            }

            val loginPageHtml = pageResponse.body()!!.string()
            val initialHash = extractGsecHash(loginPageHtml)
                ?: return Result.failure(Exception("Konnte initialen Security-Token nicht finden."))

            Log.d("EdupageLogin", "Initialer gsechash erhalten: $initialHash")

            val loginResponse = apiService.login(user, pass, "edupage", initialHash)
            if (!loginResponse.isSuccessful || loginResponse.body() == null) {
                return Result.failure(Exception("Login fehlgeschlagen: ${loginResponse.code()}"))
            }

            val responseHtml = loginResponse.body()!!.string()

            loggedInHtmlCache = responseHtml
            Log.d("EdupageLogin", "HTML-Cache gespeichert: ${responseHtml.length} Zeichen")
            Log.d("EdupageLogin", "HTML enthält 'userhome': ${responseHtml.contains("userhome")}")

            val finalHash = extractGsecHash(responseHtml)
            if (finalHash != null) {
                SessionManager.gsechash = finalHash
                Log.d("EdupageLogin", "=== LOGIN ERFOLGREICH ===")
                Log.d("EdupageLogin", "Finaler gsechash: $finalHash")
                Log.d("EdupageLogin", "HTML-Cache Status: ${loggedInHtmlCache != null}")
                Result.success(true)
            } else {
                Result.failure(Exception("Login OK, aber finaler Token fehlt."))
            }
        } catch (e: Exception) {
            loggedInHtmlCache = null
            Log.e("EdupageLogin", "Login-Fehler", e)
            Result.failure(e)
        }
    }

    private fun extractGsecHash(html: String): String? {
        val pattern = Pattern.compile("ASC\\.gsechash\\s*=\\s*\"(.*?)\";")
        val matcher = pattern.matcher(html)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    suspend fun getSubstitutionPlanForDate(date: Date): Result<List<SubstitutionEntry>> {
        val hash = SessionManager.gsechash
            ?: return Result.failure(Exception("Nicht eingeloggt (kein Security-Token vorhanden)."))

        return try {
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(date)
            val response = apiService.getSubstitutionPlanForDate(formattedDate, hash)

            if (response.isSuccessful && response.body() != null) {
                val outerHtml = response.body()!!.string()
                val entries = parseSubstitutionHtml(outerHtml)
                Result.success(entries)
            } else {
                Result.failure(Exception("Abruf fehlgeschlagen: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // **ERWEITERTE STUNDENPLAN-METHODE MIT GRUPPENFILTER**
    suspend fun getTimetableForDate(date: Date): Result<List<TimetableEntry>> {
        Log.d("TimetableFlow", "=== STUNDENPLAN-ABRUF GESTARTET ===")
        Log.d("TimetableFlow", "Datum: $date")
        Log.d("TimetableFlow", "Repository-Instanz: ${this.hashCode()}")

        val hash = SessionManager.gsechash
        if (hash == null) {
            Log.e("TimetableFlow", "Kein gsechash verfügbar")
            return Result.failure(Exception("Nicht eingeloggt (kein Security-Token vorhanden)."))
        }
        Log.d("TimetableFlow", "gsechash verfügbar: ${hash.take(10)}...")

        val htmlCache = this.loggedInHtmlCache
        Log.d("TimetableFlow", "HTML-Cache Status:")
        Log.d("TimetableFlow", "  - Cache verfügbar: ${htmlCache != null}")
        Log.d("TimetableFlow", "  - Cache Länge: ${htmlCache?.length ?: 0}")

        if (htmlCache == null) {
            Log.e("TimetableFlow", "KRITISCHER FEHLER: Kein HTML-Cache in Repository-Instanz ${this.hashCode()}")
            return Result.failure(Exception("Kein Login-HTML-Cache verfügbar. Repository-Problem!"))
        }

        try {
            Log.d("TimetableFlow", "Starte HTML-Parsing...")

            val allTimetableEntries = parseTimetableFromLoginHtml(htmlCache, date)

            // **NEUE GRUPPENFILTERUNG FÜR STUNDENPLAN ANWENDEN**
            val filteredEntries = context?.let { ctx ->
                applyTimetableGroupFilter(allTimetableEntries, ctx)
            } ?: allTimetableEntries

            if (filteredEntries.isNotEmpty()) {
                Log.d("TimetableFlow", "=== STUNDENPLAN ERFOLGREICH ===")
                Log.d("TimetableFlow", "Anzahl Einträge: ${filteredEntries.size}")
                return Result.success(filteredEntries)
            } else {
                Log.d("TimetableFlow", "=== KEIN STUNDENPLAN FÜR HEUTE ===")
                return Result.success(emptyList())
            }

        } catch (e: Exception) {
            Log.e("TimetableFlow", "Fehler beim HTML-Parsing", e)
            return Result.failure(e)
        }
    }

    // **VERBESSERTE GRUPPENFILTER-FUNKTION FÜR STUNDENPLAN**
    private fun applyTimetableGroupFilter(entries: List<TimetableEntry>, context: Context): List<TimetableEntry> {
        val selectedGroup = SettingsManager.getSelectedGroup(context)
        val autoFilter = SettingsManager.getAutoFilter(context)
        val selectedElective = SettingsManager.getSelectedElective(context)
        val autoFilterElectives = SettingsManager.getAutoFilterElectives(context)

        Log.d("TimetableGroupFilter", "=== FILTER-DEBUG ===")
        Log.d("TimetableGroupFilter", "Angewählte Gruppe: '$selectedGroup'")
        Log.d("TimetableGroupFilter", "Auto-Filter aktiviert: $autoFilter")
        Log.d("TimetableGroupFilter", "Angewählter Wahlkurs: '$selectedElective'")
        Log.d("TimetableGroupFilter", "Auto-Filter Wahlkurse aktiviert: $autoFilterElectives")
        Log.d("TimetableGroupFilter", "Stundenplan-Einträge vor Filterung: ${entries.size}")

        // **ERWEITERTE DEBUG-INFORMATIONEN**
        entries.forEachIndexed { index, entry ->
            Log.d("TimetableGroupFilter", "  [$index] ${entry.period}: '${entry.subject}' (Gruppe: ${entry.detectedGroup}, Gruppen: ${entry.groupNames})")
        }

        // **SCHRITT 1: GRUPPEN-FILTERUNG (A/B)**
        val groupFilteredEntries = if (autoFilter && selectedGroup != null) {
            applyGroupFilter(entries, selectedGroup)
        } else {
            Log.d("TimetableGroupFilter", "⚠ Gruppen-Filter deaktiviert oder keine Gruppe gewählt")
            entries
        }

        // **SCHRITT 2: WAHLKURS-FILTERUNG**
        val finalFilteredEntries = if (autoFilterElectives && selectedElective != null) {
            applyElectiveFilter(groupFilteredEntries, selectedElective)
        } else {
            Log.d("TimetableGroupFilter", "⚠ Wahlkurs-Filter deaktiviert oder kein Wahlkurs gewählt")
            groupFilteredEntries
        }

        Log.d("TimetableGroupFilter", "=== FILTER-ERGEBNIS ===")
        Log.d("TimetableGroupFilter", "Einträge nach Filterung: ${finalFilteredEntries.size}")
        finalFilteredEntries.forEach { entry ->
            Log.d("TimetableGroupFilter", "  → ${entry.period}: '${entry.subject}' (Gruppe: ${entry.detectedGroup})")
        }

        return finalFilteredEntries.sortedBy { it.startTime }
    }

    // **GRUPPEN-FILTERUNG (A/B) - AUSGELAGERT**
    private fun applyGroupFilter(entries: List<TimetableEntry>, selectedGroup: String): List<TimetableEntry> {
        Log.d("TimetableGroupFilter", "✓ Gruppen-Filter wird angewendet für Gruppe '$selectedGroup'")

        val entriesByPeriod = entries.groupBy { it.period }
        val filteredEntries = mutableListOf<TimetableEntry>()

        for ((period, periodEntries) in entriesByPeriod) {
            Log.d("TimetableGroupFilter", "Verarbeite Periode: $period mit ${periodEntries.size} Einträgen")

            // Sammle ausgefallene Stunden in dieser Periode separat (dürfen nie verschwinden)
            val cancelledEntries = periodEntries.filter { it.type == "Entfällt" }
            if (cancelledEntries.isNotEmpty()) {
                Log.d("TimetableGroupFilter", "  → ${cancelledEntries.size} ausgefallene(r) Eintrag/Einträge gefunden: ${cancelledEntries.map { it.subject }}")
            }

            if (periodEntries.size == 1) {
                val entry = periodEntries.first()

                if (entry.type == "Entfällt") {
                    // Ausgefallene Stunde immer anzeigen
                    filteredEntries.add(entry)
                    Log.d("TimetableGroupFilter", "  ✓ (Entfällt) Einzeleintrag aufgenommen: '${entry.subject}'")
                } else if (entry.detectedGroup == null || entry.detectedGroup == selectedGroup) {
                    filteredEntries.add(entry)
                    Log.d("TimetableGroupFilter", "  ✓ Einzeleintrag passend: '${entry.subject}' (Gruppe: ${entry.detectedGroup})")
                } else {
                    Log.d("TimetableGroupFilter", "  ✗ Einzeleintrag herausgefiltert: '${entry.subject}' (Gruppe: ${entry.detectedGroup} ≠ $selectedGroup)")
                }
            } else {
                Log.d("TimetableGroupFilter", "  Mehrere Einträge für Periode $period:")
                periodEntries.forEach { entry ->
                    Log.d("TimetableGroupFilter", "    - '${entry.subject}' (Typ: ${entry.type}, Gruppe: ${entry.detectedGroup}, Gruppen: ${entry.groupNames})")
                }

                val selectedGroupEntry = periodEntries.find { entry ->
                    entry.detectedGroup == selectedGroup && entry.type != "Entfällt"
                }

                if (selectedGroupEntry != null) {
                    filteredEntries.add(selectedGroupEntry)
                    Log.d("TimetableGroupFilter", "  ✓ Ausgewählte Gruppe gefunden: '${selectedGroupEntry.subject}'")
                } else {
                    val commonEntries = periodEntries.filter { entry ->
                        entry.detectedGroup == null && entry.type != "Entfällt"
                    }

                    if (commonEntries.isNotEmpty()) {
                        filteredEntries.addAll(commonEntries)
                        Log.d("TimetableGroupFilter", "  ✓ Gemeinsame Stunden: ${commonEntries.map { it.subject }}")
                    } else {
                        // Fallback nur wenn keine normalen Einträge außer Entfall vorhanden
                        val fallbackEntry = periodEntries.firstOrNull { it.type != "Entfällt" }
                        if (fallbackEntry != null) {
                            filteredEntries.add(fallbackEntry)
                            Log.d("TimetableGroupFilter", "  ⚠ Fallback: '${fallbackEntry.subject}' (keine passende Gruppe gefunden)")
                        }
                    }
                }
            }

            // Stelle sicher, dass ALLE ausgefallenen Stunden ebenfalls (zusätzlich) angezeigt werden
            cancelledEntries.forEach { cancelled ->
                if (!filteredEntries.contains(cancelled)) {
                    filteredEntries.add(cancelled)
                    Log.d("TimetableGroupFilter", "  ✓ (Entfällt) zusätzlich hinzugefügt: '${cancelled.subject}'")
                }
            }
        }

        Log.d("TimetableGroupFilter", "Nach Gruppen-Filter: ${filteredEntries.size} Einträge (inkl. Entfälle)")
        return filteredEntries
    }

    // **WAHLKURS-FILTERUNG (ORCHESTER/KUNSTWERKSTATT/OBERSTUFENCHOR)**
    private fun applyElectiveFilter(entries: List<TimetableEntry>, selectedElective: String): List<TimetableEntry> {
        Log.d("TimetableGroupFilter", "✓ Wahlkurs-Filter wird angewendet für '$selectedElective'")

        val availableElectives = SettingsManager.getAvailableElectives()
        Log.d("TimetableGroupFilter", "Verfügbare Wahlkurse: $availableElectives")

        val filteredEntries = entries.filter { entry ->
            // Ausgefallene Stunden nie herausfiltern
            if (entry.type == "Entfällt") {
                Log.d("TimetableGroupFilter", "  ✓ (Entfällt) nicht gefiltert: '${entry.subject}'")
                return@filter true
            }

            val isSelectedElective = entry.subject.contains(selectedElective, ignoreCase = true)
            val isOtherElective = availableElectives.any { elective ->
                elective != selectedElective && entry.subject.contains(elective, ignoreCase = true)
            }

            val shouldInclude = when {
                isSelectedElective -> {
                    Log.d("TimetableGroupFilter", "  ✓ Ausgewählter Wahlkurs: '${entry.subject}'")
                    true
                }
                isOtherElective -> {
                    Log.d("TimetableGroupFilter", "  ✗ Anderer Wahlkurs herausgefiltert: '${entry.subject}'")
                    false
                }
                else -> {
                    Log.d("TimetableGroupFilter", "  ✓ Reguläres Fach: '${entry.subject}'")
                    true
                }
            }
            shouldInclude
        }

        Log.d("TimetableGroupFilter", "Nach Wahlkurs-Filter: ${filteredEntries.size} Einträge (inkl. Entfälle)")
        return filteredEntries
    }

    // **ERWEITERTE STUNDENPLAN-PARSING MIT GRUPPENERKENNUNG**
    private fun parseTimetableFromLoginHtml(html: String, date: Date): List<TimetableEntry> {
        Log.d("TimetableParser", "=== HTML-PARSING GESTARTET ===")
        Log.d("TimetableParser", "HTML Länge: ${html.length}")

        val pattern = Pattern.compile("\\.userhome\\((.*?)\\);\\s*\\}\\);", Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        if (!matcher.find()) {
            Log.e("TimetableParser", "FEHLER: Konnte 'userhome' JSON-Block nicht finden")
            Log.d("TimetableParser", "HTML Anfang: ${html.take(500)}")
            return emptyList()
        }
        Log.d("TimetableParser", "✓ Userhome-Block gefunden")

        val userhomeJson = matcher.group(1) ?: return emptyList()
        Log.d("TimetableParser", "JSON Länge: ${userhomeJson.length}")

        val gson = Gson()
        val data: JsonObject = try {
            gson.fromJson(userhomeJson, JsonObject::class.java)
        } catch (e: Exception) {
            Log.e("TimetableParser", "FEHLER: JSON-Parsing fehlgeschlagen", e)
            Log.d("TimetableParser", "JSON Anfang: ${userhomeJson.take(200)}")
            return emptyList()
        }
        Log.d("TimetableParser", "✓ JSON erfolgreich geparst")

        val dbi = data.getAsJsonObject("dbi")
        if (dbi == null) {
            Log.e("TimetableParser", "FEHLER: Kein DBI-Objekt im JSON")
            return emptyList()
        }
        Log.d("TimetableParser", "✓ DBI-Objekt gefunden")

        val subjects = parseDbiMap(dbi.getAsJsonObject("subjects"))
        val teachers = parseDbiMap(dbi.getAsJsonObject("teachers"))
        val classrooms = parseDbiMap(dbi.getAsJsonObject("classrooms"))
        val periodsMap = parseDbiMap(dbi.getAsJsonObject("periods"), useShort = false)
        Log.d("TimetableParser", "✓ DBI-Maps erstellt: ${subjects.size} Fächer, ${teachers.size} Lehrer")

        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(date)
        Log.d("TimetableParser", "Suche Plan für Datum: $formattedDate")

        val dailyPlan = data.getAsJsonObject("dp")
            ?.getAsJsonObject("dates")
            ?.getAsJsonObject(formattedDate)
            ?.getAsJsonArray("plan")

        if (dailyPlan == null) {
            Log.d("TimetableParser", "ℹ Kein Plan für $formattedDate gefunden")

            val dp = data.getAsJsonObject("dp")
            if (dp != null) {
                val dates = dp.getAsJsonObject("dates")
                if (dates != null) {
                    Log.d("TimetableParser", "Verfügbare Daten für Daten:")
                    dates.entrySet().take(5).forEach { entry ->
                        Log.d("TimetableParser", "  - ${entry.key}")
                    }
                }
            }
            return emptyList()
        }
        Log.d("TimetableParser", "✓ Tagesplan gefunden: ${dailyPlan.size()} Einträge")

        val timetableEntries = mutableListOf<TimetableEntry>()
        for (planElement in dailyPlan) {
            val planObject = planElement.asJsonObject

            if (!planObject.has("subjectid") || planObject.get("subjectid").asString.isNullOrEmpty()) {
                continue
            }

            val periodNum = planObject.get("period")?.asString ?: continue
            val periodInfo = periodsMap[periodNum] ?: periodNum
            val startTime = planObject.get("starttime")?.asString ?: ""
            val endTime = planObject.get("endtime")?.asString ?: ""

            val subjectId = planObject.get("subjectid").asString
            val subjectName = subjects[subjectId] ?: "Unbekanntes Fach"

            val teacherId = planObject.getAsJsonArray("teacherids")?.firstOrNull()?.asString
            val teacherName = teachers[teacherId] ?: ""

            val classroomId = planObject.getAsJsonArray("classroomids")?.firstOrNull()?.asString
            val classroomName = classrooms[classroomId?.replace("*", "")] ?: ""

            // **NEUE GRUPPENERKENNUNG FÜR STUNDENPLAN**
            val groupNamesArray = planObject.getAsJsonArray("groupnames")
            val groupNames = mutableListOf<String>()
            if (groupNamesArray != null) {
                for (element in groupNamesArray) {
                    groupNames.add(element.asString)
                }
            }

            val detectedGroup = detectTimetableGroupFromNames(groupNames, subjectName, teacherName)

            val changes = planObject.getAsJsonArray("changes")
            var type = "Stunde"

            // Debug-Logging für ausgefallene Stunden
            Log.d("TimetableParser", "=== EINTRAG ANALYSIS ===")
            Log.d("TimetableParser", "Fach: $subjectName")
            Log.d("TimetableGroupDetection", "Fach: $subjectName, Gruppenamen: $groupNames, Erkannte Gruppe: $detectedGroup")
            Log.d("TimetableParser", "Changes Array: $changes")
            Log.d("TimetableParser", "Changes String: ${changes?.toString()}")

            if (changes != null && changes.toString().contains("cancelled")) {
                type = "Entfällt"
                Log.d("TimetableParser", "✓ ENTFALL ERKANNT für $subjectName")
            } else {
                Log.d("TimetableParser", "○ Normale Stunde: $subjectName")
            }

            timetableEntries.add(
                TimetableEntry(
                    period = periodInfo,
                    startTime = startTime,
                    endTime = endTime,
                    subject = subjectName,
                    teacher = teacherName,
                    room = classroomName,
                    type = type,
                    detectedGroup = detectedGroup, // **NEU**
                    groupNames = groupNames // **NEU**
                )
            )
        }

        // **WICHTIG: KEINE VORZEITIGE GRUPPIERUNG MEHR!**
        // Entferne die vorzeitige Gruppierung, damit die Gruppenfilterung alle Einträge sieht
        Log.d("TimetableParser", "=== PARSING BEENDET ===")
        Log.d("TimetableParser", "Alle Einträge vor Filterung: ${timetableEntries.size}")
        return timetableEntries.sortedBy { it.startTime }
    }

    // **NEUE GRUPPENERKENNUNG SPEZIELL FÜR STUNDENPLAN**
    private fun detectTimetableGroupFromNames(groupNames: List<String>, subject: String, teacher: String): String? {
        // 1. Direkte Gruppenerkennung aus groupnames
        for (groupName in groupNames) {
            val groupPatterns = listOf(
                Regex(".*Gruppe\\s*([AB]).*", RegexOption.IGNORE_CASE),
                Regex(".*Gr\\.?\\s*([AB]).*", RegexOption.IGNORE_CASE),
                Regex(".*\\(([AB])\\).*", RegexOption.IGNORE_CASE),
                Regex(".*([AB])\\s*-\\s*Gruppe.*", RegexOption.IGNORE_CASE),
                Regex(".*\\b([AB])\\b.*", RegexOption.IGNORE_CASE),
                Regex("^([AB])$", RegexOption.IGNORE_CASE)
            )

            for (pattern in groupPatterns) {
                val match = pattern.find(groupName)
                if (match != null) {
                    val group = match.groupValues[1].uppercase()
                    return group
                }
            }
        }

        // 2. Gruppenerkennung aus Fachname
        val subjectGroup = detectGroupFromText(subject)
        if (subjectGroup != null) {
            return subjectGroup
        }

        // 3. Gruppenerkennung aus Lehrername
        val teacherGroup = detectGroupFromText(teacher)
        if (teacherGroup != null) {
            return teacherGroup
        }

        return null
    }

    // **HILFSMETHODE FÜR GRUPPENERKENNUNG AUS TEXT**
    private fun detectGroupFromText(text: String): String? {
        val groupPatterns = listOf(
            Regex("Gruppe\\s*([AB])(?:\\s|,|\\.|$)", RegexOption.IGNORE_CASE),
            Regex("Gr\\.\\s*([AB])(?:\\s|,|\\.|$)", RegexOption.IGNORE_CASE),
            Regex("([AB])\\s*-\\s*Gruppe", RegexOption.IGNORE_CASE),
            Regex("\\(([AB])\\)", RegexOption.IGNORE_CASE),
            Regex("([AB])\\s*Gr", RegexOption.IGNORE_CASE),
            Regex("\\b([AB])\\s*,", RegexOption.IGNORE_CASE),
            Regex("\\s([AB])\\s*$", RegexOption.IGNORE_CASE)
        )

        for (pattern in groupPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].uppercase()
            }
        }
        return null
    }

    private fun parseDbiMap(obj: JsonObject?, useShort: Boolean = true): Map<String, String> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (entry in obj.entrySet()) {
            val item = entry.value.asJsonObject
            val name = item.get("name")?.asString
            if (!name.isNullOrEmpty()) {
                map[entry.key] = name
            }
        }
        return map
    }

    private fun parseSubstitutionHtml(html: String): List<SubstitutionEntry> {
        val entries = mutableListOf<SubstitutionEntry>()
        val pattern = Pattern.compile("\"report_html\":\\s*\"(.*?)\"\\}\\);")
        val matcher = pattern.matcher(html.replace("\n", ""))
        if (!matcher.find()) {
            Log.e("Parser", "Konnte den 'report_html' Block nicht finden.")
            return entries
        }

        var innerHtmlString = matcher.group(1) ?: return entries
        innerHtmlString = innerHtmlString.replace("\\\"", "\"").replace("\\/", "/")

        val doc = Jsoup.parse(innerHtmlString)
        val rows = doc.select(".row")

        for (row in rows) {
            val period = row.select(".period span").text()
            val info = row.select(".info span").text()
            val type = when {
                row.hasClass("event") -> "Event"
                row.hasClass("remove") -> "Entfällt"
                row.hasClass("change") -> "Änderung"
                else -> "Unbekannt"
            }
            if (period.isNotEmpty() && info.isNotEmpty()) {
                entries.add(SubstitutionEntry(period, info, type))
            }
        }
        return entries
    }
}