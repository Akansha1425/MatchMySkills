package com.example.matchmyskills.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentDashboardBinding
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.ui.dashboard.BottomSheetCreateOpportunity
import com.example.matchmyskills.util.LocationHelper
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.DashboardViewModel
import com.example.matchmyskills.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.example.matchmyskills.viewmodel.AuthViewModel

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DashboardAdapter

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("LocationPermission", "Permission granted, fetching location")
            fetchLocation()
        } else {
            Log.w("LocationPermission", "Permission denied")
            binding.locationText.text = "Location permission denied"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        observeState()
        observeNotificationBadge()
        
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
                val loc = "$city, $state"
                binding.locationText.text = "📍 $loc"
                Log.d("LocationFetched", "Location: $loc")
                // Save to profile so it's visible on Profile Page
                authViewModel.updateProfile(mapOf("location" to loc))
            }

            override fun onLocationError(message: String) {
                binding.locationText.text = message
                Log.e("LocationError", message)
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = DashboardAdapter(
            onJobClick = { job ->
                // Navigate to same applicant list as dashboard
                val action = DashboardFragmentDirections.actionDashboardFragmentToCandidatesFragment(job.id)
                findNavController().navigate(action)
            },
            onHackathonClick = { hackathon ->
                val action = DashboardFragmentDirections.actionDashboardFragmentToHackathonDetailFragment(hackathon.id)
                findNavController().navigate(action)
            }
        )
        binding.rvJobs.layoutManager = LinearLayoutManager(context)
        binding.rvJobs.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            Log.d("CREATE_OPPORTUNITY", "Dashboard FAB clicked")
            BottomSheetCreateOpportunity().show(parentFragmentManager, BottomSheetCreateOpportunity.TAG)
        }

        binding.notificationContainer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_notificationsFragment)
        }
    }

    private fun observeState() {
        viewModel.dashboardState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    // Show shimmer or loading indicator
                }
                is UiState.Success -> {
                    val data = state.data
                    val items = mutableListOf<DashboardItem>()
                    val groupedCounts = data.applicationCountByOpportunityId
                    // Client-side sorting because server-side orderBy was removed to avoid index requirements
                    val sortedJobs = data.jobs.sortedByDescending { it.createdAt?.time ?: 0L }
                    val sortedHackathons = data.hackathons.sortedByDescending { it.createdAt?.time ?: 0L }
                    
                    items.addAll(sortedJobs.map { job ->
                        DashboardItem.JobItem(job, groupedCounts[job.id] ?: 0)
                    })
                    items.addAll(sortedHackathons.map { DashboardItem.HackathonItem(it) })
                    
                    adapter.submitList(items)
                    updateStats(state.data)
                    
                    binding.rvJobs.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is UiState.Empty -> {
                    adapter.submitList(emptyList())
                    updateStats(DashboardViewModel.DashboardData(emptyList(), emptyList(), emptyMap(), 0, 0, 0, 0, 0))
                    binding.rvJobs.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                }
                is UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateStats(data: DashboardViewModel.DashboardData) {
        binding.tvCountInternships.text = data.jobs.size.toString()
        binding.tvCountHackathons.text = data.hackathons.size.toString()
        binding.tvCountApplicants.text = data.totalApplicants.toString()
        binding.tvCountShortlisted.text = data.shortlistedCount.toString()
        binding.tvCountPending.text = data.pendingCount.toString()
        binding.tvCountRejected.text = data.rejectedCount.toString()
    }

    private fun observeNotificationBadge() {
        notificationViewModel.unreadCount.onEach { count ->
            if (count > 0) {
                binding.tvNotificationBadge.visibility = View.VISIBLE
                binding.tvNotificationBadge.text = if (count > 99) "99+" else count.toString()
            } else {
                binding.tvNotificationBadge.visibility = View.GONE
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
