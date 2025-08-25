package com.quantumprof.edunew9.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.quantumprof.edunew9.R
import com.quantumprof.edunew9.ui.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import com.quantumprof.edunew9.ui.main.SubstitutionFragment
import com.quantumprof.edunew9.ui.main.TimetableFragment

class MainActivity : AppCompatActivity() {

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
                R.id.navigation_substitution -> {
                    setCurrentFragment(substitutionFragment)
                    true
                }
                R.id.navigation_timetable -> {
                    setCurrentFragment(timetableFragment)
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false // Return false to prevent highlighting this item
                }
                else -> false
            }
        }
    }


    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            commit()
        }
    }
}