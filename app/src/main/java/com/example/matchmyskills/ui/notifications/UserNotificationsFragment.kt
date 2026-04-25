package com.example.matchmyskills.ui.notifications

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentNotificationsBinding
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.UserNotificationViewModel
import com.example.matchmyskills.JobDetailActivity
import com.example.matchmyskills.OpportunityDetailActivity
import com.example.matchmyskills.util.toJob
import com.example.matchmyskills.util.toHackathon
import com.example.matchmyskills.model.UserNotification
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.os.Parcelable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class UserNotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserNotificationViewModel by viewModels()
    private lateinit var adapter: UserNotificationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationsBinding.bind(view)

        setupRecyclerView()
        observeNotifications()
        
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.title = "Notifications"
    }

    private fun setupRecyclerView() {
        adapter = UserNotificationAdapter(
            onNotificationClick = { notification ->
                if (!notification.isRead) {
                    viewModel.markAsRead(notification.id)
                }
                handleNotificationClick(notification)
            },
            onNotificationLongClick = { notification ->
                showActionDialog(notification.id)
            }
        )
        binding.rvNotifications.layoutManager = LinearLayoutManager(context)
        binding.rvNotifications.adapter = adapter
    }

    private fun observeNotifications() {
        viewModel.notificationsState.onEach { state ->
            when (state) {
                is UiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    adapter.submitList(state.data)
                }
                is UiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    adapter.submitList(emptyList())
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun showActionDialog(notificationId: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Notification Options")
            .setItems(arrayOf("Delete")) { _, which ->
                if (which == 0) viewModel.deleteNotification(notificationId)
            }
            .show()
    }

    private fun handleNotificationClick(notification: UserNotification) {
        val opportunityId = notification.opportunityId
        if (opportunityId.isBlank()) return

        binding.progressBar.visibility = View.VISIBLE
        val firestore = FirebaseFirestore.getInstance()

        if (notification.opportunityType.equals("HACKATHON", ignoreCase = true)) {
            firestore.collection("hackathons").document(opportunityId).get()
                .addOnSuccessListener { doc ->
                    binding.progressBar.visibility = View.GONE
                    val hackathon = doc.toHackathon()
                    if (hackathon != null) {
                        val intent = Intent(requireContext(), OpportunityDetailActivity::class.java).apply {
                            putExtra("EXTRA_HACKATHON", hackathon as Parcelable)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "Hackathon details not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load hackathon details", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Default to JOB/INTERNSHIP
            firestore.collection("jobs").document(opportunityId).get()
                .addOnSuccessListener { doc ->
                    binding.progressBar.visibility = View.GONE
                    val job = doc.toJob()
                    if (job != null) {
                        val intent = Intent(requireContext(), JobDetailActivity::class.java).apply {
                            putExtra("EXTRA_JOB", job as Parcelable)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "Opportunity details not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load opportunity details", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
