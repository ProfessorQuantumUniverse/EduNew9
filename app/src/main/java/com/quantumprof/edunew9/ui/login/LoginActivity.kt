package com.quantumprof.edunew9.ui.login


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.quantumprof.edunew9.ui.main.MainActivity
import com.quantumprof.edunew9.R

class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameEditText: EditText = findViewById(R.id.username)
        val passwordEditText: EditText = findViewById(R.id.password)
        val loginButton: Button = findViewById(R.id.loginButton)
        val loadingProgressBar: ProgressBar = findViewById(R.id.loading)

        // --- HIER KOMMT DIE ANPASSUNG ---
        loginViewModel.loginResult.observe(this) { result ->
            loadingProgressBar.visibility = View.GONE
            loginButton.isEnabled = true

            result.fold(
                onSuccess = {
                    // Der Login war erfolgreich! Wir müssen hier nichts mehr prüfen.
                    Log.d("LoginActivity", "Login erfolgreich, starte MainActivity.")
                    Toast.makeText(this, "Login erfolgreich!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Schließe die LoginActivity, damit man nicht zurück kann.
                },
                onFailure = { error ->
                    // Der Login ist fehlgeschlagen. Zeige die Fehlermeldung aus dem Repository.
                    Log.e("LoginActivity", "Login fehlgeschlagen: ${error.message}")
                    Toast.makeText(this, "Login fehlgeschlagen: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isNotBlank() && password.isNotBlank()) {
                loadingProgressBar.visibility = View.VISIBLE
                loginButton.isEnabled = false
                loginViewModel.login(username, password)
            } else {
                Toast.makeText(this, "Bitte Benutzernamen und Passwort eingeben.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}