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
import com.example.matchmyskills.ai.CandidateAiAnalyzer
import com.example.matchmyskills.ai.CandidateAnalysisInput
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
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class ApplicantDetailFragment : Fragment(R.layout.fragment_applicant_detail) {

    private val viewModel: ApplicantViewModel by viewModels()
    private val args: ApplicantDetailFragmentArgs by navArgs()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var _binding: FragmentApplicantDetailBinding? = null
    private val binding get() = _binding!!
    private var application: Application? = null
    private var currentProfileImageUrl: String? = null
    private var currentJobTitle: String = "the opportunity"

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
                runAiAnalysis(app)
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

            // Handle status-based UI
            updateActionButtonsState(app.status)

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
                        val intent = android.content.Intent(requireContext(), com.example.matchmyskills.ResumePreviewActivity::class.java)
                        intent.putExtra("resume_url", finalUrl)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open resume preview.", Toast.LENGTH_SHORT).show()
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

    private fun updateActionButtonsState(status: String) {
        binding.apply {
            when (status.lowercase()) {
                "shortlisted" -> {
                    btnShortlist.text = "Shortlisted"
                    btnShortlist.isEnabled = false
                    btnShortlist.alpha = 0.6f
                    btnReject.visibility = View.GONE
                }
                "rejected" -> {
                    btnReject.text = "Rejected"
                    btnReject.isEnabled = false
                    btnShortlist.visibility = View.GONE
                }
                else -> {
                    btnShortlist.text = "Shortlist Candidate"
                    btnShortlist.isEnabled = true
                    btnShortlist.alpha = 1.0f
                    btnReject.visibility = View.VISIBLE
                    btnReject.isEnabled = true
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

        if (!isAdded || _binding == null) return

        binding.ivProfile.setImageResource(R.drawable.ic_profile)

        firestore.collection("users").document(candidateId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val profileImage = doc.getString("profileImage")?.trim().orEmpty()
                val profileImageUrl = doc.getString("profileImageUrl")?.trim().orEmpty()
                val actualImageUrl = profileImage.ifBlank { profileImageUrl }
                currentProfileImageUrl = actualImageUrl.ifBlank { null }

                if (actualImageUrl.isNotBlank()) {
                    Glide.with(this@ApplicantDetailFragment)
                        .load(actualImageUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.ivProfile)
                } else {
                    binding.ivProfile.setImageResource(R.drawable.ic_profile)
                }
            }
            .addOnFailureListener { error ->
                Log.e("ApplicantDetail", "Error loading profile image", error)
                if (isAdded && _binding != null) {
                    binding.ivProfile.setImageResource(R.drawable.ic_profile)
                }
            }
    }

    private fun setupListeners() {
        binding.btnShortlist.setOnClickListener {
            application?.let { app ->
                if (app.candidateId.isBlank()) {
                    Toast.makeText(context, "Cannot shortlist: Candidate ID missing", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val displayTitle = app.opportunityTitle.ifBlank { currentJobTitle }
                viewModel.shortlistCandidate(
                    applicationId = app.id,
                    candidateId = app.candidateId,
                    jobTitle = displayTitle,
                    candidateEmail = app.candidateEmail,
                    candidateName = app.candidateName,
                    opportunityId = app.opportunityId,
                    opportunityType = app.opportunityType
                )
            }
        }
        binding.btnReject.setOnClickListener {
            application?.let { app ->
                if (app.candidateId.isBlank()) {
                    Toast.makeText(context, "Cannot reject: Candidate ID missing", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val displayTitle = app.opportunityTitle.ifBlank { currentJobTitle }
                viewModel.rejectCandidate(
                    applicationId = app.id, 
                    candidateId = app.candidateId, 
                    jobTitle = displayTitle,
                    opportunityId = app.opportunityId,
                    opportunityType = app.opportunityType
                )
            }
        }
    }

    private fun runAiAnalysis(app: Application) {
        binding.progressAiAnalysis.visibility = View.VISIBLE
        binding.tvAiMatchScore.text = "Match Score: --"
        binding.tvAiFitLabel.text = "Analyzing..."

        lifecycleScope.launch {
            val opportunityContext = fetchOpportunityContext(app)
            if (opportunityContext == null) {
                binding.progressAiAnalysis.visibility = View.GONE
                binding.tvAiFitLabel.text = "Needs Improvement"
                binding.tvAiRecommendation.text = "Recommendation: Could not load job details for AI insights."
                return@launch
            }

            val (jobTitle, jobDescription, requiredSkills) = opportunityContext
            currentJobTitle = jobTitle
            val result = CandidateAiAnalyzer.analyze(
                CandidateAnalysisInput(
                    jobDescription = jobDescription,
                    requiredSkills = requiredSkills,
                    candidateSkills = app.candidateSkills.ifEmpty { app.skills },
                    resumeText = app.resumeText
                )
            )

            binding.progressAiAnalysis.visibility = View.GONE

            result.onSuccess { analysis ->
                binding.tvAiMatchScore.text = "Match Score: ${analysis.matchPercentage}%"
                binding.tvAiFitLabel.text = analysis.fitLabel
                binding.tvAiStrengths.text = "Strengths:\n${formatList(analysis.strengths)}"
                binding.tvAiMissing.text = "Missing Skills:\n${formatList(analysis.missingSkills)}"
                binding.tvAiRecommendation.text = "Recommendation: ${analysis.recommendation}"
            }.onFailure { error ->
                binding.tvAiMatchScore.text = "Match Score: --"
                binding.tvAiFitLabel.text = "Needs Improvement"
                binding.tvAiStrengths.text = "Strengths:\n- AI analysis unavailable"
                binding.tvAiMissing.text = "Missing Skills:\n- Not enough data"
                binding.tvAiRecommendation.text = "Recommendation: ${error.message ?: "AI service unavailable"}"
            }
        }
    }

    private suspend fun fetchOpportunityContext(app: Application): Triple<String, String, List<String>>? {
        return try {
            if (app.opportunityType.equals("HACKATHON", ignoreCase = true)) {
                val doc = firestore.collection("hackathons")
                    .document(app.opportunityId.ifBlank { app.jobId })
                    .get()
                    .await()
                val title = doc.getString("title").orEmpty()
                val description = doc.getString("description").orEmpty()
                val skills = doc.get("themes") as? List<String> ?: emptyList()
                Triple(title, description, skills)
            } else {
                val doc = firestore.collection("jobs")
                    .document(app.opportunityId.ifBlank { app.jobId })
                    .get()
                    .await()
                val title = doc.getString("title").orEmpty()
                val description = doc.getString("description").orEmpty()
                val skills = (doc.get("coreSkills") as? List<String>)
                    ?: (doc.get("skills") as? List<String>)
                    ?: emptyList()
                Triple(title, description, skills)
            }
        } catch (e: Exception) {
            Log.e("ApplicantDetail", "Failed to load opportunity context", e)
            null
        }
    }

    private fun formatList(items: List<String>): String {
        if (items.isEmpty()) return "- None"
        return items.joinToString("\n") { "- $it" }
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
