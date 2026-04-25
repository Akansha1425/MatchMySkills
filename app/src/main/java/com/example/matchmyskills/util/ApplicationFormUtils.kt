package com.example.matchmyskills.util

import android.view.View
import com.example.matchmyskills.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

enum class ApplicationType(val value: String, val opportunityType: String) {
    JOB("job", "JOB"),
    INTERNSHIP("internship", "INTERNSHIP"),
    HACKATHON("hackathon", "HACKATHON");

    companion object {
        fun fromOpportunityType(raw: String?): ApplicationType {
            return when (raw?.trim()?.uppercase()) {
                "INTERNSHIP" -> INTERNSHIP
                "HACKATHON" -> HACKATHON
                else -> JOB
            }
        }
    }
}

object ApplicationFormUtils {

    fun configureResumeSection(
        rootView: View,
        applicationType: ApplicationType,
        resumeInput: TextInputEditText
    ) {
        val resumeSection = rootView.findViewById<View>(R.id.layoutResumeSection)
        val resumeInputLayout = rootView.findViewById<TextInputLayout>(R.id.tilResumeUrl)

        when (applicationType) {
            ApplicationType.JOB -> {
                resumeSection.visibility = View.VISIBLE
                resumeInputLayout.visibility = View.VISIBLE
                resumeInputLayout.hint = "Resume Link (Required for Job)"
            }
            ApplicationType.INTERNSHIP -> {
                resumeSection.visibility = View.VISIBLE
                resumeInputLayout.visibility = View.VISIBLE
                resumeInputLayout.hint = "Resume Link (Optional)"
            }
            ApplicationType.HACKATHON -> {
                resumeSection.visibility = View.GONE
                resumeInputLayout.visibility = View.GONE
                resumeInput.setText("")
            }
        }
    }

    fun validateResume(
        applicationType: ApplicationType,
        resumeUrl: String
    ): String? {
        if (applicationType == ApplicationType.JOB && resumeUrl.isBlank()) {
            return "Resume is required for job applications"
        }
        return null
    }
}
