package com.quantumprof.edunew9.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.quantumprof.edunew9.data.EdupageRepository
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EdupageRepository(application.applicationContext)

    private val _loginResult = MutableLiveData<Result<Boolean>>()
    val loginResult: LiveData<Result<Boolean>> = _loginResult

    fun login(user: String, pass: String) {
        viewModelScope.launch {
            // Die komplexe Logik ist jetzt im Repository, der Aufruf bleibt einfach.
            val result = repository.login(user, pass)
            _loginResult.postValue(result)
        }
    }
}