package com.example.matchmyskills.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.BottomSheetCreateOpportunityBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetCreateOpportunity : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateOpportunityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetCreateOpportunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardInternship.setOnClickListener {
            Log.d("CREATE_OPPORTUNITY", "Post Internship clicked")
            val navController = findNavController()
            dismiss()
            navController.navigate(R.id.action_dashboardFragment_to_createInternshipFragment)
        }
        binding.ivInternshipArrow.setOnClickListener { binding.cardInternship.performClick() }

        binding.cardHackathon.setOnClickListener {
            Log.d("CREATE_OPPORTUNITY", "Organize Hackathon clicked")
            val navController = findNavController()
            dismiss()
            navController.navigate(R.id.action_dashboardFragment_to_createHackathonFragment)
        }
        binding.ivHackathonArrow.setOnClickListener { binding.cardHackathon.performClick() }

        binding.cardJob.setOnClickListener {
            Log.d("CREATE_OPPORTUNITY", "Post Job clicked")
            val navController = findNavController()
            dismiss()
            navController.navigate(R.id.createJobFragment)
        }
        binding.ivJobArrow.setOnClickListener { binding.cardJob.performClick() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BottomSheetCreateOpportunity"
    }
}
