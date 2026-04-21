package com.example.matchmyskills

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AlertDialog
import com.example.matchmyskills.data.remote.ExternalOpportunityDataSource
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.util.LocationHelper
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.button.MaterialButton
import android.content.Intent
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
    private lateinit var chartJobs: PieChart
    private lateinit var chartInternships: PieChart
    private lateinit var chartHackathons: PieChart
    private lateinit var tvGreeting: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var locationText: TextView
    private lateinit var ivProfileDashboard: android.widget.ImageView
    private var currentProfileImageUrl: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("LocationPermission", "Permission granted, fetching location")
            fetchLocation()
        } else {
            Log.w("LocationPermission", "Permission denied")
            locationText.text = "Location permission denied"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvJobsCount = view.findViewById(R.id.tv_jobs_count)
        tvInternshipsCount = view.findViewById(R.id.tv_internships_count)
        tvHackathonsCount = view.findViewById(R.id.tv_hackathons_count)
        tvAppliedCountLabel = view.findViewById(R.id.tv_applied_count_label)
        progressApplied = view.findViewById(R.id.progress_applied)
        chartJobs = view.findViewById(R.id.chart_jobs)
        chartInternships = view.findViewById(R.id.chart_internships)
        chartHackathons = view.findViewById(R.id.chart_hackathons)
        tvGreeting = view.findViewById(R.id.tv_greeting)
        btnLogout = view.findViewById(R.id.btn_logout)
        locationText = view.findViewById(R.id.location_text)
        ivProfileDashboard = view.findViewById(R.id.iv_profile_dashboard)
        
        ivProfileDashboard.setOnClickListener {
            currentProfileImageUrl?.let { url ->
                val intent = Intent(requireContext(), ImagePreviewActivity::class.java)
                intent.putExtra("image_url", url)
                startActivity(intent)
            }
        }
        
        setupPieCharts()
        loadUserDetails()
        loadDashboardData()
        
        // Request and fetch location
        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        if (LocationHelper.isLocationPermissionGranted(requireContext())) {
            Log.d("LocationPermission", "Permission already granted")
            fetchLocation()
        } else {
            Log.d("LocationPermission", "Requesting permission")
            requestLocationPermission.launch(LocationHelper.getRequiredPermissions()[0])
        }
    }

    private fun fetchLocation() {
        LocationHelper.fetchLocation(requireContext(), object : LocationHelper.LocationCallback {
            override fun onLocationFetched(city: String, state: String) {
                val loc = "📍 $city, $state"
                locationText.text = loc
                Log.d("LocationFetched", "Location: $city, $state")
                
                // Save to Firestore so other pages (like Profile) can see it
                saveLocationToFirestore("$city, $state")
            }

            override fun onLocationError(message: String) {
                locationText.text = message
                Log.e("LocationError", message)
            }
        })
    }

    private fun saveLocationToFirestore(location: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).update("location", location)
            .addOnSuccessListener {
                Log.d("HomeFragment", "Location updated in Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Failed to update location", e)
            }
    }

    private fun setupPieCharts() {
        chartJobs.setUsePercentValues(true)
        chartInternships.setUsePercentValues(true)
        chartHackathons.setUsePercentValues(true)

        listOf(chartJobs, chartInternships, chartHackathons).forEach { chart ->
            chart.description.isEnabled = false
            chart.setTouchEnabled(false)
            chart.isRotationEnabled = false
            chart.animateY(1000)
            chart.legend.isEnabled = false
        }
    }

    private fun loadUserDetails() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users").document(userId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e("HomeFragment", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (doc != null && doc.exists() && isAdded) {
                    val name = doc.getString("name") ?: "Student"
                    val profileImageUrl = doc.getString("profileImage") ?: doc.getString("profileImageUrl")
                    currentProfileImageUrl = profileImageUrl
                    
                    tvGreeting.text = "Welcome back, $name 👋"

                    if (!profileImageUrl.isNullOrBlank()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(ivProfileDashboard)
                    }
                }
            }
    }

    private fun setupLogoutButton() {
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun loadDashboardData() {
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

                // Fetch Applications by type
                val jobApplicationsTask = async {
                    try {
                        val userId = auth.currentUser?.uid ?: return@async 0
                        val snapshot = db.collection("applications")
                            .whereEqualTo("candidateId", userId)
                            .whereEqualTo("opportunityType", "JOB")
                            .get()
                            .await()
                        snapshot.size()
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error fetching job applications", e)
                        0
                    }
                }

                val internshipApplicationsTask = async {
                    try {
                        val userId = auth.currentUser?.uid ?: return@async 0
                        val snapshot = db.collection("applications")
                            .whereEqualTo("candidateId", userId)
                            .whereEqualTo("opportunityType", "INTERNSHIP")
                            .get()
                            .await()
                        snapshot.size()
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error fetching internship applications", e)
                        0
                    }
                }

                val hackathonApplicationsTask = async {
                    try {
                        val userId = auth.currentUser?.uid ?: return@async 0
                        val snapshot = db.collection("applications")
                            .whereEqualTo("candidateId", userId)
                            .whereEqualTo("opportunityType", "HACKATHON")
                            .get()
                            .await()
                        snapshot.size()
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error fetching hackathon applications", e)
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
                val jobApplied = jobApplicationsTask.await()
                val internshipApplied = internshipApplicationsTask.await()
                val hackathonApplied = hackathonApplicationsTask.await()
                
                val extJobsCount = externalJobsTask.await()
                val extInternshipsCount = externalInternshipsTask.await()
                val extHackathonsCount = externalHackathonsTask.await()

                val totalJobs = fbJobsCount + extJobsCount
                val totalInternships = fbInternshipsCount + extInternshipsCount
                val totalHackathons = fbHackathonsCount + extHackathonsCount
                val totalApplications = jobApplied + internshipApplied + hackathonApplied
                val totalOpportunities = totalJobs + totalInternships + totalHackathons

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    tvJobsCount.text = totalJobs.toString()
                    tvInternshipsCount.text = totalInternships.toString()
                    tvHackathonsCount.text = totalHackathons.toString()

                    tvAppliedCountLabel.text = "$totalApplications Applications / $totalOpportunities Total Opportunities"
                    
                    if (totalOpportunities > 0) {
                        val progress = ((totalApplications.toFloat() / totalOpportunities.toFloat()) * 100).toInt()
                        progressApplied.progress = progress.coerceAtMost(100)
                    } else {
                        progressApplied.progress = 0
                    }

                    // Setup pie charts
                    setupPieChart(chartJobs, jobApplied, totalJobs, "#1565C0")
                    setupPieChart(chartInternships, internshipApplied, totalInternships, "#2E7D32")
                    setupPieChart(chartHackathons, hackathonApplied, totalHackathons, "#EF6C00")
                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading dashboard data", e)
            }
        }
    }

    private fun setupPieChart(chart: PieChart, applied: Int, total: Int, color: String) {
        val entries = mutableListOf<PieEntry>()
        val notApplied = (total - applied).coerceAtLeast(0)

        entries.add(PieEntry(applied.toFloat(), "Applied"))
        if (notApplied > 0) {
            entries.add(PieEntry(notApplied.toFloat(), "Not Applied"))
        }

        val dataSet = PieDataSet(entries, "").apply {
            val appliedColor = android.graphics.Color.parseColor(color)
            val notAppliedColor = android.graphics.Color.parseColor("#E0E0E0")
            setColors(if (notApplied > 0) listOf(appliedColor, notAppliedColor) else listOf(appliedColor))
            valueFormatter = PercentFormatter()
            valueTextSize = 12f
            valueTextColor = android.graphics.Color.BLACK
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter())
        }

        chart.data = data
        chart.invalidate()
    }

    private fun isInternship(job: Job): Boolean {
        if (job.opportunityType.equals("INTERNSHIP", ignoreCase = true)) {
            return true
        }
        val text = "${job.title} ${job.description}".lowercase()
        return text.contains("intern") || text.contains("internship")
    }
}
