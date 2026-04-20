package com.example.matchmyskills.ui.jobs

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentCreateJobBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateJobFragment : Fragment(R.layout.fragment_create_job) {

    private var _binding: FragmentCreateJobBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedDeadline: Date? = null
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateJobBinding.bind(view)

        setupDropdowns()
        setupDatePicker()
        setupListeners()
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

            val skillList = skills.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val valid = validateInputs(
                jobTitle = jobTitle,
                companyName = companyName,
                description = description,
                workMode = workMode,
                location = location,
                experience = experience,
                skillList = skillList,
                jobFunction = jobFunction,
                employmentType = employmentType,
                deadline = selectedDeadline
            )

            if (!valid) return@setOnClickListener

            saveJob(
                jobTitle = jobTitle,
                companyName = companyName,
                description = description,
                workMode = workMode,
                location = location,
                experience = experience,
                skills = skillList,
                jobFunction = jobFunction,
                employmentType = employmentType,
                salary = salary,
                deadline = selectedDeadline!!
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
        skillList: List<String>,
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
        if (skillList.isEmpty()) {
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

    private fun saveJob(
        jobTitle: String,
        companyName: String,
        description: String,
        workMode: String,
        location: String,
        experience: String,
        skills: List<String>,
        jobFunction: String,
        employmentType: String,
        salary: String,
        deadline: Date
    ) {
        setLoading(true)

        val recruiterId = auth.currentUser?.uid.orEmpty()
        val job = hashMapOf(
            "jobTitle" to jobTitle,
            "title" to jobTitle,
            "companyName" to companyName,
            "description" to description,
            "workMode" to workMode,
            "location" to location,
            "city" to location,
            "experience" to experience,
            "skills" to skills,
            "coreSkills" to skills,
            "jobFunction" to jobFunction,
            "employmentType" to employmentType,
            "salary" to salary,
            "stipend" to salary,
            "deadline" to deadline,
            "status" to "Active",
            "opportunityType" to "JOB",
            "source" to "FIREBASE",
            "recruiterId" to recruiterId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        Log.d("CreateJobFragment", "Saving job: $job")

        db.collection("jobs")
            .add(job)
            .addOnSuccessListener { documentReference ->
                Log.d("CreateJobFragment", "Job saved successfully with id=${documentReference.id}")
                Toast.makeText(requireContext(), "Job posted successfully", Toast.LENGTH_SHORT).show()
                setLoading(false)
                findNavController().popBackStack()
            }
            .addOnFailureListener { exception ->
                Log.e("CreateJobFragment", "Failed to save job", exception)
                setLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Failed to post job: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSubmit.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
