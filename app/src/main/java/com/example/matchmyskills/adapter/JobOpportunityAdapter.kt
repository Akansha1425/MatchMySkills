package com.example.matchmyskills.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.R
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.util.MatchingEngine

class JobOpportunityAdapter(
    private var jobs: List<Job>,
    private var candidateSkills: List<String> = emptyList(),
    private val onClick: (Job) -> Unit
) : RecyclerView.Adapter<JobOpportunityAdapter.JobViewHolder>() {

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJobTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvJobCompany: TextView = itemView.findViewById(R.id.tvJobCompany)
        val tvJobLocation: TextView = itemView.findViewById(R.id.tvJobLocation)
        val tvJobSkills: TextView = itemView.findViewById(R.id.tvJobSkills)
        val tvJobStipend: TextView = itemView.findViewById(R.id.tvJobStipend)
        val tvTimeLeft: TextView = itemView.findViewById(R.id.tvTimeLeft)
        val tvJobType: TextView = itemView.findViewById(R.id.tvJobType)
        val tvMatchScore: TextView = itemView.findViewById(R.id.tvMatchScore)
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

        // Set Type Label
        val type = job.opportunityType
        holder.tvJobType.text = if (type.equals("INTERNSHIP", ignoreCase = true)) "Internship" else "Job"
        if (type.equals("INTERNSHIP", ignoreCase = true)) {
            holder.tvJobType.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
            holder.tvJobType.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            holder.tvJobType.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E3F2FD"))
            holder.tvJobType.setTextColor(android.graphics.Color.parseColor("#2196F3"))
        }

        // Calculate and display Match Score if candidate skills are available
        if (candidateSkills.isNotEmpty()) {
            val application = MatchingEngine.calculateMatchScore(candidateSkills, job)
            val score = application.matchScore
            holder.tvMatchScore.text = "${score.toInt()}% Match"
            holder.tvMatchScore.visibility = View.VISIBLE
            
            val scoreColor = MatchingEngine.getScoreColor(score)
            holder.tvMatchScore.setTextColor(Color.parseColor(scoreColor))
            
            // Subtle background for the score tag based on score
            val bgColor = when {
                score >= 90.0 -> "#E8F5E9" // Light Green
                score >= 70.0 -> "#FFFDE7" // Light Yellow
                else -> "#FFEBEE" // Light Red
            }
            holder.tvMatchScore.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(bgColor))
        } else {
            holder.tvMatchScore.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onClick(job)
        }
    }

    override fun getItemCount(): Int = jobs.size

    fun updateData(newData: List<Job>, skills: List<String> = candidateSkills) {
        jobs = newData
        candidateSkills = skills
        notifyDataSetChanged()
    }
}
