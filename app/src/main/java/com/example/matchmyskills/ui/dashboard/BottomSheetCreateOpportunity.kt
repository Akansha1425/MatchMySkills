package com.example.matchmyskills.ui.dashboard

import android.os.Bundle
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
            dismiss()
            findNavController().navigate(R.id.action_dashboardFragment_to_createInternshipFragment)
        }

        binding.cardHackathon.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.action_dashboardFragment_to_createHackathonFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BottomSheetCreateOpportunity"
    }
}
