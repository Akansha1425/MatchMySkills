package com.example.matchmyskills.ui.candidates

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.databinding.ItemApplicantBinding
import com.example.matchmyskills.model.Application
import com.example.matchmyskills.util.MatchingEngine

class ApplicantAdapter(private val onApplicantClick: (Application) -> Unit) :
    ListAdapter<Application, ApplicantAdapter.ApplicantViewHolder>(ApplicationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
        val binding = ItemApplicantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApplicantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ApplicantViewHolder(private val binding: ItemApplicantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: Application) {
            binding.apply {
                tvName.text = app.candidateName
                tvCollege.text = app.candidateCollege
                tvMatchScore.text = "${app.matchScore.toInt()}%"
                tvMatchScore.setTextColor(Color.parseColor(MatchingEngine.getScoreColor(app.matchScore)))
                chipStatus.text = app.status
                
                root.setOnClickListener { onApplicantClick(app) }
            }
        }
    }

    class ApplicationDiffCallback : DiffUtil.ItemCallback<Application>() {
        override fun areItemsTheSame(oldItem: Application, newItem: Application): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Application, newItem: Application): Boolean = oldItem == newItem
    }
}
