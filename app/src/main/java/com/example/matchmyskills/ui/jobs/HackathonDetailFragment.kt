package com.example.matchmyskills.ui.jobs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentHackathonDetailBinding
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.OpportunityViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class HackathonDetailFragment : Fragment(R.layout.fragment_hackathon_detail) {

    private var _binding: FragmentHackathonDetailBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: OpportunityViewModel by viewModels()
    private val args: HackathonDetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHackathonDetailBinding.bind(view)

        setupToolbar()
        observeViewModel()
        
        viewModel.getHackathonDetails(args.hackathonId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.hackathonDetailState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    displayHackathonDetails(state.data)
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun displayHackathonDetails(hackathon: Hackathon) {
        binding.apply {
            tvDetailTitle.text = hackathon.title
            tvDetailOrganizer.text = "Organized by ${hackathon.organizer}"
            tvDetailDescription.text = hackathon.description
            tvDetailMode.text = hackathon.mode
            tvDetailPrizePool.text = if (hackathon.prizePool.isBlank()) "TBD" else hackathon.prizePool
            tvDetailEligibility.text = if (hackathon.eligibility.isBlank()) "Open to all" else hackathon.eligibility
            tvDetailTeamSize.text = hackathon.teamSize
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvDetailDeadline.text = hackathon.deadline?.let { dateFormat.format(it) } ?: "No deadline"

            // Themes
            chipGroupThemes.removeAllViews()
            hackathon.themes.forEach { theme ->
                val chip = Chip(requireContext()).apply {
                    text = theme
                    isClickable = false
                    setChipBackgroundColorResource(R.color.dashboard_surface)
                }
                chipGroupThemes.addView(chip)
            }

            btnViewApplicants.setOnClickListener {
                val action = HackathonDetailFragmentDirections.actionHackathonDetailFragmentToCandidatesFragment(hackathon.id)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
