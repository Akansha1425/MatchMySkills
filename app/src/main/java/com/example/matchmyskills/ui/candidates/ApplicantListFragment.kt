package com.example.matchmyskills.ui.candidates

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentApplicantListBinding
import com.example.matchmyskills.model.Application
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.ApplicantViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class ApplicantListFragment : Fragment(R.layout.fragment_applicant_list) {

    private val viewModel: ApplicantViewModel by viewModels()
    private val args: ApplicantListFragmentArgs by navArgs()
    private var _binding: FragmentApplicantListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ApplicantAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentApplicantListBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        observeState()

        fetchByTab(0)
    }

    private fun setupRecyclerView() {
        adapter = ApplicantAdapter(
            onApplicantClick = { app ->
                val action = ApplicantListFragmentDirections.actionCandidatesFragmentToApplicantDetailFragment(app.id)
                findNavController().navigate(action)
            },
            onShortlistClick = { app ->
                viewModel.updateStatus(app.id, "Shortlisted")
            },
            onRejectClick = { app ->
                viewModel.updateStatus(app.id, "Rejected")
            }
        )
        binding.rvApplicants.layoutManager = LinearLayoutManager(context)
        binding.rvApplicants.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.let { fetchByTab(it.position) }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchCandidates(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun fetchByTab(position: Int) {
        val jobId = args.jobId
        val statuses = when (position) {
            0 -> emptyList() // All
            1 -> listOf("Shortlisted")
            2 -> listOf("Rejected")
            else -> emptyList()
        }
        viewModel.fetchApplicants(jobId, statuses)
    }

    private fun observeState() {
        viewModel.applicantsState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                    adapter.submitList(state.data)
                    updateTabCounts(state.data)
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = state.message
                }
                is UiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    adapter.submitList(emptyList())
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.updateState.onEach { state ->
            if (state is UiState.Success) {
                // Refresh current tab
                fetchByTab(binding.tabLayout.selectedTabPosition)
                viewModel.resetUpdateState()
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateTabCounts(data: List<Application>) {
        // Note: This only counts items in the current view. 
        // For real counts across all tabs, we'd need a separate fetch or count logic.
        // For now, let's just update the selected tab text if needed.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
