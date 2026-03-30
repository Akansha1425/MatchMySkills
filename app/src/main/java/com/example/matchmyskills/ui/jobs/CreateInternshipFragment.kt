package com.example.matchmyskills.ui.jobs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentCreateInternshipBinding
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.OpportunityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class CreateInternshipFragment : Fragment(R.layout.fragment_create_internship) {
    private val viewModel: OpportunityViewModel by viewModels()
    private var _binding: FragmentCreateInternshipBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateInternshipBinding.bind(view)

        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val skills = binding.etSkills.text.toString().trim()
            val duration = binding.etDuration.text.toString().trim()
            val stipend = binding.etStipend.text.toString().trim()
            val deadline = binding.etDeadline.text.toString().toIntOrNull() ?: 0

            if (title.isEmpty() || description.isEmpty() || skills.isEmpty() || duration.isEmpty()) {
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createInternship(title, description, skills, duration, stipend, deadline)
        }

        observeState()
    }

    private fun observeState() {
        viewModel.createState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.btnSubmit.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    Toast.makeText(context, "Internship posted successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is UiState.Error -> {
                    binding.btnSubmit.isEnabled = true
                    binding.progressBar.visibility = View.GONE
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
