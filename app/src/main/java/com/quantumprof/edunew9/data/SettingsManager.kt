package com.quantumprof.edunew9.data

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "edu_settings"
    private const val KEY_SELECTED_GROUP = "selected_group"
    private const val KEY_AUTO_FILTER = "auto_filter_groups"
    private const val KEY_SELECTED_ELECTIVE = "selected_elective"  // **NEU**
    private const val KEY_AUTO_FILTER_ELECTIVES = "auto_filter_electives"  // **NEU**

    // **WAHLKURSE KONSTANTEN**
    const val ELECTIVE_ORCHESTER = "Orchester"
    const val ELECTIVE_KUNSTWERKSTATT = "Kunstwerkstatt"
    const val ELECTIVE_OBERSTUFENCHOR = "OberstufenChor"

    fun saveSelectedGroup(context: Context, group: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_GROUP, group).apply()
    }
    
    fun getSelectedGroup(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_GROUP, null)
    }
    
    fun saveAutoFilter(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_FILTER, enabled).apply()
    }
    
    fun getAutoFilter(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_FILTER, true)
    }

    // **NEUE WAHLKURS-FILTERUNG**
    fun saveSelectedElective(context: Context, elective: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_ELECTIVE, elective).apply()
    }

    fun getSelectedElective(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_ELECTIVE, null)
    }

    fun saveAutoFilterElectives(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_FILTER_ELECTIVES, enabled).apply()
    }

    fun getAutoFilterElectives(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_FILTER_ELECTIVES, true)
    }

    // **HILFSMETHODE FÜR VERFÜGBARE WAHLKURSE**
    fun getAvailableElectives(): List<String> {
        return listOf(ELECTIVE_ORCHESTER, ELECTIVE_KUNSTWERKSTATT, ELECTIVE_OBERSTUFENCHOR)
    }
}