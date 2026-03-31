package com.example.matchmyskills.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.R
import com.example.matchmyskills.model.Hackathon

class HackathonAdapter(
    private var hackathons: List<Hackathon>,
    private val onClick: (Hackathon) -> Unit
) : RecyclerView.Adapter<HackathonAdapter.HackathonViewHolder>() {

    class HackathonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvOrganizer: TextView = itemView.findViewById(R.id.tvOrganizer)
        val tvPrizePool: TextView = itemView.findViewById(R.id.tvPrizePool)
        val tvMode: TextView = itemView.findViewById(R.id.tvMode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HackathonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_opportunity, parent, false)
        return HackathonViewHolder(view)
    }

    override fun onBindViewHolder(holder: HackathonViewHolder, position: Int) {
        val hackathon = hackathons[position]
        holder.tvTitle.text = hackathon.title
        holder.tvOrganizer.text = hackathon.organizer
        holder.tvPrizePool.text = "Prize: ${hackathon.prizePool}"
        holder.tvMode.text = hackathon.mode

        holder.itemView.setOnClickListener {
            onClick(hackathon)
        }
    }

    override fun getItemCount(): Int = hackathons.size

    fun updateData(newData: List<Hackathon>) {
        hackathons = newData
        notifyDataSetChanged()
    }
}
