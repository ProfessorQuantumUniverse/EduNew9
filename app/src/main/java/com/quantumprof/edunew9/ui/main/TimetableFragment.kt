package com.quantumprof.edunew9.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quantumprof.edunew9.R

class TimetableFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timetable, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_timetable)
        progressBar = view.findViewById(R.id.progress_bar_timetable)
        recyclerView.layoutManager = LinearLayoutManager(context)

        observeViewModel()

        // Lade die Daten, wenn das Fragment erstellt wird
        mainViewModel.loadTimetable()
    }

    private fun observeViewModel() {
        mainViewModel.timetable.observe(viewLifecycleOwner) { result ->
            progressBar.visibility = View.VISIBLE
            result.fold(
                onSuccess = { entries ->
                    progressBar.visibility = View.GONE
                    recyclerView.adapter = TimetableAdapter(entries)
                },
                onFailure = { error ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Fehler beim Laden des Stundenplans: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}