package com.example.matchmyskills.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.R
import com.example.matchmyskills.model.Job

class JobOpportunityAdapter(
    private var jobs: List<Job>,
    private val onClick: (Job) -> Unit
) : RecyclerView.Adapter<JobOpportunityAdapter.JobViewHolder>() {

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJobTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvJobCompany: TextView = itemView.findViewById(R.id.tvJobCompany)
        val tvJobLocation: TextView = itemView.findViewById(R.id.tvJobLocation)
        val tvJobSkills: TextView = itemView.findViewById(R.id.tvJobSkills)
        val tvJobStipend: TextView = itemView.findViewById(R.id.tvJobStipend)
        val tvTimeLeft: TextView = itemView.findViewById(R.id.tvTimeLeft)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job_opportunity, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        holder.tvJobTitle.text = job.title

        holder.tvJobCompany.text = if (job.companyName.isBlank() || job.companyName.equals("Unknown", ignoreCase = true)) {
            "Posted by Recruiter"
        } else {
            job.companyName
        }

        holder.tvJobLocation.text = job.location.ifBlank { "Remote" }
        val skillText = job.coreSkills.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Not specified"
        holder.tvJobSkills.text = "Skills: $skillText"
        holder.tvJobSkills.visibility = if (skillText == "Not specified") View.GONE else View.VISIBLE
        holder.tvJobStipend.text = if (job.stipend.isBlank()) "Salary: Not shared" else "Salary: ${job.stipend}"

        val deadline = job.deadline
        if (deadline != null) {
            val currentTime = System.currentTimeMillis()
            val diff = deadline.time - currentTime
            val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt()

            holder.tvTimeLeft.text = when {
                daysLeft > 0 -> "$daysLeft days left"
                daysLeft == 0 -> "Ends today"
                else -> "Expired"
            }
            holder.tvTimeLeft.visibility = View.VISIBLE
        } else {
            holder.tvTimeLeft.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onClick(job)
        }
    }

    override fun getItemCount(): Int = jobs.size

    fun updateData(newData: List<Job>) {
        jobs = newData
        notifyDataSetChanged()
    }
}
