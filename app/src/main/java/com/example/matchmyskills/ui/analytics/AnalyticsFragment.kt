package com.example.matchmyskills.ui.analytics

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentAnalyticsBinding
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.AnalyticsViewModel
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class AnalyticsFragment : Fragment(R.layout.fragment_analytics) {

    private val viewModel: AnalyticsViewModel by viewModels()
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalyticsBinding.bind(view)

        setupCharts()
        observeState()
        viewModel.fetchAnalytics()
    }

    private fun setupCharts() {
        binding.barChart.description.isEnabled = false
        binding.pieChart.description.isEnabled = false
        binding.pieChart.isDrawHoleEnabled = true
        binding.pieChart.setHoleColor(android.R.color.transparent)
    }

    private fun observeState() {
        viewModel.analyticsData.onEach { state ->
            when (state) {
                is UiState.Success -> {
                    updateBarChart(state.data)
                    updatePieChart(state.data)
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateBarChart(apps: List<com.example.matchmyskills.model.Application>) {
        val entries = ArrayList<BarEntry>()
        // Simplified: group by day/match tier for production
        entries.add(BarEntry(1f, 10f))
        entries.add(BarEntry(2f, 20f))
        entries.add(BarEntry(3f, 15f))

        val dataSet = BarDataSet(entries, "Applications")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        binding.barChart.data = BarData(dataSet)
        binding.barChart.invalidate()
    }

    private fun updatePieChart(apps: List<com.example.matchmyskills.model.Application>) {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(40f, "Shortlisted"))
        entries.add(PieEntry(30f, "Pending"))
        entries.add(PieEntry(30f, "Rejected"))

        val dataSet = PieDataSet(entries, "Status")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
