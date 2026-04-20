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

class InternshipFragment : Fragment(R.layout.fragment_internship) {

    private lateinit var rvInternships: RecyclerView
    private lateinit var progressBarInternships: ProgressBar
    private lateinit var internshipAdapter: JobOpportunityAdapter

    private val db = FirebaseFirestore.getInstance()
    private var firebaseInternships: List<Job> = emptyList()
    private var externalInternships: List<Job> = emptyList()
    private var pendingSources: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvInternships = view.findViewById(R.id.rvInternships)
        progressBarInternships = view.findViewById(R.id.progressBarInternships)

        rvInternships.layoutManager = LinearLayoutManager(requireContext())
        internshipAdapter = JobOpportunityAdapter(emptyList()) { selectedInternship ->
            val intent = Intent(requireActivity(), JobDetailActivity::class.java).apply {
                putExtra("EXTRA_JOB", selectedInternship)
            }
            startActivity(intent)
        }
        rvInternships.adapter = internshipAdapter

        fetchInternships()
    }

    private fun fetchInternships() {
        progressBarInternships.visibility = View.VISIBLE
        pendingSources = 2

        db.collection("jobs")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                val internships = mutableListOf<Job>()
                for (doc in documents) {
                    try {
                        val parsed = doc.toObject(Job::class.java)
                        if (isInternship(parsed)) {
                            internships.add(
                                parsed.copy(
                                    opportunityType = "INTERNSHIP",
                                    source = parsed.source.ifBlank { "FIREBASE" }
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("InternshipFragment", "Error parsing doc ${doc.id}", e)
                    }
                }
                firebaseInternships = internships
                onSourceLoaded()
            }
            .addOnFailureListener { e ->
                firebaseInternships = emptyList()
                onSourceLoaded()
                Toast.makeText(requireContext(), "Failed to load Firebase internships: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        viewLifecycleOwner.lifecycleScope.launch {
            externalInternships = ExternalOpportunityDataSource.fetchJobs(
                keyword = "internship",
                type = "INTERNSHIP"
            )
            onSourceLoaded()
        }
    }

    private fun isInternship(job: Job): Boolean {
        if (job.opportunityType.equals("INTERNSHIP", ignoreCase = true)) {
            return true
        }

        val text = "${job.title} ${job.description}".lowercase()
        return text.contains("intern") || text.contains("internship")
    }

    private fun onSourceLoaded() {
        pendingSources -= 1
        if (pendingSources > 0) return

        progressBarInternships.visibility = View.GONE

        val merged = (firebaseInternships + externalInternships)
            .distinctBy { it.id }

        internshipAdapter.updateData(merged)
    }
}
