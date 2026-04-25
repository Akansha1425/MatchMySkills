package com.example.matchmyskills.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.matchmyskills.LoginActivity
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentProfileBinding
import com.example.matchmyskills.model.User
import com.example.matchmyskills.repository.AuthRepository
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.AuthViewModel
import com.example.matchmyskills.viewmodel.DashboardViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: AuthViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var currentUser: User? = null

    private val profileUiPrefs by lazy {
        requireContext().getSharedPreferences("profile_ui_prefs", android.content.Context.MODE_PRIVATE)
    }

    private val requestLocationPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchLocation()
        }
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadProfileImage(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        setupListeners()
        observeProfile()
        observeImageUpload()
        observeDashboardState()
        viewModel.observeProfile()
        dashboardViewModel.fetchDashboardData()
    }

    private fun observeDashboardState() {
        dashboardViewModel.dashboardState.onEach { state ->
            if (state is UiState.Success) {
                val data = state.data
                binding.tvStatJobs.text = data.jobs.size.toString()
                binding.tvStatHires.text = data.hiredCount.toString()
                binding.tvStatApps.text = data.totalApplicants.toString()
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun observeProfile() {
        viewModel.profileState.onEach { state ->
            when (state) {
                is UiState.Success -> {
                    currentUser = state.data
                    bindUserData(state.data)
                }
                is UiState.Empty -> {
                    binding.tvCompanyName.text = "Profile Incomplete"
                }
                is UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun bindUserData(user: User) {
        if (!isAdded) {
            Log.w("ProfileFragment_Image", "Fragment not attached during bindUserData")
            return
        }

        try {
            with(binding) {
                tvCompanyName.text = user.companyName ?: "Add Company Name"
                tvEmail.text = user.email
                tvLocation.text = user.location ?: "Not specified"
                tvCompanySize.text = user.companySize ?: "Not specified"
                tvRecruiterName.text = user.name ?: "Add Name"
                tvJobTitle.text = user.jobTitle ?: "Add Job Title"
                tvBio.text = user.bio ?: "Add a professional bio to attract candidates"
                
                layoutVerified.isVisible = user.isVerified
                
                user.memberSince?.let {
                    val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    tvMemberSince.text = sdf.format(it)
                } ?: run {
                    tvMemberSince.text = "Joined Jan 2026"
                }

                // Load Profile Image safely
                val profileImageUrl = user.profileImageUrl ?: user.profileImage
                Log.d("ProfileFragment_Image", "Binding user data - ProfileImageUrl: $profileImageUrl")
                if (!profileImageUrl.isNullOrBlank()) {
                    Log.d("ProfileFragment_Image", "Loading profile image from URL: $profileImageUrl")
                    Glide.with(this@ProfileFragment)
                        .load(profileImageUrl)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivProfile)
                } else {
                    Log.d("ProfileFragment_Image", "Profile image URL is null/blank, using placeholder")
                    ivProfile.setImageResource(R.drawable.ic_profile)
                }

                tvProfileImageHint.isVisible = !profileImageUrl.isNullOrBlank() && !isProfileHintDismissed(user.id)

                if (user.location.isNullOrBlank()) {
                    checkLocationPermission()
                } else {
                    tvLocation.text = user.location
                }

                // Dynamic Chips
                chipGroupTags.removeAllViews()
                user.hiringTags.forEach { tag ->
                    val chip = Chip(requireContext()).apply {
                        text = tag
                        isClickable = false
                        isCheckable = false
                        setChipBackgroundColorResource(R.color.matchmyskills_divider)
                    }
                    chipGroupTags.addView(chip)
                }
                if (user.hiringTags.isEmpty()) {
                    val emptyChip = Chip(requireContext()).apply {
                        text = "+ Add Tags"
                        setOnClickListener { showEditProfileDialog() }
                    }
                    chipGroupTags.addView(emptyChip)
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment_Image", "Error in bindUserData", e)
            Toast.makeText(context, "Error binding profile data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeImageUpload() {
        viewModel.imageUploadState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    Toast.makeText(context, "Uploading photo...", Toast.LENGTH_SHORT).show()
                }
                is UiState.Success -> {
                    Toast.makeText(context, "Photo uploaded!", Toast.LENGTH_SHORT).show()
                }
                is UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnChangePhoto.setOnClickListener {
            launchProfileImagePicker()
        }

        binding.ivProfile.setOnClickListener {
            openProfileImagePreview()
        }

        binding.ivProfile.setOnLongClickListener {
            launchProfileImagePicker()
            true
        }

        binding.btnLogout.setOnClickListener {
            authRepository.clearUserData()
            val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnLinkedin.setOnClickListener {
            currentUser?.linkedin?.let { url ->
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } ?: Toast.makeText(context, "No LinkedIn profile linked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchProfileImagePicker() {
        if (!isAdded) return

        imagePicker.launch("image/*")
    }

    private fun openProfileImagePreview() {
        val imageUrl = currentUser?.profileImageUrl ?: currentUser?.profileImage
        if (imageUrl.isNullOrBlank() ||
            !(imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))
        ) {
            Toast.makeText(context, "No profile image to preview", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), com.example.matchmyskills.ImagePreviewActivity::class.java).apply {
            putExtra("image_url", imageUrl)
        }
        startActivity(intent)

        currentUser?.id?.let { userId ->
            markProfileHintDismissed(userId)
            binding.tvProfileImageHint.isVisible = false
        }
    }

    private fun isProfileHintDismissed(userId: String): Boolean {
        if (userId.isBlank()) return false
        return profileUiPrefs.getBoolean("hint_dismissed_$userId", false)
    }

    private fun markProfileHintDismissed(userId: String) {
        if (userId.isBlank()) return
        profileUiPrefs.edit().putBoolean("hint_dismissed_$userId", true).apply()
    }

    private fun showEditProfileDialog() {
        currentUser?.let {
            EditProfileBottomSheet(it).show(parentFragmentManager, EditProfileBottomSheet.TAG)
        }
    }

    private fun checkLocationPermission() {
        if (com.example.matchmyskills.util.LocationHelper.isLocationPermissionGranted(requireContext())) {
            fetchLocation()
        } else {
            requestLocationPermission.launch(com.example.matchmyskills.util.LocationHelper.getRequiredPermissions()[0])
        }
    }

    private fun fetchLocation() {
        com.example.matchmyskills.util.LocationHelper.fetchLocation(requireContext(), object : com.example.matchmyskills.util.LocationHelper.LocationCallback {
            override fun onLocationFetched(city: String, state: String) {
                val loc = "$city, $state"
                binding.tvLocation.text = loc
                // Update profile with detected location
                viewModel.updateProfile(mapOf("location" to loc))
            }

            override fun onLocationError(message: String) {
                binding.tvLocation.text = "Location unavailable"
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
