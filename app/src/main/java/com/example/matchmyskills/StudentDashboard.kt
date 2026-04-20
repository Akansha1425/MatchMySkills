package com.example.matchmyskills

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StudentDashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_jobs -> JobFragment()
                R.id.nav_internships -> InternshipFragment()
                R.id.nav_hackathons -> HackathonFragment()
                R.id.nav_student_profile -> StudentProfileFragment()
                else -> null
            }
            if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
                true
            } else {
                false
            }
        }
    }
}
