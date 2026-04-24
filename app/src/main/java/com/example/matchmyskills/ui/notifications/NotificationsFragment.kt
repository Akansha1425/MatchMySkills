package com.example.matchmyskills.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.matchmyskills.R
import com.example.matchmyskills.databinding.FragmentNotificationsBinding
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationsBinding.bind(view)

        setupToolbar()
        setupRecyclerView()
        observeNotifications()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onNotificationClick = { notification ->
            if (notification.jobId.isBlank()) {
                Toast.makeText(requireContext(), "Opportunity is no longer available", Toast.LENGTH_SHORT).show()
                return@NotificationAdapter
            }

            navigateToCandidates(notification.jobId)
            },
            onNotificationLongClick = { notification ->
                showNotificationActionDialog(notification.id, notification.isRead)
            }
        )
        binding.rvNotifications.layoutManager = LinearLayoutManager(context)
        binding.rvNotifications.adapter = adapter
    }

    private fun showNotificationActionDialog(notificationId: String, isRead: Boolean) {
        val actions = if (isRead) {
            arrayOf("Delete")
        } else {
            arrayOf("Mark as read", "Delete")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Notification")
            .setItems(actions) { _, which ->
                when {
                    !isRead && which == 0 -> viewModel.markAsRead(notificationId)
                    !isRead && which == 1 -> viewModel.deleteNotification(notificationId)
                    isRead && which == 0 -> viewModel.deleteNotification(notificationId)
                }
            }
            .show()
    }

    private fun navigateToCandidates(jobId: String) {
        val navController = findNavController()
        val canNavigate = navController.currentDestination?.id == R.id.notificationsFragment
        if (!canNavigate) return

        runCatching {
            navController.navigate(
                R.id.candidatesFragment,
                bundleOf("jobId" to jobId)
            )
        }.onFailure { error ->
            Log.e("NotificationsFragment", "Navigation failed for jobId=$jobId", error)
            Toast.makeText(requireContext(), "Unable to open applicants right now", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeNotifications() {
        viewModel.notificationsState.onEach { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }

                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(state.data)
                    binding.tvEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                }

                is UiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(emptyList())
                    binding.tvEmpty.visibility = View.VISIBLE
                }

                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = state.message
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
