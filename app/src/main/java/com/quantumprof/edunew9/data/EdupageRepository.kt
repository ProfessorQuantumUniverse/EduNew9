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

class EdupageRepository(private val context: Context? = null) {
    private val apiService = ApiClient.instance

    private var loggedInHtmlCache: String? = null

    private var gsechash: String? = null

    suspend fun login(user: String, pass: String): Result<Boolean> {
        return try {
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

            // --- HIER IST DIE KORREKTUR ---
            // 1. LESE DEN BODY GENAU EINMAL und speichere ihn in einer lokalen Variable.
            val responseHtml = loginResponse.body()!!.string()

            // 2. Speichere diese lokale Variable im Cache für spätere Verwendung (Stundenplan).
            loggedInHtmlCache = responseHtml

            // 3. Verwende DIESELBE LOKALE VARIABLE, um den Hash zu extrahieren.
            val finalHash = extractGsecHash(responseHtml)
            // --- ENDE DER KORREKTUR ---

            if (finalHash != null) {
                SessionManager.gsechash = finalHash
                Log.d("EdupageLogin", "Finaler gsechash nach Login: $finalHash")
                Result.success(true)
            } else {
                // Jetzt prüfen wir den Inhalt des gecachten HTMLs auf die Fehlermeldung
                if (finalHash != null) {
                    this.gsechash = finalHash
                    SessionManager.gsechash = finalHash // Auch im SessionManager für andere Aufrufe
                    Log.d("EdupageLogin", "Login erfolgreich. Finaler gsechash: $finalHash")
                    Result.success(true)
                } else {
                    Result.failure(Exception("Login OK, aber finaler Token fehlt."))}
            }
        } catch (e: Exception) {
            loggedInHtmlCache = null // Bei Fehler Cache leeren
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

    private fun copyHtmlToClipboard(htmlContent: String) {
        try {
            context?.let {
                val clipboard = it.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("EduPage HTML Response", htmlContent)
                clipboard.setPrimaryClip(clip)
                Log.d("EdupageRepository", "HTML content copied to clipboard (${htmlContent.length} characters)")
            } ?: run {
                Log.w("EdupageRepository", "Context is null, cannot copy to clipboard")
            }
        } catch (e: Exception) {
            Log.e("EdupageRepository", "Failed to copy HTML to clipboard", e)
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

    suspend fun getTimetableForDate(date: Date): Result<List<TimetableEntry>> {
        val hash = this.gsechash ?: return Result.failure(Exception("Nicht eingeloggt."))
        val gson = Gson()

        try {
            // SCHRITT A: Hole das "Wörterbuch" (DBI)
            val dbiResponse = apiService.getMainDbi()
            val dbiJson = extractJsonFromResponse(dbiResponse, "mainDBIAccessor")
                ?: return Result.failure(Exception("DBI-Daten konnten nicht extrahiert werden."))
            val dbiData = gson.fromJson(dbiJson, JsonObject::class.java)
            val dbi = dbiData.getAsJsonObject("dbi") ?: return Result.failure(Exception("DBI-Objekt nicht gefunden."))

            val subjects = parseDbiMap(dbi.getAsJsonObject("subjects"))
            val teachers = parseDbiMap(dbi.getAsJsonObject("teachers"))
            val classrooms = parseDbiMap(dbi.getAsJsonObject("classrooms"))
            val periodsMap = parseDbiMap(dbi.getAsJsonObject("periods"), useShort = false)
            Log.d("TimetableFlow", "Schritt A: DBI-Daten erfolgreich geladen.")

            // SCHRITT B: Hole die aktuelle Stundenplan-Version (tt_num)
            val currentTtResponse = apiService.getCurrentTimetableData()
            val currentTtJson = extractJsonFromResponse(currentTtResponse, "curentttGetData")
                ?: return Result.failure(Exception("Stundenplan-Metadaten konnten nicht extrahiert werden."))
            val currentTtData = gson.fromJson(currentTtJson, JsonObject::class.java)
            val ttNum = currentTtData.get("tt_num")?.asInt
                ?: return Result.failure(Exception("Stundenplan-Versionsnummer (tt_num) nicht gefunden."))
            Log.d("TimetableFlow", "Schritt B: Stundenplan-Version ist $ttNum.")

            // SCHRITT C: Hole die Stundenplan-Einträge für die Woche
            val formattedDateForApi = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(date)
            val viewerResponse = apiService.getTimetableViewerData(formattedDateForApi, ttNum, hash)
            val viewerJson = extractJsonFromResponse(viewerResponse, "getTTViewerData")
                ?: return Result.failure(Exception("Stundenplan-Einträge konnten nicht extrahiert werden."))
            val viewerData = gson.fromJson(viewerJson, JsonObject::class.java)

            // Die Daten sind in einem verschachtelten 'data'-Objekt
            val dataObject = viewerData.getAsJsonObject("data")
            val timetableItems = mutableListOf<JsonObject>()
            // Iteriere über alle Tage der Woche, die in der Antwort enthalten sind
            dataObject.entrySet().forEach { dayEntry ->
                dayEntry.value.asJsonObject.getAsJsonArray("tt_items")?.forEach { item ->
                    timetableItems.add(item.asJsonObject)
                }
            }
            Log.d("TimetableFlow", "Schritt C: ${timetableItems.size} Stundenplan-Einträge für die Woche gefunden.")

            // Verarbeite die Einträge für den angeforderten Tag
            val entriesForDate = timetableItems.filter {
                it.get("date")?.asString == formattedDateForApi
            }

            val timetableEntries = mutableListOf<TimetableEntry>()
            for (item in entriesForDate) {
                val periodNum = item.get("period")?.asString ?: continue

                timetableEntries.add(
                    TimetableEntry(
                        period = periodsMap[periodNum] ?: periodNum,
                        startTime = item.get("starttime")?.asString ?: "",
                        endTime = item.get("endtime")?.asString ?: "",
                        subject = subjects[item.get("subjectid")?.asString] ?: "Unbekannt",
                        teacher = teachers[item.getAsJsonArray("teacherids")?.firstOrNull()?.asString] ?: "",
                        room = classrooms[item.getAsJsonArray("classroomids")?.firstOrNull()?.asString?.replace("*", "")] ?: "",
                        type = item.get("subst_info")?.asString?.trim()?.ifEmpty { "Stunde" } ?: "Stunde"
                    )
                )
            }
            val finalEntries = timetableEntries.distinctBy { it.period + it.subject }.sortedBy { it.startTime }
            Log.i("TimetableFlow", "ERFOLG! ${finalEntries.size} Einträge für $formattedDateForApi extrahiert.")
            return Result.success(finalEntries)

        } catch (e: Exception) {
            Log.e("TimetableFlow", "Ein schwerwiegender Fehler ist im API-Datenfluss aufgetreten.", e)
            return Result.failure(e)
        }
    }

    // Hilfsfunktion, um das JSON aus der JavaScript-Callback-Wrapper zu extrahieren
    private fun extractJsonFromResponse(response: Response<ResponseBody>, functionName: String): String? {
        if (!response.isSuccessful) return null
        val body = response.body()?.string() ?: return null
        val pattern = Pattern.compile("d\\['$functionName'\\]\\s*=\\s*(\\{.*\\});", Pattern.DOTALL)
        val matcher = pattern.matcher(body)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun parseTimetableFromLoginHtml(html: String, date: Date): List<TimetableEntry> {
        Log.d("TimetableParser", "Beginne mit dem Parsen des Stundenplans.")

        // 1. Robusterer Regex, der nicht zu "gierig" ist und beim ersten "});" stoppt.
        val pattern = Pattern.compile("\\.userhome\\((.*?)\\);\\s*\\}\\);", Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        if (!matcher.find()) {
            Log.e("TimetableParser", "Konnte den 'userhome' JSON-Block nicht finden. Regex hat versagt.")
            return emptyList()
        }
        Log.d("TimetableParser", "Userhome-Block gefunden.")

        val userhomeJson = matcher.group(1) ?: return emptyList()
        val gson = Gson()
        val data: JsonObject = try {
            gson.fromJson(userhomeJson, JsonObject::class.java)
        } catch (e: Exception) {
            Log.e("TimetableParser", "JSON-Parsing des Userhome-Blocks fehlgeschlagen.", e)
            return emptyList()
        }
        Log.d("TimetableParser", "Userhome-Block erfolgreich als JSON geparst.")

        // 2. Erstelle die ID-zu-Name-Maps aus dem DBI-Objekt
        val dbi = data.getAsJsonObject("dbi")
        if (dbi == null) {
            Log.e("TimetableParser", "DBI-Objekt im JSON nicht gefunden.")
            return emptyList()
        }
        val subjects = parseDbiMap(dbi.getAsJsonObject("subjects"))
        val teachers = parseDbiMap(dbi.getAsJsonObject("teachers"))
        val classrooms = parseDbiMap(dbi.getAsJsonObject("classrooms"))
        val periodsMap = parseDbiMap(dbi.getAsJsonObject("periods"), useShort = false)
        Log.d("TimetableParser", "DBI-Maps (Lehrer, Fächer, etc.) erfolgreich erstellt.")

        // 3. Finde den Plan für das angeforderte Datum
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(date)
        val dailyPlan = data.getAsJsonObject("dp")
            ?.getAsJsonObject("dates")
            ?.getAsJsonObject(formattedDate)
            ?.getAsJsonArray("plan")

        if (dailyPlan == null) {
            Log.d("TimetableParser", "Kein Stundenplan ('plan'-Array) für das Datum $formattedDate gefunden.")
            return emptyList()
        }
        Log.d("TimetableParser", "Stundenplan für $formattedDate gefunden. ${dailyPlan.size()} Einträge gefunden.")

        // 4. Gehe jeden Eintrag durch und baue das TimetableEntry-Objekt
        val timetableEntries = mutableListOf<TimetableEntry>()
        for (planElement in dailyPlan) {
            val planObject = planElement.asJsonObject

            // Überspringe leere Einträge, Pausen oder Termine ohne Fach
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
            // Das Sternchen (*) bei manchen Raum-IDs entfernen
            val classroomName = classrooms[classroomId?.replace("*", "")] ?: ""

            // Prüfen, ob die Stunde ausfällt (basierend auf dem "changes"-Array)
            val changes = planObject.getAsJsonArray("changes")
            var type = "Stunde"
            if (changes != null && changes.toString().contains("cancelled")) {
                type = "Entfällt"
            }

            timetableEntries.add(
                TimetableEntry(
                    period = periodInfo,
                    startTime = startTime,
                    endTime = endTime,
                    subject = subjectName,
                    teacher = teacherName,
                    room = classroomName,
                    type = type
                )
            )
        }

        // Korrigierte Logik zum Entfernen von Duplikaten:
        // Gruppiere nach Stunde und füge die Informationen zusammen, falls sie sich unterscheiden.
        val finalEntries = timetableEntries
            .groupBy { it.period }
            .map { (_, entries) ->
                if (entries.size > 1) {
                    // Wenn es mehrere Einträge für eine Stunde gibt (z.B. verschiedene Gruppen),
                    // nehmen wir den ersten als Basis. Man könnte hier auch komplexere Logik einbauen.
                    entries.first()
                } else {
                    entries.first()
                }
            }

        Log.d("TimetableParser", "Parsing beendet. ${finalEntries.size} finale Einträge werden zurückgegeben.")
        return finalEntries
    }


    private fun parseDbiMap(obj: JsonObject?, useShort: Boolean = true): Map<String, String> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (entry in obj.entrySet()) {
            val item = entry.value.asJsonObject
            // Name bevorzugen, da "short" manchmal fehlt oder nicht aussagekräftig ist.
            val name = item.get("name")?.asString
            if (!name.isNullOrEmpty()) {
                map[entry.key] = name
            }
        }
        return map
    }

    private fun parseSubstitutionHtml(html: String): List<SubstitutionEntry> {
        val entries = mutableListOf<SubstitutionEntry>()
        // 1. Finde den "report_html" String im JavaScript-Block
        val pattern = Pattern.compile("\"report_html\":\\s*\"(.*?)\"\\}\\);")
        val matcher = pattern.matcher(html.replace("\n", "")) // Zeilenumbrüche entfernen für einfacheres Regex
        if (!matcher.find()) {
            Log.e("Parser", "Konnte den 'report_html' Block nicht finden.")
            return entries
        }

        // 2. Extrahiere und dekodiere den inneren HTML-String
        var innerHtmlString = matcher.group(1) ?: return entries
        innerHtmlString = innerHtmlString.replace("\\\"", "\"").replace("\\/", "/")

        // 3. Parse das innere HTML mit JSoup
        val doc = Jsoup.parse(innerHtmlString)
        val rows = doc.select(".row") // Wähle alle Elemente mit der Klasse "row"

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