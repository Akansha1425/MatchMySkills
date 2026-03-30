package com.example.matchmyskills

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.matchmyskills.databinding.ActivityOnboardingBinding
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnComplete.setOnClickListener {
            val companyName = binding.etCompanyName.text.toString().trim()
            val industry = binding.etIndustry.text.toString().trim()
            val website = binding.etWebsite.text.toString().trim()

            if (companyName.isNotEmpty() && industry.isNotEmpty()) {
                viewModel.completeOnboarding(companyName, industry, website)
            } else {
                Toast.makeText(this, "Company name and industry are required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeState() {
        viewModel.onboardingState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnComplete.isEnabled = false
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    
                    // Navigate to MainActivity and clear back stack
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnComplete.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }.launchIn(lifecycleScope)
    }
}
