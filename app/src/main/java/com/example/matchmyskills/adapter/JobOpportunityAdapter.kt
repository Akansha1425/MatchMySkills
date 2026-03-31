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
        val tvJobStipend: TextView = itemView.findViewById(R.id.tvJobStipend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job_opportunity, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        holder.tvJobTitle.text = job.title
        holder.tvJobCompany.text = job.companyName
        holder.tvJobLocation.text = job.location
        holder.tvJobStipend.text = "Stipend: ${job.stipend}"

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
