package com.quantumprof.edunew9.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.quantumprof.edunew9.R
import com.quantumprof.edunew9.data.EdupageRepository
import com.quantumprof.edunew9.databinding.ActivityMainBinding
import com.quantumprof.edunew9.ui.login.LoginActivity
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import com.quantumprof.edunew9.ui.main.MainViewModel
import com.quantumprof.edunew9.ui.main.SubstitutionFragment // Musst du noch erstellen
import com.quantumprof.edunew9.ui.main.TimetableFragment // Musst du noch erstellen
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var repository: EdupageRepository

    private val substitutionFragment = SubstitutionFragment()
    private val timetableFragment = TimetableFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Standard-Fragment setzen
        setCurrentFragment(substitutionFragment)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_substitution -> setCurrentFragment(substitutionFragment)
                R.id.navigation_timetable -> setCurrentFragment(timetableFragment)
            }
            true
        }
    }


    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            commit()
        }
    }
}