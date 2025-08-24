package com.quantumprof.edunew9.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantumprof.edunew9.data.EdupageRepository
import com.quantumprof.edunew9.data.SubstitutionEntry
import com.quantumprof.edunew9.data.TimetableEntry // Importieren
import kotlinx.coroutines.launch
import java.util.Date

class MainViewModel : ViewModel() {
    private val repository = EdupageRepository()

    // Vertretungsplan
    private val _substitutionPlan = MutableLiveData<Result<List<SubstitutionEntry>>>()
    val substitutionPlan: LiveData<Result<List<SubstitutionEntry>>> = _substitutionPlan

    // Stundenplan (NEU)
    private val _timetable = MutableLiveData<Result<List<TimetableEntry>>>()
    val timetable: LiveData<Result<List<TimetableEntry>>> = _timetable

    private var isSubstitutionLoading = false
    private var isTimetableLoading = false

    fun loadSubstitutionPlan() {
        // Nur laden, wenn nicht schon ein Ladevorgang läuft
        if (isSubstitutionLoading) return
        isSubstitutionLoading = true
        viewModelScope.launch {
            val result = repository.getSubstitutionPlanForDate(Date())
            _substitutionPlan.postValue(result)
            isSubstitutionLoading = false // Ladevorgang beendet
        }
    }

    fun loadTimetable() {
        // Nur laden, wenn nicht schon ein Ladevorgang läuft
        if (isTimetableLoading) return
        isTimetableLoading = true
        viewModelScope.launch {
            val result = repository.getTimetableForDate(Date())
            _timetable.postValue(result)
            isTimetableLoading = false // Ladevorgang beendet
        }
    }
}