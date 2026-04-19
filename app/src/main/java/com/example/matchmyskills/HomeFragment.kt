package com.example.matchmyskills

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

// Pass the layout resource ID directly into the Fragment constructor
class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find the buttons by their IDs from our XML
        val btnInternships = view.findViewById<Button>(R.id.btn_internships)
        val btnHackathons = view.findViewById<Button>(R.id.btn_hackathons)

        // 2. Set Click Listener for Internships Button
        btnInternships.setOnClickListener {
            // Intent to travel from the current Activity (requireActivity) to the InternshipActivity
            val intent = Intent(requireActivity(), InternshipActivity::class.java)
            startActivity(intent)
        }

        // 3. Set Click Listener for Hackathons Button
        btnHackathons.setOnClickListener {
            val intent = Intent(requireActivity(), HackathonActivity::class.java)
            startActivity(intent)
        }
        
        // 4. Set Click Listener for Jobs Button
        val btnJobs = view.findViewById<Button>(R.id.btn_jobs)
        btnJobs.setOnClickListener {
            val intent = Intent(requireActivity(), JobActivity::class.java)
            startActivity(intent)
        }

        // 5. Set Click Listener for Profile Button
        val btnProfile = view.findViewById<Button>(R.id.btn_profile)
        btnProfile.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StudentProfileFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
