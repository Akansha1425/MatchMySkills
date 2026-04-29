package com.example.matchmyskills

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import com.example.matchmyskills.adapter.JobOpportunityAdapter
import com.example.matchmyskills.data.remote.ExternalOpportunityDataSource
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.util.toJob
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class InternshipFragment : Fragment(R.layout.fragment_internship) {

    private lateinit var rvInternships: RecyclerView
    private lateinit var progressBarInternships: ProgressBar
    private lateinit var internshipAdapter: JobOpportunityAdapter

    private val db = FirebaseFirestore.getInstance()
    private var firebaseInternships: List<Job> = emptyList()
    private var externalInternships: List<Job> = emptyList()
    private var allMergedInternships: List<Job> = emptyList()
    private var candidateSkills: List<String> = emptyList()
    private var pendingSources: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvInternships = view.findViewById(R.id.rvInternships)
        progressBarInternships = view.findViewById(R.id.progressBarInternships)
        val etSearch = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)

        rvInternships.layoutManager = LinearLayoutManager(requireContext())
        internshipAdapter = JobOpportunityAdapter(emptyList()) { selectedInternship ->
            val intent = Intent(requireActivity(), JobDetailActivity::class.java).apply {
                putExtra("EXTRA_JOB", selectedInternship)
            }
            startActivity(intent)
        }
        rvInternships.adapter = internshipAdapter

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterInternships(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        fetchCandidateSkills()
        fetchInternships()
    }

    private fun fetchCandidateSkills() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                candidateSkills = doc.get("skills") as? List<String> ?: emptyList()
                if (allMergedInternships.isNotEmpty()) {
                    sortAndDisplayInternships()
                }
            }
    }

    private fun fetchInternships() {
        progressBarInternships.visibility = View.VISIBLE
        pendingSources = 2

        db.collection("jobs")
            .whereEqualTo("status", "Active")
            .whereEqualTo("opportunityType", "INTERNSHIP")
            .get()
            .addOnSuccessListener { documents ->
                val internshipsList = mutableListOf<Job>()
                for (doc in documents) {
                    try {
                        val parsed = doc.toJob()
                        if (parsed != null) {
                            internshipsList.add(parsed)
                        }
                    } catch (e: Exception) {
                        Log.e("InternshipFragment", "Error parsing doc ${doc.id}", e)
                    }
                }
                firebaseInternships = internshipsList
                onSourceLoaded()
            }
            .addOnFailureListener { e ->
                firebaseInternships = emptyList()
                onSourceLoaded()
                Toast.makeText(requireContext(), "Failed to load Firebase internships: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                externalInternships = ExternalOpportunityDataSource.fetchJobs(
                    keyword = "internship",
                    type = "INTERNSHIP"
                )
            } catch (e: Exception) {
                Log.e("InternshipFragment", "External fetch failed", e)
                externalInternships = emptyList()
            } finally {
                onSourceLoaded()
            }
        }
    }

    private fun onSourceLoaded() {
        pendingSources -= 1
        if (pendingSources > 0) return

        progressBarInternships.visibility = View.GONE

        allMergedInternships = (firebaseInternships + externalInternships)
            .distinctBy { it.id }

        sortAndDisplayInternships()
    }

    private fun sortAndDisplayInternships() {
        val displayList = if (candidateSkills.isNotEmpty()) {
            allMergedInternships.sortedByDescending { internship ->
                com.example.matchmyskills.util.MatchingEngine.calculateMatchScore(candidateSkills, internship).matchScore
            }
        } else {
            allMergedInternships
        }
        internshipAdapter.updateData(displayList, candidateSkills)
    }

    private fun filterInternships(query: String) {
        if (query.isEmpty()) {
            sortAndDisplayInternships()
            return
        }

        val filtered = allMergedInternships.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.companyName.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true)
        }
        
        val displayList = if (candidateSkills.isNotEmpty()) {
            filtered.sortedByDescending { internship ->
                com.example.matchmyskills.util.MatchingEngine.calculateMatchScore(candidateSkills, internship).matchScore
            }
        } else {
            filtered
        }
        internshipAdapter.updateData(displayList, candidateSkills)
    }
}
