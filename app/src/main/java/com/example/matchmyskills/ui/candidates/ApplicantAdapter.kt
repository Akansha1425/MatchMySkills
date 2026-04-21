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
                
                // Fetch and load profile image
                loadCandidateImage(app.candidateId)
                
                root.setOnClickListener { onApplicantClick(app) }
            }
        }

        private fun loadCandidateImage(candidateId: String) {
            binding.ivAvatar.setTag(com.example.matchmyskills.R.id.iv_avatar, candidateId)
            
            if (candidateId.isNullOrBlank()) {
                binding.ivAvatar.setImageResource(com.example.matchmyskills.R.drawable.ic_profile)
                return
            }

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(candidateId)
                .get()
                .addOnSuccessListener { doc ->
                    // Check if this view is still meant for the same candidate
                    if (binding.ivAvatar.getTag(com.example.matchmyskills.R.id.iv_avatar) != candidateId) return@addOnSuccessListener

                    val imageUrl = doc.getString("profileImage") ?: doc.getString("profileImageUrl")
                    if (!imageUrl.isNullOrBlank()) {
                        com.bumptech.glide.Glide.with(binding.ivAvatar.context)
                            .load(imageUrl)
                            .circleCrop()
                            .placeholder(com.example.matchmyskills.R.drawable.ic_profile)
                            .error(com.example.matchmyskills.R.drawable.ic_profile)
                            .into(binding.ivAvatar)
                    } else {
                        binding.ivAvatar.setImageResource(com.example.matchmyskills.R.drawable.ic_profile)
                    }
                }
                .addOnFailureListener {
                    if (binding.ivAvatar.getTag(com.example.matchmyskills.R.id.iv_avatar) == candidateId) {
                        binding.ivAvatar.setImageResource(com.example.matchmyskills.R.drawable.ic_profile)
                    }
                }
        }
    }

    class ApplicationDiffCallback : DiffUtil.ItemCallback<Application>() {
        override fun areItemsTheSame(oldItem: Application, newItem: Application): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Application, newItem: Application): Boolean = oldItem == newItem
    }
}
