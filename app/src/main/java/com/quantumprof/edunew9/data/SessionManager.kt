package com.quantumprof.edunew9.data

// Ein einfacher Singleton zum Speichern von Session-Daten im Arbeitsspeicher.
// Für eine persistente Anmeldung müsste man dies mit DataStore erweitern.
object SessionManager {
    var gsechash: String? = null
}