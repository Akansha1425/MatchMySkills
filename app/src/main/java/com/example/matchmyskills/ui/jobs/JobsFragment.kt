package com.example.matchmyskills.ui.jobs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentJobsBinding
import com.example.matchmyskills.ui.dashboard.DashboardAdapter
import com.example.matchmyskills.ui.dashboard.DashboardItem
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class JobsFragment : Fragment(R.layout.fragment_jobs) {

    private val viewModel: DashboardViewModel by viewModels()
    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DashboardAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentJobsBinding.bind(view)

        setupRecyclerView()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = DashboardAdapter(
            onJobClick = { job ->
                // Navigate to same applicant list as dashboard
                val action = JobsFragmentDirections.actionJobsFragmentToApplicantListFragment(job.id)
                findNavController().navigate(action)
            },
            onHackathonClick = { _ ->
                Toast.makeText(context, "Hackathon details coming soon", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvJobs.layoutManager = LinearLayoutManager(context)
        binding.rvJobs.adapter = adapter
    }

    private fun observeState() {
        viewModel.dashboardState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val data = state.data
                    val items = mutableListOf<DashboardItem>()
                    items.addAll(data.jobs.map { DashboardItem.JobItem(it) })
                    items.addAll(data.hackathons.map { DashboardItem.HackathonItem(it) })
                    
                    adapter.submitList(items)
                    binding.rvJobs.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
                is UiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvJobs.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
