package com.quantumprof.edunew9.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.quantumprof.edunew9.data.EdupageRepository
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    // **VERWENDE SINGLETON**
    private val repository = EdupageRepository.getInstance(application.applicationContext)

    private val _loginResult = MutableLiveData<Result<Boolean>>()
    val loginResult: LiveData<Result<Boolean>> = _loginResult

    fun login(user: String, pass: String) {
        viewModelScope.launch {
            val result = repository.login(user, pass)
            _loginResult.postValue(result)
        }
    }
}