package com.quantumprof.edunew9.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quantumprof.edunew9.R
import com.quantumprof.edunew9.data.TimetableEntry

class TimetableAdapter(private val entries: List<TimetableEntry>) :
    RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val time: TextView = view.findViewById(R.id.tv_time)
        val subject: TextView = view.findViewById(R.id.tv_subject)
        val teacherRoom: TextView = view.findViewById(R.id.tv_teacher_room)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.timetable_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.time.text = "${entry.startTime}\n${entry.endTime}"
        holder.subject.text = entry.subject
        val teacherAndRoom = listOf(entry.teacher, entry.room).filter { it.isNotEmpty() }.joinToString(" • ")
        holder.teacherRoom.text = teacherAndRoom
    }

    override fun getItemCount() = entries.size
}