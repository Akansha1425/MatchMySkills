package com.example.matchmyskills.ui.notifications

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.matchmyskills.databinding.ItemNotificationBinding
import com.example.matchmyskills.model.RecruiterNotification

class NotificationAdapter(
    private val onNotificationClick: (RecruiterNotification) -> Unit,
    private val onNotificationLongClick: (RecruiterNotification) -> Unit
) : ListAdapter<RecruiterNotification, NotificationAdapter.NotificationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return NotificationViewHolder(ItemNotificationBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecruiterNotification) {
            binding.tvMessage.text = item.message

            binding.tvTime.text = item.timestamp?.let { timestamp ->
                DateUtils.getRelativeTimeSpanString(
                    timestamp.time,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
            } ?: "Just now"

            binding.viewUnreadDot.visibility = if (item.isRead) View.INVISIBLE else View.VISIBLE

            binding.root.setOnClickListener {
                onNotificationClick(item)
            }

            binding.root.setOnLongClickListener {
                onNotificationLongClick(item)
                true
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecruiterNotification>() {
        override fun areItemsTheSame(oldItem: RecruiterNotification, newItem: RecruiterNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecruiterNotification, newItem: RecruiterNotification): Boolean {
            return oldItem == newItem
        }
    }
}
