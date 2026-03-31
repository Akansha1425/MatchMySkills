package com.example.matchmyskills

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.adapter.JobOpportunityAdapter
import com.example.matchmyskills.model.Job
import com.google.firebase.firestore.FirebaseFirestore

class JobActivity : AppCompatActivity() {

    private lateinit var rvJobs: RecyclerView
    private lateinit var progressBarJobs: ProgressBar
    private lateinit var jobOpportunityAdapter: JobOpportunityAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job)

        rvJobs = findViewById(R.id.rvJobs)
        progressBarJobs = findViewById(R.id.progressBarJobs)

        rvJobs.layoutManager = LinearLayoutManager(this)
        
        jobOpportunityAdapter = JobOpportunityAdapter(emptyList()) { selectedJob ->
            val intent = Intent(this, JobDetailActivity::class.java).apply {
                putExtra("EXTRA_JOB", selectedJob)
            }
            startActivity(intent)
        }
        rvJobs.adapter = jobOpportunityAdapter

        fetchJobs()
    }

    private fun fetchJobs() {
        progressBarJobs.visibility = View.VISIBLE
        db.collection("jobs")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                progressBarJobs.visibility = View.GONE
                val jobList = mutableListOf<Job>()
                for (doc in documents) {
                    try {
                        val job = doc.toObject(Job::class.java)
                        jobList.add(job)
                    } catch (e: Exception) {
                        Log.e("JobActivity", "Error parsing doc ${doc.id}", e)
                    }
                }
                jobOpportunityAdapter.updateData(jobList)
            }
            .addOnFailureListener { e ->
                progressBarJobs.visibility = View.GONE
                Toast.makeText(this, "Failed to load jobs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
