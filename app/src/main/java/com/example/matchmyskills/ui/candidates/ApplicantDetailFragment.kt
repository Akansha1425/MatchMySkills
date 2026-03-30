package com.example.matchmyskills.ui.candidates

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentApplicantDetailBinding
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.ApplicantViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class ApplicantDetailFragment : Fragment(R.layout.fragment_applicant_detail) {

    private val viewModel: ApplicantViewModel by viewModels()
    private val args: ApplicantDetailFragmentArgs by navArgs()
    private var _binding: FragmentApplicantDetailBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentApplicantDetailBinding.bind(view)

        setupUI()
        setupListeners()
        observeState()
    }

    private fun setupUI() {
        val app = args.application
        binding.apply {
            toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
            tvName.text = app.candidateName
            tvCollege.text = app.candidateCollege
            tvScoreValue.text = "${app.matchScore.toInt()}%"
            progressScore.progress = app.matchScore.toInt()
            tvCoreMatch.text = "Core: ${app.coreMatchCount}" // Placeholder for total core skills
            tvOptionalMatch.text = "Optional: ${app.optionalMatchCount}" // Placeholder for total optional skills

            cgSkills.removeAllViews()
            app.candidateSkills.forEach { skill ->
                val chip = Chip(context).apply {
                    text = skill
                }
                cgSkills.addView(chip)
            }
        }
    }

    private fun setupListeners() {
        binding.btnShortlist.setOnClickListener {
            viewModel.updateStatus(args.application.id, "Shortlisted")
        }
        binding.btnReject.setOnClickListener {
            viewModel.updateStatus(args.application.id, "Rejected")
        }
        binding.btnHire.setOnClickListener {
            viewModel.updateStatus(args.application.id, "Hired")
        }
    }

    private fun observeState() {
        viewModel.updateState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    // Show small loading overlay
                }
                is UiState.Success -> {
                    Toast.makeText(context, "Status updated!", Toast.LENGTH_SHORT).show()
                    viewModel.resetUpdateState()
                    findNavController().popBackStack()
                }
                is UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
