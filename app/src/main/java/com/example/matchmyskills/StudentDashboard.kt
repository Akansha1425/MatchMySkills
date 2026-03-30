package com.example.matchmyskills

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StudentDashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        // Check if the activity is newly created (savedInstanceState == null)
        // If it is, load the HomeFragment.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }
}
