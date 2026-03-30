package com.example.matchmyskills.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentDashboardBinding
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DashboardAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = DashboardAdapter(
            onJobClick = { job ->
                val action = DashboardFragmentDirections.actionDashboardFragmentToApplicantListFragment(job.id)
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
            BottomSheetCreateOpportunity().show(childFragmentManager, BottomSheetCreateOpportunity.TAG)
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
                    // Client-side sorting because server-side orderBy was removed to avoid index requirements
                    val sortedJobs = data.jobs.sortedByDescending { it.createdAt?.time ?: 0L }
                    val sortedHackathons = data.hackathons.sortedByDescending { it.createdAt?.time ?: 0L }
                    
                    items.addAll(sortedJobs.map { DashboardItem.JobItem(it) })
                    items.addAll(sortedHackathons.map { DashboardItem.HackathonItem(it) })
                    
                    adapter.submitList(items)
                    updateStats(state.data)
                    
                    binding.rvJobs.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
                is UiState.Empty -> {
                    adapter.submitList(emptyList())
                    updateStats(DashboardViewModel.DashboardData(emptyList(), emptyList(), 0, 0, 0, 0))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
