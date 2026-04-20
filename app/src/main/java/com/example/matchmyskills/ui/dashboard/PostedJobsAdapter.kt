package com.example.matchmyskills.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.databinding.ItemJobOpportunityBinding
import com.example.matchmyskills.model.JobOpportunity

class PostedJobsAdapter(
    private val onItemClick: (JobOpportunity) -> Unit
) : ListAdapter<JobOpportunity, PostedJobsAdapter.PostedJobViewHolder>(PostedJobDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostedJobViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return PostedJobViewHolder(ItemJobOpportunityBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: PostedJobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostedJobViewHolder(private val binding: ItemJobOpportunityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(job: JobOpportunity) {
            binding.tvJobTitle.text = job.jobTitle
            binding.tvJobCompany.text = job.companyName
            val mode = job.workMode.ifBlank { "On-site" }
            val location = job.location.ifBlank { "Location not specified" }
            binding.tvJobLocation.text = "$mode • $location"
            binding.tvJobStipend.text = if (job.salary.isBlank()) {
                "Salary: Not disclosed"
            } else {
                "Salary: ${job.salary}"
            }

            job.deadline?.let { deadline ->
                val diff = deadline.time - System.currentTimeMillis()
                val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt()
                binding.tvTimeLeft.text = when {
                    daysLeft > 0 -> "$daysLeft days left"
                    daysLeft == 0 -> "Ends today"
                    else -> "Expired"
                }
                binding.tvTimeLeft.visibility = View.VISIBLE
            } ?: run {
                binding.tvTimeLeft.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(job) }
        }
    }

    class PostedJobDiffCallback : DiffUtil.ItemCallback<JobOpportunity>() {
        override fun areItemsTheSame(oldItem: JobOpportunity, newItem: JobOpportunity): Boolean {
            return oldItem.jobId == newItem.jobId
        }

        override fun areContentsTheSame(oldItem: JobOpportunity, newItem: JobOpportunity): Boolean {
            return oldItem == newItem
        }
    }
}
