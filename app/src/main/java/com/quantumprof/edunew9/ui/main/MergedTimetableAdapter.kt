package com.quantumprof.edunew9.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quantumprof.edunew9.R
import com.quantumprof.edunew9.data.MergedTimetableEntry

class MergedTimetableAdapter(private val entries: List<MergedTimetableEntry>) :
    RecyclerView.Adapter<MergedTimetableAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val startTime: TextView = view.findViewById(R.id.tv_start_time)
        val endTime: TextView = view.findViewById(R.id.tv_end_time)
        val period: TextView = view.findViewById(R.id.tv_period)
        val subject: TextView = view.findViewById(R.id.tv_subject)
        val teacher: TextView = view.findViewById(R.id.tv_teacher)
        val room: TextView = view.findViewById(R.id.tv_room)
        val statusIcon: ImageView = view.findViewById(R.id.iv_status)
        val groupInfo: TextView = view.findViewById(R.id.tv_group_info)
        val durationIndicator: TextView = view.findViewById(R.id.tv_duration_indicator)
        val periodIndicator: View = view.findViewById(R.id.view_period_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.merged_timetable_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context

        // Setze die Zeiten
        holder.startTime.text = entry.startTime
        holder.endTime.text = entry.endTime

        // Formatiere die Stunden-Nummer für zusammengeführte Stunden
        val periodText = if (entry.isMultiplePeriod) {
            "${entry.periodRange}. Std"
        } else {
            when {
                entry.periods.first().contains(".") -> entry.periods.first().replace(".", ". Std")
                entry.periods.first().isNotEmpty() -> "${entry.periods.first()}. Std"
                else -> ""
            }
        }
        holder.period.text = periodText

        // Setze das Fach
        holder.subject.text = entry.subject

        // Setze den Lehrer
        val teacherText = when {
            entry.teacher.isNotEmpty() && !entry.teacher.startsWith("Herr") && !entry.teacher.startsWith("Frau") ->
                if (entry.teacher.length <= 4) entry.teacher else "Herr/Frau ${entry.teacher}"
            entry.teacher.isNotEmpty() -> entry.teacher
            else -> "Kein Lehrer"
        }
        holder.teacher.text = teacherText

        // Setze den Raum
        holder.room.text = if (entry.room.isNotEmpty()) entry.room else "?"

        // Gruppeninformation
        val groupText = when {
            entry.detectedGroup != null -> "Gruppe ${entry.detectedGroup}"
            entry.groupNames.isNotEmpty() -> entry.groupNames.joinToString(", ")
            else -> ""
        }

        if (groupText.isNotEmpty()) {
            holder.groupInfo.visibility = View.VISIBLE
            holder.groupInfo.text = groupText

            // Farbkodierung je nach Gruppe
            when (entry.detectedGroup) {
                "A" -> holder.groupInfo.setBackgroundColor(ContextCompat.getColor(context, R.color.group_a_color))
                "B" -> holder.groupInfo.setBackgroundColor(ContextCompat.getColor(context, R.color.group_b_color))
                else -> holder.groupInfo.setBackgroundColor(ContextCompat.getColor(context, R.color.group_neutral_color))
            }
        } else {
            holder.groupInfo.visibility = View.GONE
        }

        // Dauer-Indikator für Mehrfachstunden
        if (entry.isMultiplePeriod) {
            holder.durationIndicator.visibility = View.VISIBLE
            holder.durationIndicator.text = entry.durationText

            // Verschiedene Farben je nach Anzahl der Stunden
            val colorRes = when (entry.originalEntries.size) {
                2 -> R.color.double_period_color
                3 -> R.color.triple_period_color
                else -> R.color.multiple_period_color
            }
            holder.durationIndicator.setBackgroundColor(ContextCompat.getColor(context, colorRes))

            // Verstärkte visuelle Hervorhebung für Mehrfachstunden
            holder.periodIndicator.visibility = View.VISIBLE
            holder.periodIndicator.setBackgroundColor(ContextCompat.getColor(context, colorRes))

        } else {
            holder.durationIndicator.visibility = View.GONE
            holder.periodIndicator.visibility = View.GONE
        }

        // Prüfe ob Unterricht ausfällt
        val isCancelled = entry.type == "Entfällt"

        // Setze das Status-Icon basierend auf dem Typ
        when (entry.type) {
            "Entfällt" -> {
                holder.statusIcon.visibility = View.VISIBLE
                holder.statusIcon.setImageResource(R.drawable.ic_cancel_24)
                holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.substitution_cancelled_text))
            }
            "Änderung" -> {
                holder.statusIcon.visibility = View.VISIBLE
                holder.statusIcon.setImageResource(R.drawable.ic_edit_24)
                holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.substitution_changed_text))
            }
            else -> {
                holder.statusIcon.visibility = View.VISIBLE
                holder.statusIcon.setImageResource(R.drawable.ic_check_circle_24)
                holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.substitution_event_text))
            }
        }

        // Ausgegraut Darstellung für ausgefallene Stunden
        if (isCancelled) {
            holder.startTime.alpha = 0.5f
            holder.endTime.alpha = 0.5f
            holder.period.alpha = 0.5f
            holder.subject.alpha = 0.5f
            holder.teacher.alpha = 0.5f
            holder.room.alpha = 0.5f
            holder.groupInfo.alpha = 0.5f
            holder.durationIndicator.alpha = 0.5f

            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            holder.itemView.alpha = 0.7f
            holder.subject.paintFlags = holder.subject.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.startTime.alpha = 1.0f
            holder.endTime.alpha = 1.0f
            holder.period.alpha = 1.0f
            holder.subject.alpha = 1.0f
            holder.teacher.alpha = 1.0f
            holder.room.alpha = 1.0f
            holder.groupInfo.alpha = 1.0f
            holder.durationIndicator.alpha = 1.0f

            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            holder.itemView.alpha = 1.0f
            holder.subject.paintFlags = holder.subject.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        // Fachspezifische Farbkodierung
        val subjectColor = getSubjectColor(entry.subject)
        // Hier könnten Sie eine subtile Farbkodierung am linken Rand hinzufügen
    }

    private fun getSubjectColor(subject: String): Int {
        return when {
            subject.contains("Mathe", ignoreCase = true) -> R.color.timetable_math
            subject.contains("Deutsch", ignoreCase = true) ||
            subject.contains("Englisch", ignoreCase = true) ||
            subject.contains("Französisch", ignoreCase = true) -> R.color.timetable_language
            subject.contains("Physik", ignoreCase = true) ||
            subject.contains("Chemie", ignoreCase = true) ||
            subject.contains("Biologie", ignoreCase = true) -> R.color.timetable_science
            subject.contains("Geschichte", ignoreCase = true) ||
            subject.contains("Erdkunde", ignoreCase = true) -> R.color.timetable_history
            subject.contains("Kunst", ignoreCase = true) ||
            subject.contains("Musik", ignoreCase = true) -> R.color.timetable_arts
            subject.contains("Sport", ignoreCase = true) -> R.color.timetable_sport
            else -> R.color.timetable_default
        }
    }

    override fun getItemCount() = entries.size
}
