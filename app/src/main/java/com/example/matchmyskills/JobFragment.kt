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
import com.example.matchmyskills.adapter.JobOpportunityAdapter
import com.example.matchmyskills.data.remote.ExternalOpportunityDataSource
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.util.toJob
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class JobFragment : Fragment(R.layout.fragment_job) {

    private lateinit var rvJobs: RecyclerView
    private lateinit var progressBarJobs: ProgressBar
    private lateinit var jobOpportunityAdapter: JobOpportunityAdapter
    private val db = FirebaseFirestore.getInstance()
    private var firebaseJobs: List<Job> = emptyList()
    private var externalJobs: List<Job> = emptyList()
    private var allMergedJobs: List<Job> = emptyList()
    private var pendingSources: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvJobs = view.findViewById(R.id.rvJobs)
        progressBarJobs = view.findViewById(R.id.progressBarJobs)
        val etSearch = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)

        rvJobs.layoutManager = LinearLayoutManager(requireContext())
        
        jobOpportunityAdapter = JobOpportunityAdapter(emptyList()) { selectedJob ->
            val intent = Intent(requireActivity(), JobDetailActivity::class.java).apply {
                putExtra("EXTRA_JOB", selectedJob)
            }
            startActivity(intent)
        }
        rvJobs.adapter = jobOpportunityAdapter

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterJobs(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        fetchJobs()
    }

    private fun fetchJobs() {
        progressBarJobs.visibility = View.VISIBLE
        pendingSources = 2

        // Fetching without strict status/type to ensure manual/legacy jobs are visible
        db.collection("jobs")
            .get()
            .addOnSuccessListener { documents ->
                val jobList = mutableListOf<Job>()
                for (doc in documents) {
                    try {
                        val parsed = doc.toJob()
                        // Local filter: Must be Active (default) and NOT internship
                        if (parsed != null && parsed.status == "Active" && 
                            !parsed.opportunityType.equals("INTERNSHIP", ignoreCase = true)) {
                            jobList.add(parsed)
                        }
                    } catch (e: Exception) {
                        Log.e("JobFragment", "Error parsing doc ${doc.id}", e)
                    }
                }
                Log.d("JobFragment", "Loaded ${jobList.size} Firestore jobs")
                firebaseJobs = jobList
                onSourceLoaded()
            }
            .addOnFailureListener { e ->
                Log.e("JobFragment", "Failed to load Firebase jobs", e)
                firebaseJobs = emptyList()
                onSourceLoaded()
            }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                externalJobs = ExternalOpportunityDataSource.fetchJobs(
                    keyword = "software",
                    type = "JOB"
                )
            } catch (e: Exception) {
                externalJobs = emptyList()
            } finally {
                onSourceLoaded()
            }
        }
    }

    private fun onSourceLoaded() {
        pendingSources -= 1
        if (pendingSources > 0) return

        progressBarJobs.visibility = View.GONE

        allMergedJobs = (firebaseJobs + externalJobs)
            .distinctBy { it.id }

        jobOpportunityAdapter.updateData(allMergedJobs)
    }

    private fun filterJobs(query: String) {
        if (query.isEmpty()) {
            jobOpportunityAdapter.updateData(allMergedJobs)
            return
        }

        val filtered = allMergedJobs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.companyName.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true)
        }
        jobOpportunityAdapter.updateData(filtered)
    }
}
