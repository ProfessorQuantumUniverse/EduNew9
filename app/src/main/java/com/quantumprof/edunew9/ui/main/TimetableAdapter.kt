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
        val subjectInline: TextView = view.findViewById(R.id.tv_subject_inline)
        val subjectFull: TextView = view.findViewById(R.id.tv_subject_full)
        val teacher: TextView = view.findViewById(R.id.tv_teacher)
        val room: TextView = view.findViewById(R.id.tv_room)
        val statusIcon: ImageView = view.findViewById(R.id.iv_status)
        val groupInfo: TextView = view.findViewById(R.id.tv_group_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.timetable_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context

        // Zeiten
        holder.startTime.text = entry.startTime
        holder.endTime.text = entry.endTime

        // Periodenformatierung
        val periodText = when {
            entry.period.contains(".") -> entry.period.replace(".", ". Std")
            entry.period.isNotEmpty() -> "${entry.period}. Std"
            else -> ""
        }
        holder.period.text = periodText

        // Reset Titel Views
        holder.subjectInline.visibility = View.VISIBLE
        holder.subjectFull.visibility = View.GONE
        holder.subjectInline.text = entry.subject
        holder.subjectFull.text = entry.subject

        // Breitenmessung (ohne auf Ellipsis zu warten)
        fun applyTitleLayout() {
            val inlineWidth = holder.subjectInline.width
            if (inlineWidth > 0) {
                val available = inlineWidth - holder.subjectInline.paddingLeft - holder.subjectInline.paddingRight
                val textWidth = holder.subjectInline.paint.measureText(entry.subject)
                if (textWidth > available) {
                    holder.subjectFull.visibility = View.VISIBLE
                    holder.subjectInline.visibility = View.GONE
                } else {
                    holder.subjectFull.visibility = View.GONE
                    holder.subjectInline.visibility = View.VISIBLE
                }
            }
        }
        // Falls width noch 0 -> nach Layout erneut
        if (holder.subjectInline.width == 0) {
            holder.subjectInline.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    holder.subjectInline.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    applyTitleLayout()
                }
            })
        } else applyTitleLayout()

        // Lehrerformatierung
        val teacherText = when {
            entry.teacher.isNotEmpty() && !entry.teacher.startsWith("Herr") && !entry.teacher.startsWith("Frau") ->
                if (entry.teacher.length <= 4) entry.teacher else "Herr/Frau ${entry.teacher}"
            entry.teacher.isNotEmpty() -> entry.teacher
            else -> "Kein Lehrer"
        }
        holder.teacher.text = teacherText

        // Raum
        holder.room.text = if (entry.room.isNotEmpty()) entry.room else "?"

        // Gruppe
        val groupText = when {
            entry.detectedGroup != null -> "Gruppe ${entry.detectedGroup}"
            entry.groupNames.isNotEmpty() -> entry.groupNames.joinToString(", ")
            else -> ""
        }
        if (groupText.isNotEmpty()) {
            holder.groupInfo.visibility = View.VISIBLE
            holder.groupInfo.text = groupText
            when (entry.detectedGroup) {
                "A" -> holder.groupInfo.setBackgroundColor(ContextCompat.getColor(context, R.color.group_a_color))
                "B" -> holder.groupInfo.setBackgroundColor(ContextCompat.getColor(context, R.color.group_b_color))
                else -> holder.groupInfo.setBackgroundColor(ContextCompat.getColor(context, R.color.group_neutral_color))
            }
        } else {
            holder.groupInfo.visibility = View.GONE
        }

        // Type / Status Icon
        val isCancelled = entry.type == "Entfällt"
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

        // Alpha & Strikethrough auf beide potenziellen Titel anwenden
        if (isCancelled) {
            listOf(holder.startTime, holder.endTime, holder.period, holder.subjectInline, holder.subjectFull, holder.teacher, holder.room, holder.groupInfo)
                .forEach { it.alpha = 0.5f }
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            holder.itemView.alpha = 0.7f
            holder.subjectInline.paintFlags = holder.subjectInline.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            holder.subjectFull.paintFlags = holder.subjectFull.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            listOf(holder.startTime, holder.endTime, holder.period, holder.subjectInline, holder.subjectFull, holder.teacher, holder.room, holder.groupInfo)
                .forEach { it.alpha = 1.0f }
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            holder.itemView.alpha = 1.0f
            holder.subjectInline.paintFlags = holder.subjectInline.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.subjectFull.paintFlags = holder.subjectFull.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun getItemCount() = entries.size
}