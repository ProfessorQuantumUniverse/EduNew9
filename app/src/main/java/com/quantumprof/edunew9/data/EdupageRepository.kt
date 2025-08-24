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

        // Hilfsmethode für Kompatibilität
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

            // **KRITISCH**: HTML-Cache in der SINGLETON-INSTANZ speichern
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
        Log.d("TimetableFlow", "=== STUNDENPLAN-ABRUF GESTARTET ===")
        Log.d("TimetableFlow", "Datum: $date")
        Log.d("TimetableFlow", "Repository-Instanz: ${this.hashCode()}")

        val hash = SessionManager.gsechash
        if (hash == null) {
            Log.e("TimetableFlow", "Kein gsechash verfügbar")
            return Result.failure(Exception("Nicht eingeloggt (kein Security-Token vorhanden)."))
        }
        Log.d("TimetableFlow", "gsechash verfügbar: ${hash.take(10)}...")

        // **DEBUGGING**: Status des HTML-Caches
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

            val timetableEntries = parseTimetableFromLoginHtml(htmlCache, date)

            if (timetableEntries.isNotEmpty()) {
                Log.d("TimetableFlow", "=== STUNDENPLAN ERFOLGREICH ===")
                Log.d("TimetableFlow", "Anzahl Einträge: ${timetableEntries.size}")
                return Result.success(timetableEntries)
            } else {
                Log.d("TimetableFlow", "=== KEIN STUNDENPLAN FÜR HEUTE ===")
                return Result.success(emptyList())
            }

        } catch (e: Exception) {
            Log.e("TimetableFlow", "Fehler beim HTML-Parsing", e)
            return Result.failure(e)
        }
    }

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

            // **DEBUGGING**: Zeige verfügbare Daten
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

        val finalEntries = timetableEntries
            .groupBy { it.period }
            .map { (_, entries) -> entries.first() }
            .sortedBy { it.startTime }

        Log.d("TimetableParser", "=== PARSING BEENDET ===")
        Log.d("TimetableParser", "Finale Einträge: ${finalEntries.size}")
        return finalEntries
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