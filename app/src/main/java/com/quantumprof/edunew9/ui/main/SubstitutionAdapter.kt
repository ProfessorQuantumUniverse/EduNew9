package com.quantumprof.edunew9.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quantumprof.edunew9.R
import com.quantumprof.edunew9.data.SubstitutionEntry

class SubstitutionAdapter(private var entries: List<SubstitutionEntry>) :
    RecyclerView.Adapter<SubstitutionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val type: TextView = view.findViewById(R.id.tv_substitution_type)
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
        holder.type.text = entry.type
        holder.period.text = "Stunde: ${entry.period}"
        holder.info.text = entry.info
    }

    override fun getItemCount() = entries.size
}