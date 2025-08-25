package com.quantumprof.edunew9.ui.settings

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.quantumprof.edunew9.R
import com.quantumprof.edunew9.data.SettingsManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup Material Toolbar
        setupToolbar()

        // Setup back button handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        setupGroupSelection()
    }

    private fun setupToolbar() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupGroupSelection() {
        val groupDropdownLayout: TextInputLayout = findViewById(R.id.group_dropdown_layout)
        val groupDropdown: AutoCompleteTextView = findViewById(R.id.group_dropdown)
        val autoFilterSwitch: MaterialSwitch = findViewById(R.id.switch_auto_filter)

        // Dropdown Setup mit Material Design 3
        val groupOptions = arrayOf("Alle Gruppen", "Gruppe A", "Gruppe B")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, groupOptions)
        groupDropdown.setAdapter(adapter)

        // Aktuelle Einstellungen laden
        val currentGroup = SettingsManager.getSelectedGroup(this)
        val currentText = when (currentGroup) {
            "A" -> "Gruppe A"
            "B" -> "Gruppe B"
            else -> "Alle Gruppen"
        }
        groupDropdown.setText(currentText, false)
        autoFilterSwitch.isChecked = SettingsManager.getAutoFilter(this)

        // Listener für Gruppenauswahl
        groupDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedGroup = when (position) {
                1 -> "A"
                2 -> "B"
                else -> null
            }
            SettingsManager.saveSelectedGroup(this@SettingsActivity, selectedGroup)
            Log.d("SettingsActivity", "Neue Gruppe gespeichert: $selectedGroup")
        }

        autoFilterSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveAutoFilter(this, isChecked)
            Log.d("SettingsActivity", "Auto-Filter gespeichert: $isChecked")
        }

        // Setup für Wahlkursfilterung
        setupElectiveSelection()
    }

    private fun setupElectiveSelection() {
        val electiveDropdownLayout: TextInputLayout = findViewById(R.id.elective_dropdown_layout)
        val electiveDropdown: AutoCompleteTextView = findViewById(R.id.elective_dropdown)
        val autoFilterElectivesSwitch: MaterialSwitch = findViewById(R.id.switch_auto_filter_electives)

        // Dropdown Setup für Wahlkurse
        val electiveOptions = arrayOf("Alle Wahlkurse") + SettingsManager.getAvailableElectives()
        val electiveAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, electiveOptions)
        electiveDropdown.setAdapter(electiveAdapter)

        // Aktuelle Wahlkurs-Einstellungen laden
        val currentElective = SettingsManager.getSelectedElective(this)
        val currentElectiveText = currentElective ?: "Alle Wahlkurse"
        electiveDropdown.setText(currentElectiveText, false)
        autoFilterElectivesSwitch.isChecked = SettingsManager.getAutoFilterElectives(this)

        // Wahlkurs Dropdown Listener
        electiveDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedElective = if (position == 0) {
                null // "Alle Wahlkurse"
            } else {
                SettingsManager.getAvailableElectives()[position - 1]
            }
            SettingsManager.saveSelectedElective(this@SettingsActivity, selectedElective)
            Log.d("SettingsActivity", "Neuer Wahlkurs gespeichert: $selectedElective")
        }

        autoFilterElectivesSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveAutoFilterElectives(this, isChecked)
            Log.d("SettingsActivity", "Auto-Filter Wahlkurse gespeichert: $isChecked")
        }
    }
}