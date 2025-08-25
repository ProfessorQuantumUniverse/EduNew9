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

        // Zeit-Anzeige
        holder.startTime.text = entry.startTime
        holder.endTime.text = entry.endTime

        // Fach-Name
        holder.subject.text = entry.subject

        // Lehrer-Information
        val teacherText = when {
            entry.teacher.isNotEmpty() && !entry.teacher.startsWith("Herr") && !entry.teacher.startsWith("Frau") ->
                if (entry.teacher.length <= 4) entry.teacher else "Herr/Frau ${entry.teacher}"
            entry.teacher.isNotEmpty() -> entry.teacher
            else -> "Kein Lehrer"
        }
        holder.teacher.text = teacherText

        // Raum-Information
        holder.room.text = if (entry.room.isNotEmpty()) entry.room else "?"

        // Gruppen-Information
        setupGroupInfo(holder, entry, context)

        // Status-Icon
        setupStatusIcon(holder, entry, context)

        // Dauer-Indikator
        setupDurationIndicator(holder, entry, context)

        // **VERBESSERTE PERIODE-INDIKATOR LOGIK**
        setupModernPeriodIndicator(holder, entry, context)

        // Ausgefallene Stunden-Styling
        applyAppearanceForCancelledLessons(holder, entry, context)
    }

    private fun setupGroupInfo(holder: ViewHolder, entry: MergedTimetableEntry, context: android.content.Context) {
        val groupText = when {
            entry.detectedGroup != null -> "Gruppe ${entry.detectedGroup}"
            entry.groupNames.isNotEmpty() -> entry.groupNames.joinToString(", ")
            else -> ""
        }

        if (groupText.isNotEmpty()) {
            holder.groupInfo.visibility = View.VISIBLE
            holder.groupInfo.text = groupText

            when (entry.detectedGroup) {
                "A" -> {
                    holder.groupInfo.setBackgroundResource(R.drawable.group_badge_background_modern)
                    holder.groupInfo.setTextColor(ContextCompat.getColor(context, R.color.group_a_text))
                }
                "B" -> {
                    holder.groupInfo.setBackgroundColor(ContextCompat.getColor(context, R.color.group_b_color))
                    holder.groupInfo.setTextColor(ContextCompat.getColor(context, R.color.group_b_text))
                }
                else -> {
                    holder.groupInfo.setBackgroundColor(ContextCompat.getColor(context, R.color.group_neutral_color))
                    holder.groupInfo.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_onSurfaceVariant))
                }
            }
        } else {
            holder.groupInfo.visibility = View.GONE
        }
    }

    private fun setupStatusIcon(holder: ViewHolder, entry: MergedTimetableEntry, context: android.content.Context) {
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
    }

    private fun setupDurationIndicator(holder: ViewHolder, entry: MergedTimetableEntry, context: android.content.Context) {
        if (entry.isMultiplePeriod) {
            holder.durationIndicator.visibility = View.VISIBLE

            val (text, backgroundRes, textColor) = when (entry.periods.size) {
                2 -> Triple("2 std", R.drawable.duration_indicator_background_modern, R.color.badge_text_light)
                3 -> Triple("3 std.", R.drawable.triple_period_background, R.color.badge_text_light)
                else -> Triple("${entry.periods.size} Stunden", R.drawable.multiple_period_background, R.color.badge_text_light)
            }

            holder.durationIndicator.text = text
            holder.durationIndicator.setBackgroundResource(backgroundRes)
            holder.durationIndicator.setTextColor(ContextCompat.getColor(context, textColor))
        } else {
            holder.durationIndicator.visibility = View.GONE
        }
    }

    /**
     * **NEUE MODERNE PERIODE-INDIKATOR FUNKTION**
     * Verwendet abgerundete, elegantere Formen für den Merge-Indikator
     */
    private fun setupModernPeriodIndicator(holder: ViewHolder, entry: MergedTimetableEntry, context: android.content.Context) {
        if (entry.isMultiplePeriod) {
            holder.periodIndicator.visibility = View.VISIBLE

            // Wähle das moderne Design basierend auf der Anzahl der Perioden
            val indicatorRes = when (entry.periods.size) {
                2 -> R.drawable.period_indicator_double_rounded  // **NEUE ABGERUNDETE VERSION**
                3 -> R.drawable.period_indicator_triple_rounded  // **NEUE ABGERUNDETE VERSION**
                else -> R.drawable.period_indicator_multiple_rounded // **NEUE ABGERUNDETE VERSION**
            }

            holder.periodIndicator.setBackgroundResource(indicatorRes)

            // Optional: Leichte Elevation für mehr Tiefe
            holder.periodIndicator.elevation = 2f

            // Optional: Sanfte Animation beim Erscheinen
            holder.periodIndicator.alpha = 0f
            holder.periodIndicator.animate()
                .alpha(1f)
                .setDuration(200)
                .start()

        } else {
            holder.periodIndicator.visibility = View.GONE
        }
    }

    private fun applyAppearanceForCancelledLessons(holder: ViewHolder, entry: MergedTimetableEntry, context: android.content.Context) {
        val isCancelled = entry.type == "Entfällt"

        if (isCancelled) {
            // Reduzierte Transparenz für ausgefallene Stunden
            listOf(
                holder.startTime, holder.endTime,
                holder.subject, holder.teacher, holder.room, holder.groupInfo
            ).forEach { it.alpha = 0.6f }

            // Graue Hintergrundfarbe
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_tint_3))
            holder.itemView.alpha = 0.8f

            // Durchgestrichener Text für das Fach
            holder.subject.paintFlags = holder.subject.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

            // **AUCH DER PERIODE-INDIKATOR WIRD GEDIMMT**
            holder.periodIndicator.alpha = 0.5f
        } else {
            // Normale Darstellung
            listOf(
                holder.startTime, holder.endTime,
                holder.subject, holder.teacher, holder.room, holder.groupInfo
            ).forEach { it.alpha = 1.0f }

            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            holder.itemView.alpha = 1.0f

            // Entferne Durchstreichung
            holder.subject.paintFlags = holder.subject.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

            // **NORMALE SICHTBARKEIT FÜR PERIODE-INDIKATOR**
            holder.periodIndicator.alpha = 1.0f
        }
    }

    override fun getItemCount() = entries.size
}