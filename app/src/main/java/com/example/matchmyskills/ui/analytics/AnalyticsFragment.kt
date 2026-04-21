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
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.model.Hackathon
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
                    updatePieChart(state.data.applications)
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateBarChart(data: AnalyticsViewModel.AnalyticsData) {
        val hackathonCount = data.hackathons.size
        val internshipCount = data.jobs.count { it.opportunityType.equals("INTERNSHIP", ignoreCase = true) }
        val jobCount = data.jobs.count { it.opportunityType.equals("JOB", ignoreCase = true) }

        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, hackathonCount.toFloat()))
        entries.add(BarEntry(1f, internshipCount.toFloat()))
        entries.add(BarEntry(2f, jobCount.toFloat()))

        val dataSet = BarDataSet(entries, "Posted Opportunities")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        
        binding.barChart.data = BarData(dataSet)
        binding.barChart.xAxis.apply {
            valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(listOf("Hackathons", "Internships", "Jobs"))
            position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            labelCount = 3
        }
        binding.barChart.invalidate()
    }

    private fun updatePieChart(apps: List<com.example.matchmyskills.model.Application>) {
        val total = apps.size.toFloat()
        if (total == 0f) {
            binding.pieChart.clear()
            return
        }

        val shortlisted = apps.count { it.status == "Shortlisted" }.toFloat()
        val hired = apps.count { it.status == "Hired" }.toFloat()
        val rejected = apps.count { it.status == "Rejected" }.toFloat()
        val pending = apps.count { it.status == "Pending" || it.status == "Applied" }.toFloat()

        val entries = ArrayList<PieEntry>()
        
        // Calculate based on true total of all applications
        if (shortlisted > 0) entries.add(PieEntry((shortlisted / total) * 100f, "Shortlisted"))
        if (hired > 0) entries.add(PieEntry((hired / total) * 100f, "Hired"))
        if (rejected > 0) entries.add(PieEntry((rejected / total) * 100f, "Rejected"))
        if (pending > 0) entries.add(PieEntry((pending / total) * 100f, "Pending"))

        val dataSet = PieDataSet(entries, "Status Distribution")
        dataSet.colors = listOf(
            android.graphics.Color.parseColor("#4CAF50"), // Green for Shortlisted
            android.graphics.Color.parseColor("#2196F3"), // Blue for Hired
            android.graphics.Color.parseColor("#F44336"), // Red for Rejected
            android.graphics.Color.parseColor("#FFC107")  // Amber for Pending
        )
        dataSet.sliceSpace = 3f
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = android.graphics.Color.WHITE
        
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.setUsePercentValues(true)
        binding.pieChart.centerText = "Total\n${apps.size}"
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
