package com.quantumprof.edunew9.data

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "edupage_settings"
    private const val KEY_SELECTED_GROUP = "selected_group"
    private const val KEY_AUTO_FILTER = "auto_filter"
    private const val KEY_SELECTED_ELECTIVE = "selected_elective"
    private const val KEY_AUTO_FILTER_ELECTIVES = "auto_filter_electives"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Gruppen-Einstellungen
    fun saveSelectedGroup(context: Context, group: String?) {
        getPrefs(context).edit()
            .putString(KEY_SELECTED_GROUP, group)
            .apply()
    }

    fun getSelectedGroup(context: Context): String? {
        return getPrefs(context).getString(KEY_SELECTED_GROUP, null)
    }

    fun saveAutoFilter(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_AUTO_FILTER, enabled)
            .apply()
    }

    fun getAutoFilter(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_FILTER, true)
    }

    // Wahlkurs-Einstellungen
    fun saveSelectedElective(context: Context, elective: String?) {
        getPrefs(context).edit()
            .putString(KEY_SELECTED_ELECTIVE, elective)
            .apply()
    }

    fun getSelectedElective(context: Context): String? {
        return getPrefs(context).getString(KEY_SELECTED_ELECTIVE, null)
    }

    fun saveAutoFilterElectives(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_AUTO_FILTER_ELECTIVES, enabled)
            .apply()
    }

    fun getAutoFilterElectives(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_FILTER_ELECTIVES, false)
    }

    // Verfügbare Wahlkurse
    fun getAvailableElectives(): List<String> {
        return listOf("Orchester", "Kunstwerkstatt", "Oberstufenchor")
    }
}