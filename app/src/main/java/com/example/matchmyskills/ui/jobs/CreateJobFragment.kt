package com.example.matchmyskills.ui.jobs

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentCreateJobBinding
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.OpportunityViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateJobFragment : Fragment(R.layout.fragment_create_job) {
    private val viewModel: OpportunityViewModel by viewModels()
    private var _binding: FragmentCreateJobBinding? = null
    private val binding get() = _binding!!

    private var selectedDeadline: Date? = null
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateJobBinding.bind(view)

        setupDropdowns()
        setupDatePicker()
        setupListeners()
        observeState()
    }

    private fun setupDropdowns() {
        val workModes = arrayOf("Remote", "On-site", "Hybrid")
        binding.actWorkMode.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, workModes)
        )

        val experiences = arrayOf("Fresher", "1-2 yrs", "3+ yrs")
        binding.actExperience.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, experiences)
        )

        val employmentTypes = arrayOf("Full-time", "Part-time", "Contract", "Internship")
        binding.actEmploymentType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, employmentTypes)
        )
    }

    private fun setupDatePicker() {
        val openPicker = {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select application deadline")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { millis ->
                selectedDeadline = Date(millis)
                binding.etDeadline.setText(dateFormat.format(selectedDeadline!!))
                binding.tilDeadline.error = null
            }

            picker.show(childFragmentManager, "job_deadline_picker")
        }

        binding.etDeadline.setOnClickListener { openPicker() }
        binding.tilDeadline.setEndIconOnClickListener { openPicker() }
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            clearErrors()

            val jobTitle = binding.etJobTitle.text.toString().trim()
            val companyName = binding.etCompanyName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val workMode = binding.actWorkMode.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val experience = binding.actExperience.text.toString().trim()
            val skills = binding.etSkills.text.toString().trim()
            val jobFunction = binding.etJobFunction.text.toString().trim()
            val employmentType = binding.actEmploymentType.text.toString().trim()
            val salary = binding.etSalary.text.toString().trim()

            val valid = validateInputs(
                jobTitle = jobTitle,
                companyName = companyName,
                description = description,
                workMode = workMode,
                location = location,
                experience = experience,
                skills = skills,
                jobFunction = jobFunction,
                employmentType = employmentType,
                deadline = selectedDeadline
            )

            if (!valid) return@setOnClickListener

            viewModel.createJobOpportunity(
                jobTitle = jobTitle,
                companyName = companyName,
                description = description,
                workMode = workMode,
                location = location,
                experience = experience,
                skills = skills,
                jobFunction = jobFunction,
                employmentType = employmentType,
                salary = salary,
                deadline = selectedDeadline
            )
        }
    }

    private fun validateInputs(
        jobTitle: String,
        companyName: String,
        description: String,
        workMode: String,
        location: String,
        experience: String,
        skills: String,
        jobFunction: String,
        employmentType: String,
        deadline: Date?
    ): Boolean {
        var valid = true

        if (jobTitle.isBlank()) {
            binding.tilJobTitle.error = "Job title is required"
            valid = false
        }
        if (companyName.isBlank()) {
            binding.tilCompanyName.error = "Company name is required"
            valid = false
        }
        if (description.isBlank()) {
            binding.tilDescription.error = "Description is required"
            valid = false
        }
        if (workMode.isBlank()) {
            binding.tilWorkMode.error = "Work mode is required"
            valid = false
        }
        if (location.isBlank()) {
            binding.tilLocation.error = "Location is required"
            valid = false
        }
        if (experience.isBlank()) {
            binding.tilExperience.error = "Experience is required"
            valid = false
        }
        if (skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }.isEmpty()) {
            binding.tilSkills.error = "At least one skill is required"
            valid = false
        }
        if (jobFunction.isBlank()) {
            binding.tilJobFunction.error = "Job function is required"
            valid = false
        }
        if (employmentType.isBlank()) {
            binding.tilEmploymentType.error = "Employment type is required"
            valid = false
        }
        if (deadline == null) {
            binding.tilDeadline.error = "Deadline is required"
            valid = false
        }

        return valid
    }

    private fun clearErrors() {
        binding.tilJobTitle.error = null
        binding.tilCompanyName.error = null
        binding.tilDescription.error = null
        binding.tilWorkMode.error = null
        binding.tilLocation.error = null
        binding.tilExperience.error = null
        binding.tilSkills.error = null
        binding.tilJobFunction.error = null
        binding.tilEmploymentType.error = null
        binding.tilDeadline.error = null
    }

    private fun observeState() {
        viewModel.createState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.btnSubmit.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.btnSubmit.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Job posted successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                    findNavController().popBackStack()
                }
                is UiState.Error -> {
                    binding.btnSubmit.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.btnSubmit.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
