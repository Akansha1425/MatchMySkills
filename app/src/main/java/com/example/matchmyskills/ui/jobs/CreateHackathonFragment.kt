package com.example.matchmyskills.ui.jobs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentCreateHackathonBinding
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.OpportunityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class CreateHackathonFragment : Fragment(R.layout.fragment_create_hackathon) {
    private val viewModel: OpportunityViewModel by viewModels()
    private var _binding: FragmentCreateHackathonBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateHackathonBinding.bind(view)

        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val themes = binding.etThemes.text.toString().trim()
            val prize = binding.etPrize.text.toString().trim()
            val teamSize = binding.etTeamSize.text.toString().trim()
            val mode = binding.etMode.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || themes.isEmpty()) {
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createHackathon(title, description, themes, prize, teamSize, mode)
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
                    Toast.makeText(context, "Hackathon created successfully!", Toast.LENGTH_SHORT).show()
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
