package com.example.matchmyskills.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.matchmyskills.data.remote.ExternalOpportunityDataSource
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.notifications.OpportunityNotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class OpportunitySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val forceDeadlineNotification = inputData.getBoolean(KEY_FORCE_DEADLINE_NOTIFICATION, false)

            val externalJobs = ExternalOpportunityDataSource.fetchJobs(keyword = "software", type = "JOB")
            val externalInternships = ExternalOpportunityDataSource.fetchJobs(keyword = "internship", type = "INTERNSHIP")
            val externalHackathons = ExternalOpportunityDataSource.fetchHackathons()

            val firebaseActiveJobs = fetchActiveFirebaseJobs()
            val firebaseActiveHackathons = fetchActiveFirebaseHackathons()

            val firebaseJobs = firebaseActiveJobs.filter { !isInternship(it) }
            val firebaseInternships = firebaseActiveJobs.filter { isInternship(it) }

            val knownIndex = readKnownIndex()

            val currentKeys = buildSet {
                externalJobs.forEach { add(keyFor("EXTERNAL", "JOB", it.id)) }
                externalInternships.forEach { add(keyFor("EXTERNAL", "INTERNSHIP", it.id)) }
                externalHackathons.forEach { add(keyFor("EXTERNAL", "HACKATHON", it.id)) }
                firebaseJobs.forEach { add(keyFor("FIREBASE", "JOB", it.id)) }
                firebaseInternships.forEach { add(keyFor("FIREBASE", "INTERNSHIP", it.id)) }
                firebaseActiveHackathons.forEach { add(keyFor("FIREBASE", "HACKATHON", it.id)) }
            }

            val shouldNotify = knownIndex.exactKeys.isNotEmpty() && !knownIndex.isLegacyFormat

            if (shouldNotify) {
                val newJobs = externalJobs.count {
                    !isKnown(knownIndex, keyFor("EXTERNAL", "JOB", it.id), it.id)
                } + firebaseJobs.count {
                    !isKnown(knownIndex, keyFor("FIREBASE", "JOB", it.id), it.id)
                }

                val newInternships = externalInternships.count {
                    !isKnown(knownIndex, keyFor("EXTERNAL", "INTERNSHIP", it.id), it.id)
                } + firebaseInternships.count {
                    !isKnown(knownIndex, keyFor("FIREBASE", "INTERNSHIP", it.id), it.id)
                }

                val newHackathons = externalHackathons.count {
                    !isKnown(knownIndex, keyFor("EXTERNAL", "HACKATHON", it.id), it.id)
                } + firebaseActiveHackathons.count {
                    !isKnown(knownIndex, keyFor("FIREBASE", "HACKATHON", it.id), it.id)
                }

                val totalNew = newJobs + newInternships + newHackathons

                if (totalNew > 0) {
                    OpportunityNotificationHelper.notifyNewOpportunities(
                        context = applicationContext,
                        totalNew = totalNew,
                        newJobs = newJobs,
                        newInternships = newInternships,
                        newHackathons = newHackathons
                    )
                }
            }

            notifyDeadlineAlerts(
                externalJobs = externalJobs,
                externalInternships = externalInternships,
                firebaseJobs = firebaseJobs,
                firebaseInternships = firebaseInternships,
                forceNotify = forceDeadlineNotification
            )

            writeKnownKeys(currentKeys)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    private fun notifyDeadlineAlerts(
        externalJobs: List<Job>,
        externalInternships: List<Job>,
        firebaseJobs: List<Job>,
        firebaseInternships: List<Job>,
        forceNotify: Boolean
    ) {
        val allJobs = externalJobs + firebaseJobs
        val allInternships = externalInternships + firebaseInternships

        val knownDeadlineKeys = readDeadlineAlertedKeys().toMutableSet()
        val currentWindowKeys = mutableSetOf<String>()

        val dueSoonJobs = allJobs.filter { job ->
            val key = keyFor(job.source.ifBlank { "UNKNOWN" }, "JOB", job.id)
            val dueSoon = hasLessThanTenDaysLeft(job)
            if (dueSoon) {
                currentWindowKeys.add(key)
            }
            dueSoon && (forceNotify || key !in knownDeadlineKeys)
        }

        val dueSoonInternships = allInternships.filter { internship ->
            val key = keyFor(internship.source.ifBlank { "UNKNOWN" }, "INTERNSHIP", internship.id)
            val dueSoon = hasLessThanTenDaysLeft(internship)
            if (dueSoon) {
                currentWindowKeys.add(key)
            }
            dueSoon && (forceNotify || key !in knownDeadlineKeys)
        }

        val totalDueSoon = dueSoonJobs.size + dueSoonInternships.size

        if (totalDueSoon > 0) {
            OpportunityNotificationHelper.notifyDeadlineApproaching(
                context = applicationContext,
                totalDueSoon = totalDueSoon,
                dueSoonJobs = dueSoonJobs.size,
                dueSoonInternships = dueSoonInternships.size
            )
        }

        knownDeadlineKeys.addAll(currentWindowKeys)
        writeDeadlineAlertedKeys(knownDeadlineKeys)
    }

    private suspend fun fetchActiveFirebaseJobs(): List<Job> {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("jobs")
            .whereEqualTo("status", "Active")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            runCatching { doc.toObject(Job::class.java) }.getOrNull()
        }
    }

    private suspend fun fetchActiveFirebaseHackathons(): List<Hackathon> {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("hackathons")
            .whereEqualTo("status", "Active")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            runCatching { doc.toObject(Hackathon::class.java) }.getOrNull()
        }
    }

    private fun isInternship(job: Job): Boolean {
        if (job.opportunityType.equals("INTERNSHIP", ignoreCase = true)) {
            return true
        }

        val text = "${job.title} ${job.description}".lowercase()
        return text.contains("intern") || text.contains("internship")
    }

    private fun hasLessThanTenDaysLeft(job: Job): Boolean {
        val deadline = job.deadline ?: return false
        val now = System.currentTimeMillis()
        val millisLeft = deadline.time - now
        if (millisLeft < 0L) {
            return false
        }

        val daysLeft = TimeUnit.MILLISECONDS.toDays(millisLeft)
        return daysLeft in 0..9
    }

    private fun readKnownIndex(): KnownIndex {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = prefs.getString(KEY_KNOWN_IDS, "").orEmpty()
        val entries = csv.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (entries.isEmpty()) {
            return KnownIndex(emptySet(), emptySet(), isLegacyFormat = false)
        }

        val isLegacy = entries.any { KEY_SEPARATOR !in it }
        val rawIds = entries.map { entry ->
            if (KEY_SEPARATOR in entry) entry.substringAfterLast(KEY_SEPARATOR) else entry
        }.toSet()

        return KnownIndex(entries.toSet(), rawIds, isLegacy)
    }

    private fun writeKnownKeys(keys: Set<String>) {
        val csv = keys.joinToString(",")
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_KNOWN_IDS, csv).apply()
    }

    private fun readDeadlineAlertedKeys(): Set<String> {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = prefs.getString(KEY_DEADLINE_ALERTED_IDS, "").orEmpty()
        return csv.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun writeDeadlineAlertedKeys(keys: Set<String>) {
        val csv = keys.joinToString(",")
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEADLINE_ALERTED_IDS, csv).apply()
    }

    private fun keyFor(source: String, type: String, id: String): String {
        return "$source$KEY_SEPARATOR$type$KEY_SEPARATOR$id"
    }

    private fun isKnown(knownIndex: KnownIndex, key: String, rawId: String): Boolean {
        return key in knownIndex.exactKeys || rawId in knownIndex.rawIds
    }

    private data class KnownIndex(
        val exactKeys: Set<String>,
        val rawIds: Set<String>,
        val isLegacyFormat: Boolean
    )

    companion object {
        private const val PREFS_NAME = "opportunity_sync"
        private const val KEY_KNOWN_IDS = "known_external_ids"
        private const val KEY_DEADLINE_ALERTED_IDS = "deadline_alerted_ids"
        private const val KEY_SEPARATOR = "|"
        const val KEY_FORCE_DEADLINE_NOTIFICATION = "force_deadline_notification"
    }
}
