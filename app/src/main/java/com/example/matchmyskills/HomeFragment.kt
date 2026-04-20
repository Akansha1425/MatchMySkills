package com.example.matchmyskills

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.matchmyskills.data.remote.ExternalOpportunityDataSource
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.model.Job
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var tvJobsCount: TextView
    private lateinit var tvInternshipsCount: TextView
    private lateinit var tvHackathonsCount: TextView
    private lateinit var tvAppliedCountLabel: TextView
    private lateinit var progressApplied: ProgressBar
    private lateinit var pbLoading: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvJobsCount = view.findViewById(R.id.tv_jobs_count)
        tvInternshipsCount = view.findViewById(R.id.tv_internships_count)
        tvHackathonsCount = view.findViewById(R.id.tv_hackathons_count)
        tvAppliedCountLabel = view.findViewById(R.id.tv_applied_count_label)
        progressApplied = view.findViewById(R.id.progress_applied)
        pbLoading = view.findViewById(R.id.pb_loading)

        loadDashboardData()
    }

    private fun loadDashboardData() {
        pbLoading.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch Firebase Jobs & Internships
                val firebaseJobsTask = async {
                    try {
                        val snapshot = db.collection("jobs")
                            .whereEqualTo("status", "Active")
                            .get()
                            .await()
                        
                        val allJobs = snapshot.documents.mapNotNull { it.toObject(Job::class.java) }
                        val internships = allJobs.filter { isInternship(it) }
                        val jobs = allJobs.filter { !isInternship(it) }
                        
                        Pair(jobs.size, internships.size)
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error fetching firebase jobs", e)
                        Pair(0, 0)
                    }
                }

                // Fetch Firebase Hackathons
                val firebaseHackathonsTask = async {
                    try {
                        val snapshot = db.collection("hackathons")
                            .whereEqualTo("status", "Active")
                            .get()
                            .await()
                        snapshot.size()
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error fetching firebase hackathons", e)
                        0
                    }
                }

                // Fetch Applications matching current user
                val applicationsTask = async {
                    try {
                        val userId = auth.currentUser?.uid ?: return@async 0
                        val snapshot = db.collection("applications")
                            .whereEqualTo("candidateId", userId)
                            .get()
                            .await()
                        snapshot.size()
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error fetching applications", e)
                        0
                    }
                }

                // Fetch External Data concurrently
                val externalJobsTask = async {
                    try { ExternalOpportunityDataSource.fetchJobs("software", "JOB").size } catch(e: Exception) { 0 }
                }
                
                val externalInternshipsTask = async {
                    try { ExternalOpportunityDataSource.fetchJobs("internship", "INTERNSHIP").size } catch(e: Exception) { 0 }
                }

                val externalHackathonsTask = async {
                    try { ExternalOpportunityDataSource.fetchHackathons().size } catch(e: Exception) { 0 }
                }

                // Await all results
                val (fbJobsCount, fbInternshipsCount) = firebaseJobsTask.await()
                val fbHackathonsCount = firebaseHackathonsTask.await()
                val appliedCount = applicationsTask.await()
                
                val extJobsCount = externalJobsTask.await()
                val extInternshipsCount = externalInternshipsTask.await()
                val extHackathonsCount = externalHackathonsTask.await()

                val totalJobs = fbJobsCount + extJobsCount
                val totalInternships = fbInternshipsCount + extInternshipsCount
                val totalHackathons = fbHackathonsCount + extHackathonsCount
                val totalOpportunities = totalJobs + totalInternships + totalHackathons

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    tvJobsCount.text = totalJobs.toString()
                    tvInternshipsCount.text = totalInternships.toString()
                    tvHackathonsCount.text = totalHackathons.toString()

                    tvAppliedCountLabel.text = "$appliedCount Applications / $totalOpportunities Total Opportunities"
                    
                    if (totalOpportunities > 0) {
                        // Calculate percentage out of 100 for progress bar
                        val progress = ((appliedCount.toFloat() / totalOpportunities.toFloat()) * 100).toInt()
                        // Cap at 100 just in case
                        progressApplied.progress = progress.coerceAtMost(100)
                    } else {
                        progressApplied.progress = 0
                    }

                    pbLoading.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading dashboard data", e)
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                }
            }
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

    private fun isInternship(job: Job): Boolean {
        if (job.opportunityType.equals("INTERNSHIP", ignoreCase = true)) {
            return true
        }
        val text = "${job.title} ${job.description}".lowercase()
        return text.contains("intern") || text.contains("internship")
    }
}
