package com.quantumprof.edunew9.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.quantumprof.edunew9.R
import com.quantumprof.edunew9.data.SubstitutionEntry

class SubstitutionAdapter(private var entries: List<SubstitutionEntry>) :
    RecyclerView.Adapter<SubstitutionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chipType: Chip = view.findViewById(R.id.chip_substitution_type)
        val period: TextView = view.findViewById(R.id.tv_substitution_period)
        val info: TextView = view.findViewById(R.id.tv_substitution_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.substitution_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context

        // Setze den Typ-Chip mit dynamischen Farben
        holder.chipType.text = entry.type
        when (entry.type) {
            "Entfällt" -> {
                holder.chipType.setChipBackgroundColorResource(R.color.substitution_cancelled)
                holder.chipType.setTextColor(ContextCompat.getColor(context, R.color.substitution_cancelled_text))
            }
            "Änderung" -> {
                holder.chipType.setChipBackgroundColorResource(R.color.substitution_changed)
                holder.chipType.setTextColor(ContextCompat.getColor(context, R.color.substitution_changed_text))
            }
            "Event" -> {
                holder.chipType.setChipBackgroundColorResource(R.color.substitution_event)
                holder.chipType.setTextColor(ContextCompat.getColor(context, R.color.substitution_event_text))
            }
            else -> {
                holder.chipType.setChipBackgroundColorResource(R.color.md_theme_light_primaryContainer)
                holder.chipType.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_onPrimaryContainer))
            }
        }

        // Formatiere die Stunden-Anzeige schöner
        val periodText = when {
            entry.period.contains("-") -> entry.period.replace("-", " - ")
            entry.period.contains(".") && !entry.period.contains("Stunde") -> "${entry.period} Stunde"
            else -> entry.period
        }
        holder.period.text = periodText

        // Setze die Info mit besserer Formatierung
        holder.info.text = entry.info
    }

    override fun getItemCount() = entries.size

    fun updateEntries(newEntries: List<SubstitutionEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}