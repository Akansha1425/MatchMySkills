package com.example.matchmyskills.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.matchmyskills.databinding.BottomSheetEditProfileBinding
import com.example.matchmyskills.model.User
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.AuthViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class EditProfileBottomSheet(private val user: User) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupFields()
        setupListeners()
        observeUpdateState()
    }

    private fun setupFields() {
        binding.etName.setText(user.name)
        binding.etCompanyName.setText(user.companyName)
        binding.etJobTitle.setText(user.jobTitle)
        binding.etBio.setText(user.bio)
        binding.etLocation.setText(user.location)
        binding.etLinkedin.setText(user.linkedin)
        binding.etPhone.setText(user.phone)

        val sizes = arrayOf("1-10", "11-50", "51-200", "201-500", "501-1000", "1001-5000", "5001-10000", "10000+")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sizes)
        binding.etCompanySize.setAdapter(adapter)
        binding.etCompanySize.setText(user.companySize, false)
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val updates = mapOf(
                "name" to binding.etName.text.toString(),
                "companyName" to binding.etCompanyName.text.toString(),
                "jobTitle" to binding.etJobTitle.text.toString(),
                "bio" to binding.etBio.text.toString(),
                "location" to binding.etLocation.text.toString(),
                "companySize" to binding.etCompanySize.text.toString(),
                "linkedin" to binding.etLinkedin.text.toString(),
                "phone" to binding.etPhone.text.toString()
            )
            viewModel.updateProfile(updates)
        }
    }

    private fun observeUpdateState() {
        viewModel.updateState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.btnSave.isEnabled = false
                    binding.progressBar.isVisible = true
                }
                is UiState.Success -> {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                    dismiss()
                }
                is UiState.Error -> {
                    binding.btnSave.isEnabled = true
                    binding.progressBar.isVisible = false
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditProfileBottomSheet"
    }
}
