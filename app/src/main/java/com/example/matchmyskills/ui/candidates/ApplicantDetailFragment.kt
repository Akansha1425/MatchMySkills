package com.example.matchmyskills.ui.candidates

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentApplicantDetailBinding
import com.example.matchmyskills.model.Application
import com.example.matchmyskills.util.toApplication
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.ApplicantViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ApplicantDetailFragment : Fragment(R.layout.fragment_applicant_detail) {

    private val viewModel: ApplicantViewModel by viewModels()
    private val args: ApplicantDetailFragmentArgs by navArgs()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var _binding: FragmentApplicantDetailBinding? = null
    private val binding get() = _binding!!
    private var application: Application? = null
    private var currentProfileImageUrl: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            _binding = FragmentApplicantDetailBinding.bind(view)

            loadApplicationAndBind()
            setupListeners()
            observeState()
        } catch (e: Exception) {
            Log.e("ApplicantDetail", "Error in onViewCreated", e)
            Toast.makeText(context, "Error initializing detail view: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadApplicationAndBind() {
        val applicationId = args.applicationId
        if (applicationId.isBlank()) {
            Toast.makeText(context, "Invalid applicant data", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        firestore.collection("applications").document(applicationId).get()
            .addOnSuccessListener { doc ->
                val app = doc.toApplication()
                if (app == null) {
                    Toast.makeText(context, "Applicant details not found", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    return@addOnSuccessListener
                }
                application = app
                setupUI(app)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load applicant details", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
    }

    private fun setupUI(app: Application) {
        binding.apply {
            toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
            tvName.text = app.candidateName
            tvCollege.text = app.candidateCollege
            tvEmail.text = app.candidateEmail
            tvMarks.text = "Marks: ${app.candidateMarks}"
            tvReason.text = app.candidateReason
            loadCandidateProfileImage(app.candidateId)
            
            tvScoreValue.text = "${app.matchScore.toInt()}%"
            progressScore.progress = app.matchScore.toInt()
            
            // Show a simple unified summary of matched skills
            tvCoreMatch.text = "${app.coreMatchCount} / ${app.optionalMatchCount} skills matched"
            tvOptionalMatch.visibility = android.view.View.GONE

            cgSkills.removeAllViews()
            app.candidateSkills.forEach { skill ->
                val chip = Chip(requireContext()).apply {
                    text = skill
                }
                cgSkills.addView(chip)
            }

            cgMatchedSkills.removeAllViews()
            if (app.matchedSkills.isEmpty()) {
                val placeholder = TextView(requireContext()).apply {
                    text = "No direct matches found"
                    alpha = 0.6f
                }
                cgMatchedSkills.addView(placeholder)
            } else {
                app.matchedSkills.forEach { skill ->
                    val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                        text = skill
                        setChipBackgroundColorResource(android.R.color.holo_green_light)
                        setTextColor(android.graphics.Color.WHITE)
                        chipStrokeWidth = 0f
                        isClickable = false
                    }
                    cgMatchedSkills.addView(chip)
                }
            }

            cgMissingSkills.removeAllViews()
            if (app.missingSkills.isEmpty()) {
                val placeholder = TextView(requireContext()).apply {
                    text = "No missing skills 🎉"
                    setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 8, 0, 8)
                }
                cgMissingSkills.addView(placeholder)
            } else {
                app.missingSkills.forEach { skill ->
                    val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                        text = skill
                        setChipBackgroundColorResource(android.R.color.white)
                        setTextColor(android.graphics.Color.RED)
                        chipStrokeWidth = 2f
                        setChipStrokeColorResource(android.R.color.holo_red_light)
                        isClickable = false
                    }
                    cgMissingSkills.addView(chip)
                }
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

            ivProfile.setOnClickListener {
                currentProfileImageUrl?.let { url ->
                    val intent = android.content.Intent(requireContext(), com.example.matchmyskills.ImagePreviewActivity::class.java)
                    intent.putExtra("image_url", url)
                    startActivity(intent)
                }
            }
        }
    }

    private fun loadCandidateProfileImage(candidateId: String) {
        if (candidateId.isNullOrBlank()) {
            if (isAdded && _binding != null) {
                binding.ivProfile.setImageResource(R.drawable.ic_profile)
            }
            return
        }

        lifecycleScope.launch {
            try {
                firestore.collection("users").document(candidateId).get()
                    .addOnSuccessListener { doc ->
                        if (!isAdded || _binding == null) return@addOnSuccessListener

                        val profileImage = doc.getString("profileImage")
                        val profileImageUrl = doc.getString("profileImageUrl")
                        val actualUrl = profileImage ?: profileImageUrl
                        currentProfileImageUrl = actualUrl
                        
                        if (!actualUrl.isNullOrBlank()) {
                            Glide.with(this@ApplicantDetailFragment)
                                .load(actualUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.ivProfile)
                        } else {
                            binding.ivProfile.setImageResource(R.drawable.ic_profile)
                        }
                    }
                    .addOnFailureListener {
                        if (isAdded && _binding != null) {
                            binding.ivProfile.setImageResource(R.drawable.ic_profile)
                        }
                    }
            } catch (e: Exception) {
                Log.e("ApplicantDetail", "Error loading profile image", e)
            }
        }
    }

    private fun setupListeners() {
        binding.btnShortlist.setOnClickListener {
            application?.id?.let { viewModel.updateStatus(it, "Shortlisted") }
        }
        binding.btnReject.setOnClickListener {
            application?.id?.let { viewModel.updateStatus(it, "Rejected") }
        }
        binding.btnHire.setOnClickListener {
            application?.id?.let { viewModel.updateStatus(it, "Hired") }
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
