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
import com.example.matchmyskills.adapter.HackathonAdapter
import com.example.matchmyskills.data.remote.ExternalOpportunityDataSource
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.util.toHackathon
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class HackathonFragment : Fragment(R.layout.fragment_hackathon) {

    private lateinit var rvHackathons: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var hackathonAdapter: HackathonAdapter
    private val db = FirebaseFirestore.getInstance()
    private var firebaseHackathons: List<Hackathon> = emptyList()
    private var externalHackathons: List<Hackathon> = emptyList()
    private var allMergedHackathons: List<Hackathon> = emptyList()
    private var pendingSources: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvHackathons = view.findViewById(R.id.rvHackathons)
        progressBar = view.findViewById(R.id.progressBar)
        val etSearch = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)

        rvHackathons.layoutManager = LinearLayoutManager(requireContext())
        
        hackathonAdapter = HackathonAdapter(emptyList()) { selectedHackathon ->
            val intent = Intent(requireActivity(), OpportunityDetailActivity::class.java).apply {
                putExtra("EXTRA_HACKATHON", selectedHackathon)
            }
            startActivity(intent)
        }
        rvHackathons.adapter = hackathonAdapter

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterHackathons(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

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
                    val hackathon = doc.toHackathon()
                    if (hackathon != null) {
                        hackathonList.add(hackathon)
                    }
                }
                firebaseHackathons = hackathonList
                onSourceLoaded()
            }
            .addOnFailureListener { e ->
                firebaseHackathons = emptyList()
                onSourceLoaded()
                Toast.makeText(requireContext(), "Failed to load Firebase hackathons: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                externalHackathons = ExternalOpportunityDataSource.fetchHackathons()
            } catch (e: Exception) {
                Log.e("HackathonFragment", "External fetch failed", e)
                externalHackathons = emptyList()
            } finally {
                onSourceLoaded()
            }
        }
    }

    private fun onSourceLoaded() {
        pendingSources -= 1
        if (pendingSources > 0) return

        progressBar.visibility = View.GONE
        allMergedHackathons = (firebaseHackathons + externalHackathons)
            .distinctBy { it.id }

        hackathonAdapter.updateData(allMergedHackathons)
    }

    private fun filterHackathons(query: String) {
        if (query.isEmpty()) {
            hackathonAdapter.updateData(allMergedHackathons)
            return
        }

        val filtered = allMergedHackathons.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.organizer.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.themes.any { theme -> theme.contains(query, ignoreCase = true) }
        }
        hackathonAdapter.updateData(filtered)
    }
}
