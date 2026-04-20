package com.example.matchmyskills

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.example.matchmyskills.model.Hackathon
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class OpportunityDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentHackathon: Hackathon? = null
    private lateinit var btnApply: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opportunity_detail)

        currentHackathon = intent.getParcelableExtra("EXTRA_HACKATHON")

        if (currentHackathon == null) {
            Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        populateUI(currentHackathon!!)

        btnApply = findViewById(R.id.btnApplyNow)
        btnApply.setOnClickListener {
            showApplicationDialog()
        }

        checkIfAlreadyApplied()
    }

    private fun populateUI(hackathon: Hackathon) {
        findViewById<TextView>(R.id.tvDetailTitle).text = cleanDisplayText(hackathon.title)
        findViewById<TextView>(R.id.tvDetailOrganizer).text = "By ${cleanDisplayText(hackathon.organizer)}"
        findViewById<TextView>(R.id.tvDetailDescription).text = cleanDisplayText(hackathon.description)
        findViewById<TextView>(R.id.tvDetailEligibility).text = if(hackathon.eligibility.isEmpty()) "Open to all" else cleanDisplayText(hackathon.eligibility)
        findViewById<TextView>(R.id.tvDetailTeamSize).text = cleanDisplayText(hackathon.teamSize)
        findViewById<TextView>(R.id.tvDetailMode).text = cleanDisplayText(hackathon.mode)
        findViewById<TextView>(R.id.tvDetailPrizePool).text = cleanDisplayText(hackathon.prizePool)
    }

    private fun cleanDisplayText(value: String): String {
        if (value.isBlank()) return ""
        return HtmlCompat.fromHtml(value, HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace("\\u00A0", " ")
            .trim()
    }

    private fun showApplicationDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_apply_now, null)
        dialog.setContentView(view)

        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etMarks = view.findViewById<TextInputEditText>(R.id.etMarks)
        val etResumeUrl = view.findViewById<TextInputEditText>(R.id.etResumeUrl)
        val etReason = view.findViewById<TextInputEditText>(R.id.etReason)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitApplication)

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val marks = etMarks.text.toString().trim()
            val resumeUrl = etResumeUrl.text.toString().trim()
            val reason = etReason.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || resumeUrl.isEmpty() || reason.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            btnSubmit.text = "Submitting..."

            submitApplication(name, email, marks, resumeUrl, reason, dialog)
        }

        dialog.show()
    }

    private fun submitApplication(
        name: String,
        email: String,
        marks: String,
        resumeUrl: String,
        reason: String,
        dialog: BottomSheetDialog
    ) {
        val hackathon = currentHackathon ?: return
        val applicantId = auth.currentUser?.uid ?: "unknown_user"
        val applicationId = UUID.randomUUID().toString()

        val applicationData = hashMapOf(
            "id" to applicationId,
            "applicationId" to applicationId,
            "opportunityId" to hackathon.id,
            "externalOpportunityId" to hackathon.id,
            "jobId" to "",
            "opportunityType" to "HACKATHON",
            "source" to hackathon.source,
            "opportunityTitle" to hackathon.title,
            "companyName" to hackathon.organizer,
            "recruiterId" to hackathon.recruiterId,
            "candidateId" to applicantId,
            "userId" to applicantId,
            "candidateName" to name,
            "applicantName" to name,
            "candidateEmail" to email,
            "applicantEmail" to email,
            "candidateMarks" to marks,
            "resumeUrl" to resumeUrl,
            "candidateReason" to reason,
            "status" to "Pending",
            "appliedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("applications")
            .document(applicationId)
            .set(applicationData)
            .addOnSuccessListener {
                Toast.makeText(this, "Application Submitted Successfully!", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                setAppliedState()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Application Failed: ${e.message}", Toast.LENGTH_LONG).show()
                dialog.findViewById<Button>(R.id.btnSubmitApplication)?.let {
                    it.isEnabled = true
                    it.text = "Submit Application"
                }
            }
    }

    private fun checkIfAlreadyApplied() {
        val hackathon = currentHackathon ?: return
        val userId = auth.currentUser?.uid

        if (userId.isNullOrBlank()) {
            btnApply.isEnabled = false
            btnApply.text = "Login to Apply"
            return
        }

        btnApply.isEnabled = false
        btnApply.text = "Checking..."

        db.collection("applications")
            .whereEqualTo("candidateId", userId)
            .whereEqualTo("opportunityId", hackathon.id)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    setAppliedState()
                } else {
                    setReadyToApplyState()
                }
            }
            .addOnFailureListener {
                setReadyToApplyState()
            }
    }

    private fun setAppliedState() {
        btnApply.isEnabled = false
        btnApply.text = "Already Applied"
    }

    private fun setReadyToApplyState() {
        btnApply.isEnabled = true
        btnApply.text = "Apply Now"
    }
}
