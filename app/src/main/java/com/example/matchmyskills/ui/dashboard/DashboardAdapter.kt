package com.example.matchmyskills.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.databinding.ItemHackathonBinding
import com.example.matchmyskills.databinding.ItemJobBinding
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.model.Job

sealed class DashboardItem {
    data class JobItem(val job: Job) : DashboardItem()
    data class HackathonItem(val hackathon: Hackathon) : DashboardItem()
}

class DashboardAdapter(
    private val onJobClick: (Job) -> Unit,
    private val onHackathonClick: (Hackathon) -> Unit
) : ListAdapter<DashboardItem, RecyclerView.ViewHolder>(DashboardItemDiffCallback()) {

    companion object {
        private const val TYPE_JOB = 1
        private const val TYPE_HACKATHON = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DashboardItem.JobItem -> TYPE_JOB
            is DashboardItem.HackathonItem -> TYPE_HACKATHON
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_JOB -> JobViewHolder(ItemJobBinding.inflate(inflater, parent, false))
            TYPE_HACKATHON -> HackathonViewHolder(ItemHackathonBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DashboardItem.JobItem -> (holder as JobViewHolder).bind(item.job)
            is DashboardItem.HackathonItem -> (holder as HackathonViewHolder).bind(item.hackathon)
        }
    }

    inner class JobViewHolder(private val binding: ItemJobBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(job: Job) {
            binding.apply {
                tvTitle.text = job.title
                tvCompany.text = job.companyName
                chipStatus.text = job.status
                tvApplicants.text = "View Applicants"
                btnView.setOnClickListener { onJobClick(job) }
                root.setOnClickListener { onJobClick(job) }
            }
        }
    }

    inner class HackathonViewHolder(private val binding: ItemHackathonBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(hackathon: Hackathon) {
            binding.apply {
                tvTitle.text = hackathon.title
                tvOrganizer.text = hackathon.organizer
                chipStatus.text = hackathon.status
                tvPrize.text = "Pool: ${hackathon.prizePool}"
                tvTeam.text = "Teams: ${hackathon.teamSize}"
                root.setOnClickListener { onHackathonClick(hackathon) }
            }
        }
    }

    class DashboardItemDiffCallback : DiffUtil.ItemCallback<DashboardItem>() {
        override fun areItemsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
            return when {
                oldItem is DashboardItem.JobItem && newItem is DashboardItem.JobItem -> oldItem.job.id == newItem.job.id
                oldItem is DashboardItem.HackathonItem && newItem is DashboardItem.HackathonItem -> oldItem.hackathon.id == newItem.hackathon.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DashboardItem, newItem: DashboardItem): Boolean {
            return oldItem == newItem
        }
    }
}
