package com.example.matchmyskills

import android.app.Application
import com.example.matchmyskills.background.OpportunitySyncScheduler
import com.example.matchmyskills.notifications.OpportunityNotificationHelper
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MatchMySkillsApp : Application() {

	override fun onCreate() {
		super.onCreate()
		OpportunityNotificationHelper.createChannel(this)
		OpportunitySyncScheduler.schedulePeriodicSync(this)

		if (BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank()) {
			val config = mapOf(
				"cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
				"secure" to true
			)
			MediaManager.init(this, config)
		}
	}
}
