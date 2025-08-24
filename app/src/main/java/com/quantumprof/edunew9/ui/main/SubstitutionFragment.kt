package com.quantumprof.edunew9.ui.main


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quantumprof.edunew9.R
import java.text.SimpleDateFormat
import java.util.*

class SubstitutionFragment : Fragment() {

    // Den ViewModel von der Activity holen (shared ViewModel)
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var dateHeader: TextView

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
        emptyStateLayout = view.findViewById(R.id.layout_empty_state)
        dateHeader = view.findViewById(R.id.tv_date_header)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // Setze das aktuelle Datum im Header
        updateDateHeader()

        observeViewModel()

        // Lade die Daten, wenn das Fragment erstellt wird
        mainViewModel.loadSubstitutionPlan()
    }

    private fun updateDateHeader() {
        val today = Date()
        val formatter = SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMAN)
        dateHeader.text = formatter.format(today)
    }

    private fun observeViewModel() {
        mainViewModel.substitutionPlan.observe(viewLifecycleOwner) { result ->
            progressBar.visibility = View.GONE
            result.fold(
                onSuccess = { entries ->
                    if (entries.isEmpty()) {
                        showEmptyState()
                        Toast.makeText(context, "Keine Vertretungen für heute gefunden.", Toast.LENGTH_SHORT).show()
                    } else {
                        showContent(entries)
                    }
                },
                onFailure = { error ->
                    showEmptyState()
                    Toast.makeText(context, "Fehler beim Laden des Vertretungsplans: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
    }

    private fun showContent(entries: List<com.quantumprof.edunew9.data.SubstitutionEntry>) {
        emptyStateLayout.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.adapter = SubstitutionAdapter(entries)
    }
}