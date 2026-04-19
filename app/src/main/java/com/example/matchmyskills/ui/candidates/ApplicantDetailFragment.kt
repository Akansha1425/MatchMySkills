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
            tvEmail.text = app.candidateEmail
            tvMarks.text = "Marks: ${app.candidateMarks}"
            tvReason.text = app.candidateReason
            
            tvScoreValue.text = "${app.matchScore.toInt()}%"
            progressScore.progress = app.matchScore.toInt()
            
            // Show a simple unified summary of matched skills
            tvCoreMatch.text = "${app.coreMatchCount} / ${app.optionalMatchCount} skills matched"
            tvOptionalMatch.visibility = android.view.View.GONE

            cgSkills.removeAllViews()
            app.candidateSkills.forEach { skill ->
                val chip = Chip(context).apply {
                    text = skill
                }
                cgSkills.addView(chip)
            }

            cgMatchedSkills.removeAllViews()
            app.matchedSkills.forEach { skill ->
                val chip = com.google.android.material.chip.Chip(context).apply {
                    text = skill
                    setChipBackgroundColorResource(android.R.`color`.holo_green_light)
                    alpha = 0.8f
                }
                cgMatchedSkills.addView(chip)
            }

            cgMissingSkills.removeAllViews()
            app.missingSkills.forEach { skill ->
                val chip = com.google.android.material.chip.Chip(context).apply {
                    text = skill
                    setChipBackgroundColorResource(android.R.`color`.holo_red_light)
                    alpha = 0.8f
                }
                cgMissingSkills.addView(chip)
            }

            btnViewResume.setOnClickListener {
                val url = app.resumeUrl.trim()
                if (url.isNotBlank()) {
                    try {
                        val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            "https://$url"
                        } else {
                            url
                        }
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(finalUrl))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open resume. Please ensure a valid browser is installed.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "No resume URL provided", Toast.LENGTH_SHORT).show()
                }
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
