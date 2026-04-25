package com.example.matchmyskills.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.ItemUserNotificationBinding
import com.example.matchmyskills.model.UserNotification
import android.text.format.DateUtils
import java.util.Date

class UserNotificationAdapter(
    private val onNotificationClick: (UserNotification) -> Unit,
    private val onNotificationLongClick: (UserNotification) -> Unit
) : ListAdapter<UserNotification, UserNotificationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemUserNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: UserNotification) {
            binding.apply {
                tvNotifMessage.text = notification.message
                
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    notification.timestamp?.time ?: Date().time,
                    Date().time,
                    DateUtils.MINUTE_IN_MILLIS
                )
                tvNotifTime.text = timeAgo
                
                unreadDot.visibility = if (notification.isRead) android.view.View.GONE else android.view.View.VISIBLE
                
                ivNotifType.setImageResource(
                    when (notification.type.lowercase()) {
                        "shortlisted" -> R.drawable.ic_jobs
                        "rejected" -> R.drawable.ic_dashboard
                        else -> R.drawable.ic_dashboard
                    }
                )

                root.setOnClickListener { onNotificationClick(notification) }
                root.setOnLongClickListener {
                    onNotificationLongClick(notification)
                    true
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UserNotification>() {
        override fun areItemsTheSame(oldItem: UserNotification, newItem: UserNotification) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UserNotification, newItem: UserNotification) =
            oldItem == newItem
    }
}
