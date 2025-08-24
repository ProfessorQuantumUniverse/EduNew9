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

class SubstitutionFragment : Fragment() {

    // Den ViewModel von der Activity holen (shared ViewModel)
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Das Layout für dieses Fragment inflaten
        return inflater.inflate(R.layout.fragment_substitution, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_substitutions)
        progressBar = view.findViewById(R.id.progress_bar_substitution)
        recyclerView.layoutManager = LinearLayoutManager(context)

        observeViewModel()

        // Lade die Daten, wenn das Fragment erstellt wird
        mainViewModel.loadSubstitutionPlan()
    }

    private fun observeViewModel() {
        mainViewModel.substitutionPlan.observe(viewLifecycleOwner) { result ->
            progressBar.visibility = View.VISIBLE
            result.fold(
                onSuccess = { entries ->
                    progressBar.visibility = View.GONE
                    if (entries.isEmpty()) {
                        Toast.makeText(context, "Keine Vertretungen für heute gefunden.", Toast.LENGTH_SHORT).show()
                    }
                    recyclerView.adapter = SubstitutionAdapter(entries)
                },
                onFailure = { error ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Fehler beim Laden des Vertretungsplans: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}