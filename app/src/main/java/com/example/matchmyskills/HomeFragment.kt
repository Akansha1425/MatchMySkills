package com.example.matchmyskills

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.core.view.isVisible
import com.example.matchmyskills.util.LocationHelper
import com.example.matchmyskills.viewmodel.StudentDashboardUiState
import com.example.matchmyskills.viewmodel.StudentDashboardViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.content.Intent
import com.example.matchmyskills.viewmodel.UserNotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var tvJobsCount: TextView
    private lateinit var tvInternshipsCount: TextView
    private lateinit var tvHackathonsCount: TextView
    private lateinit var tvAppliedCountLabel: TextView
    private lateinit var progressApplied: LinearProgressIndicator
    private lateinit var progressJobs: LinearProgressIndicator
    private lateinit var progressInternships: LinearProgressIndicator
    private lateinit var progressHackathons: LinearProgressIndicator
    private lateinit var tvJobsApplied: TextView
    private lateinit var tvInternshipsApplied: TextView
    private lateinit var tvHackathonsApplied: TextView
    private lateinit var tvGreeting: TextView
    private lateinit var locationText: TextView
    private lateinit var ivProfileDashboard: com.google.android.material.imageview.ShapeableImageView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var contentContainer: LinearLayout
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var notificationButton: View
    private lateinit var notificationBadge: TextView
    private var currentProfileImageUrl: String? = null

    private val viewModel: StudentDashboardViewModel by viewModels()
    private val notificationViewModel: UserNotificationViewModel by viewModels()

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchLocation()
        } else {
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
        progressJobs = view.findViewById(R.id.progress_jobs)
        progressInternships = view.findViewById(R.id.progress_internships)
        progressHackathons = view.findViewById(R.id.progress_hackathons)
        tvJobsApplied = view.findViewById(R.id.tv_jobs_applied_value)
        tvInternshipsApplied = view.findViewById(R.id.tv_internships_applied_value)
        tvHackathonsApplied = view.findViewById(R.id.tv_hackathons_applied_value)
        tvGreeting = view.findViewById(R.id.tv_greeting)
        locationText = view.findViewById(R.id.location_text)
        ivProfileDashboard = view.findViewById(R.id.iv_profile_dashboard)
        loadingIndicator = view.findViewById(R.id.loading_indicator)
        contentContainer = view.findViewById(R.id.content_container)
        emptyStateContainer = view.findViewById(R.id.empty_state_container)
        tvErrorMessage = view.findViewById(R.id.tv_error_message)
        notificationButton = view.findViewById(R.id.btn_notifications)
        notificationBadge = view.findViewById(R.id.tv_notification_badge)
        
        ivProfileDashboard.setOnClickListener {
            val url = currentProfileImageUrl?.trim().orEmpty()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@setOnClickListener
            }
            val intent = Intent(requireContext(), ImagePreviewActivity::class.java)
            intent.putExtra("image_url", url)
            startActivity(intent)
        }

        notificationButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, com.example.matchmyskills.ui.notifications.UserNotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        observeDashboardState()
        observeNotifications()
        
        // Request and fetch location
        requestLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDashboard()
    }

    private fun requestLocationPermission() {
        if (LocationHelper.isLocationPermissionGranted(requireContext())) {
            fetchLocation()
        } else {
            requestLocationPermission.launch(LocationHelper.getRequiredPermissions()[0])
        }
    }

    private fun fetchLocation() {
        LocationHelper.fetchLocation(requireContext(), object : LocationHelper.LocationCallback {
            override fun onLocationFetched(city: String, state: String) {
                val loc = "$city, $state"
                locationText.text = loc
                viewModel.updateLocation(loc)
            }

            override fun onLocationError(message: String) {
                locationText.text = "Location unavailable"
            }
        })
    }

    private fun observeNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            notificationViewModel.unreadCount.collectLatest { count ->
                if (count > 0) {
                    notificationBadge.isVisible = true
                    notificationBadge.text = if (count > 9) "9+" else count.toString()
                } else {
                    notificationBadge.isVisible = false
                }
            }
        }
    }

    private fun observeDashboardState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                renderState(state)
            }
        }
    }

    private fun renderState(state: StudentDashboardUiState) {
        loadingIndicator.isVisible = state.isLoading
        contentContainer.isVisible = !state.isLoading && !state.isEmpty
        emptyStateContainer.isVisible = !state.isLoading && state.isEmpty
        tvErrorMessage.isVisible = !state.errorMessage.isNullOrBlank()
        tvErrorMessage.text = state.errorMessage.orEmpty()

        locationText.text = state.location
        tvGreeting.text = "Welcome back, ${state.userName}"

        currentProfileImageUrl = state.profileImageUrl
        if (!state.profileImageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(state.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(ivProfileDashboard)
        } else {
            ivProfileDashboard.setImageResource(R.drawable.ic_profile)
        }

        val stats = state.stats
        tvJobsCount.text = stats.totalJobs.toString()
        tvInternshipsCount.text = stats.totalInternships.toString()
        tvHackathonsCount.text = stats.totalHackathons.toString()

        tvAppliedCountLabel.text = "${stats.totalApplications} Applications / ${stats.totalOpportunities} Opportunities"
        animateProgress(progressApplied, (stats.activityProgress * 100f).toInt())

        renderSection(
            applied = stats.appliedJobs,
            total = stats.totalJobs,
            progressBar = progressJobs,
            label = tvJobsApplied
        )

        renderSection(
            applied = stats.appliedInternships,
            total = stats.totalInternships,
            progressBar = progressInternships,
            label = tvInternshipsApplied
        )

        renderSection(
            applied = stats.appliedHackathons,
            total = stats.totalHackathons,
            progressBar = progressHackathons,
            label = tvHackathonsApplied
        )
    }

    private fun renderSection(
        applied: Int,
        total: Int,
        progressBar: LinearProgressIndicator,
        label: TextView
    ) {
        val appliedPercent = if (total > 0) {
            ((applied.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }

        label.text = "$applied / $total Applied"
        animateProgress(progressBar, appliedPercent)
    }

    private fun animateProgress(progressBar: LinearProgressIndicator, target: Int) {
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, target).apply {
            duration = 450
            start()
        }
    }
}
