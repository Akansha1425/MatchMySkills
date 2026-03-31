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
import com.example.matchmyskills.adapter.HackathonAdapter
import com.example.matchmyskills.model.Hackathon
import com.google.firebase.firestore.FirebaseFirestore

class HackathonActivity : AppCompatActivity() {

    private lateinit var rvHackathons: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var hackathonAdapter: HackathonAdapter
    private val db = FirebaseFirestore.getInstance()

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
        db.collection("hackathons")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                val hackathonList = mutableListOf<Hackathon>()
                for (doc in documents) {
                    try {
                        val hackathon = doc.toObject(Hackathon::class.java)
                        hackathonList.add(hackathon)
                    } catch (e: Exception) {
                        Log.e("HackathonActivity", "Error parsing doc ${doc.id}", e)
                    }
                }
                hackathonAdapter.updateData(hackathonList)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load hackathons: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
