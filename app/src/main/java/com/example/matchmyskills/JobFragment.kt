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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class JobFragment : Fragment(R.layout.fragment_job) {

    private lateinit var rvJobs: RecyclerView
    private lateinit var progressBarJobs: ProgressBar
    private lateinit var jobOpportunityAdapter: JobOpportunityAdapter
    private val db = FirebaseFirestore.getInstance()
    private var firebaseJobs: List<Job> = emptyList()
    private var externalJobs: List<Job> = emptyList()
    private var pendingSources: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvJobs = view.findViewById(R.id.rvJobs)
        progressBarJobs = view.findViewById(R.id.progressBarJobs)

        rvJobs.layoutManager = LinearLayoutManager(requireContext())
        
        jobOpportunityAdapter = JobOpportunityAdapter(emptyList()) { selectedJob ->
            val intent = Intent(requireActivity(), JobDetailActivity::class.java).apply {
                putExtra("EXTRA_JOB", selectedJob)
            }
            startActivity(intent)
        }
        rvJobs.adapter = jobOpportunityAdapter

        fetchJobs()
    }

    private fun fetchJobs() {
        progressBarJobs.visibility = View.VISIBLE
        pendingSources = 2

        db.collection("jobs")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                val jobList = mutableListOf<Job>()
                for (doc in documents) {
                    try {
                        val parsed = doc.toObject(Job::class.java)
                        val job = parsed.copy(
                            opportunityType = parsed.opportunityType.ifBlank { "JOB" },
                            source = parsed.source.ifBlank { "FIREBASE" }
                        )
                        jobList.add(job)
                    } catch (e: Exception) {
                        Log.e("JobFragment", "Error parsing doc ${doc.id}", e)
                    }
                }
                firebaseJobs = jobList
                onSourceLoaded()
            }
            .addOnFailureListener { e ->
                firebaseJobs = emptyList()
                onSourceLoaded()
                Toast.makeText(requireContext(), "Failed to load Firebase jobs: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        viewLifecycleOwner.lifecycleScope.launch {
            externalJobs = ExternalOpportunityDataSource.fetchJobs(
                keyword = "software",
                type = "JOB"
            )
            onSourceLoaded()
        }
    }

    private fun onSourceLoaded() {
        pendingSources -= 1
        if (pendingSources > 0) return

        progressBarJobs.visibility = View.GONE

        val merged = (firebaseJobs + externalJobs)
            .distinctBy { it.id }

        jobOpportunityAdapter.updateData(merged)
    }
}
