package com.example.matchmyskills

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.matchmyskills.repository.AuthRepository
import com.example.matchmyskills.background.OpportunitySyncScheduler
import com.example.matchmyskills.AdminDashboard
import com.example.matchmyskills.StudentDashboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private var isNavigated = false // Guard flag

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Start session check immediately (performing fetch while splash screen is visible)
        checkSession()
    }

    private fun checkSession() {
        // FirebaseAuth.getInstance().signOut() // Force logout for testing

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToDestination(LoginActivity::class.java)
        } else {
            // Show loading during Firestore fetch
            val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
            progressBar.visibility = View.VISIBLE
            fetchUserDocAndNavigate(currentUser.uid)
        }
    }

    private fun fetchUserDocAndNavigate(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                // Ensure document exists before accessing fields
                if (document != null && document.exists()) {
                    // Safe role handling with default value
                    val role = document.getString("role") ?: "student"
                    authRepository.saveUserRole(role)

                    if (role == "recruiter") {
                        val companyName = document.getString("companyName")
                        if (companyName.isNullOrEmpty()) {
                            navigateToDestination(OnboardingActivity::class.java)
                        } else {
                            navigateToDestination(MainActivity::class.java)
                        }
                    } else if (role == "student") {
                        OpportunitySyncScheduler.scheduleLoginDeadlineCheck(this)
                        navigateToDestination(StudentDashboard::class.java)
                    } else if (role == "admin") {
                        navigateToDestination(AdminDashboard::class.java)
                    }
                } else {
                    // Document missing or null, redirect to Login
                    navigateToDestination(LoginActivity::class.java)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_ERROR", "Splash Firestore fetch failed: ${e.message}")
                // On error, redirect safely to LoginActivity
                navigateToDestination(LoginActivity::class.java)
            }
    }

    private fun <T> navigateToDestination(destination: Class<T>) {
        if (isNavigated) return
        isNavigated = true

        val intent = Intent(this, destination).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}