package com.quantumprof.edunew9.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quantumprof.edunew9.R
import com.quantumprof.edunew9.data.TimetableEntry

class TimetableAdapter(private val entries: List<TimetableEntry>) :
    RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val startTime: TextView = view.findViewById(R.id.tv_start_time)
        val endTime: TextView = view.findViewById(R.id.tv_end_time)
        val period: TextView = view.findViewById(R.id.tv_period)
        val subject: TextView = view.findViewById(R.id.tv_subject)
        val teacher: TextView = view.findViewById(R.id.tv_teacher)
        val room: TextView = view.findViewById(R.id.tv_room)
        val statusIcon: ImageView = view.findViewById(R.id.iv_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.timetable_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context

        // Setze die Zeiten
        holder.startTime.text = entry.startTime
        holder.endTime.text = entry.endTime

        // Formatiere die Stunden-Nummer schöner
        val periodText = when {
            entry.period.contains(".") -> entry.period.replace(".", ". Std")
            entry.period.isNotEmpty() -> "${entry.period}. Std"
            else -> ""
        }
        holder.period.text = periodText

        // Setze das Fach
        holder.subject.text = entry.subject

        // Setze den Lehrer (mit besserer Formatierung)
        val teacherText = when {
            entry.teacher.isNotEmpty() && !entry.teacher.startsWith("Herr") && !entry.teacher.startsWith("Frau") ->
                if (entry.teacher.length <= 4) entry.teacher else "Herr/Frau ${entry.teacher}"
            entry.teacher.isNotEmpty() -> entry.teacher
            else -> "Kein Lehrer"
        }
        holder.teacher.text = teacherText

        // Setze den Raum
        holder.room.text = if (entry.room.isNotEmpty()) entry.room else "?"

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

        // Setze die Hintergrundfarbe basierend auf dem Fach
        val backgroundColor = getSubjectColor(entry.subject)
        // Hier würde man normalerweise den Hintergrund setzen, aber das machen wir über das Layout
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