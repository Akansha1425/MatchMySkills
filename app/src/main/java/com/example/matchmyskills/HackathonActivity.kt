package com.example.matchmyskills

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.adapter.HackathonAdapter
import com.example.matchmyskills.data.remote.ExternalOpportunityDataSource
import com.example.matchmyskills.model.Hackathon
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class HackathonActivity : AppCompatActivity() {

    private lateinit var rvHackathons: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var hackathonAdapter: HackathonAdapter
    private val db = FirebaseFirestore.getInstance()
    private var firebaseHackathons: List<Hackathon> = emptyList()
    private var externalHackathons: List<Hackathon> = emptyList()
    private var pendingSources: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hackathon)

        rvHackathons = findViewById(R.id.rvHackathons)
        progressBar = findViewById(R.id.progressBar)

        rvHackathons.layoutManager = LinearLayoutManager(this)
        
        hackathonAdapter = HackathonAdapter(emptyList()) { selectedHackathon ->
            val intent = Intent(this, OpportunityDetailActivity::class.java).apply {
                putExtra("EXTRA_HACKATHON", selectedHackathon)
            }
            startActivity(intent)
        }
        rvHackathons.adapter = hackathonAdapter

        fetchHackathons()
    }

    private fun fetchHackathons() {
        progressBar.visibility = View.VISIBLE
        pendingSources = 2

        db.collection("hackathons")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                val hackathonList = mutableListOf<Hackathon>()
                for (doc in documents) {
                    try {
                        val parsed = doc.toObject(Hackathon::class.java)
                        val hackathon = parsed.copy(
                            opportunityType = parsed.opportunityType.ifBlank { "HACKATHON" },
                            source = parsed.source.ifBlank { "FIREBASE" }
                        )
                        hackathonList.add(hackathon)
                    } catch (e: Exception) {
                        Log.e("HackathonActivity", "Error parsing doc ${doc.id}", e)
                    }
                }
                firebaseHackathons = hackathonList
                onSourceLoaded()
            }
            .addOnFailureListener { e ->
                firebaseHackathons = emptyList()
                onSourceLoaded()
                Toast.makeText(this, "Failed to load Firebase hackathons: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        lifecycleScope.launch {
            externalHackathons = ExternalOpportunityDataSource.fetchHackathons()
            onSourceLoaded()
        }
    }

    private fun onSourceLoaded() {
        pendingSources -= 1
        if (pendingSources > 0) return

        progressBar.visibility = View.GONE
        val merged = (firebaseHackathons + externalHackathons)
            .distinctBy { it.id }

        hackathonAdapter.updateData(merged)
    }
}
