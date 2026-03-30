package com.example.matchmyskills.ui.jobs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentHackathonDetailBinding

class HackathonDetailFragment : Fragment(R.layout.fragment_hackathon_detail) {

    private var _binding: FragmentHackathonDetailBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHackathonDetailBinding.bind(view)

        val hackathonId = arguments?.getString("hackathonId")
        binding.tvHackathonId.text = "Hackathon ID: $hackathonId"
        binding.tvPlaceholder.text = "Details and applicant management coming soon..."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
