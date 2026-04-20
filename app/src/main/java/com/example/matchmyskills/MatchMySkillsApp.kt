package com.example.matchmyskills

import android.app.Application
import com.example.matchmyskills.background.OpportunitySyncScheduler
import com.example.matchmyskills.notifications.OpportunityNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MatchMySkillsApp : Application() {

	override fun onCreate() {
		super.onCreate()
		OpportunityNotificationHelper.createChannel(this)
		OpportunitySyncScheduler.schedulePeriodicSync(this)
	}
}
