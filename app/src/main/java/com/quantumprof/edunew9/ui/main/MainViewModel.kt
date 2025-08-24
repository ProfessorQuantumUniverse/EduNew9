package com.quantumprof.edunew9.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantumprof.edunew9.data.EdupageRepository
import com.quantumprof.edunew9.data.SubstitutionEntry
import com.quantumprof.edunew9.data.TimetableEntry
import kotlinx.coroutines.launch
import java.util.Date

class MainViewModel : ViewModel() {
    // **VERWENDE SINGLETON (ohne Context, da bereits initialisiert)**
    private val repository = EdupageRepository.getInstance()

    private val _substitutionPlan = MutableLiveData<Result<List<SubstitutionEntry>>>()
    val substitutionPlan: LiveData<Result<List<SubstitutionEntry>>> = _substitutionPlan

    private val _timetable = MutableLiveData<Result<List<TimetableEntry>>>()
    val timetable: LiveData<Result<List<TimetableEntry>>> = _timetable

    private var isSubstitutionLoading = false
    private var isTimetableLoading = false

    fun loadSubstitutionPlan() {
        if (isSubstitutionLoading) return
        isSubstitutionLoading = true
        viewModelScope.launch {
            try {
                val result = repository.getSubstitutionPlanForDate(Date())
                _substitutionPlan.postValue(result)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Fehler beim Laden des Vertretungsplans", e)
                _substitutionPlan.postValue(Result.failure(e))
            } finally {
                isSubstitutionLoading = false
            }
        }
    }

    fun loadTimetable() {
        if (isTimetableLoading) return
        isTimetableLoading = true
        Log.d("MainViewModel", "=== STUNDENPLAN WIRD GELADEN ===")
        viewModelScope.launch {
            try {
                val result = repository.getTimetableForDate(Date())
                Log.d("MainViewModel", "Stundenplan-Ergebnis: ${result.isSuccess}")
                _timetable.postValue(result)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Fehler beim Laden des Stundenplans", e)
                _timetable.postValue(Result.failure(e))
            } finally {
                isTimetableLoading = false
            }
        }
    }
}