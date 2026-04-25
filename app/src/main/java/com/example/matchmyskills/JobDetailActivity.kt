package com.example.matchmyskills

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.text.HtmlCompat
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.util.CloudinaryResumeUploader
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class JobDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentJob: Job? = null
    private lateinit var btnApply: Button
    private var activeResumeInput: TextInputEditText? = null
    private var activeResumeStatus: TextView? = null
    private var activeSubmitButton: Button? = null
    private var uploadedResumeUrl: String? = null
    private var uploadedResumeType: String? = null

    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult

        CloudinaryResumeUploader.uploadPdf(
            context = this,
            pdfUri = uri,
            onStart = {
                activeSubmitButton?.isEnabled = false
                activeResumeStatus?.text = "Uploading PDF..."
            },
            onSuccess = { secureUrl ->
                uploadedResumeUrl = secureUrl
                uploadedResumeType = "pdf"
                activeResumeInput?.setText(secureUrl)
                activeResumeStatus?.text = "PDF uploaded successfully"
                activeSubmitButton?.isEnabled = true
            },
            onError = { message ->
                activeResumeStatus?.text = message
                activeSubmitButton?.isEnabled = true
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_detail)

        currentJob = intent.getParcelableExtra("EXTRA_JOB")

        if (currentJob == null) {
            Toast.makeText(this, "Failed to load job details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        populateUI(currentJob!!)

        btnApply = findViewById(R.id.btnApplyNowJob)
        btnApply.setOnClickListener {
            showApplicationDialog()
        }

        checkIfAlreadyApplied()
    }

    private fun populateUI(job: Job) {
        findViewById<TextView>(R.id.tvDetailJobTitle).text = cleanDisplayText(job.title)
        findViewById<TextView>(R.id.tvDetailJobCompany).text = "At ${cleanDisplayText(job.companyName)}"
        findViewById<TextView>(R.id.tvDetailJobDescription).text = cleanDisplayText(job.description)
        
        val skills = job.coreSkills.map { cleanDisplayText(it) }.joinToString(", ")
        findViewById<TextView>(R.id.tvDetailJobSkills).text = if (skills.isEmpty()) "Not specified" else skills

        findViewById<TextView>(R.id.tvDetailJobLocation).text = cleanDisplayText(job.location)
        findViewById<TextView>(R.id.tvDetailJobCity).text = cleanDisplayText(job.city ?: "Any")
        findViewById<TextView>(R.id.tvDetailJobStipend).text = cleanDisplayText(job.stipend)
        findViewById<TextView>(R.id.tvDetailJobDuration).text = cleanDisplayText(job.duration)
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
        val etSkills = view.findViewById<TextInputEditText>(R.id.etSkills)
        val etResumeUrl = view.findViewById<TextInputEditText>(R.id.etResumeUrl)
        val tvResumeUploadStatus = view.findViewById<TextView>(R.id.tvResumeUploadStatus)
        val btnUploadResumePdf = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUploadResumePdf)
        val etReason = view.findViewById<TextInputEditText>(R.id.etReason)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitApplication)

        uploadedResumeUrl = null
        uploadedResumeType = null
        activeResumeInput = etResumeUrl
        activeResumeStatus = tvResumeUploadStatus
        activeSubmitButton = btnSubmit

        // Autofill from profile
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etName.setText(doc.getString("name"))
                    etEmail.setText(doc.getString("email"))
                    etResumeUrl.setText(doc.getString("resumeUrl") ?: "")
                    val skillsList = doc.get("skills") as? List<String>
                    etSkills.setText(skillsList?.joinToString(", "))
                }
            }
        }

        btnUploadResumePdf.setOnClickListener {
            pdfPickerLauncher.launch("application/pdf")
        }

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val marks = etMarks.text.toString().trim()
            val skills = etSkills.text.toString().trim()
            val pastedResumeUrl = etResumeUrl.text.toString().trim()
            val reason = etReason.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || skills.isEmpty()) {
                Toast.makeText(this, "Name, email and skills are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalResumeUrl = uploadedResumeUrl?.takeIf { it.isNotBlank() } ?: pastedResumeUrl
            val finalResumeType = when {
                uploadedResumeType == "pdf" && finalResumeUrl.isNotBlank() -> "pdf"
                finalResumeUrl.isNotBlank() -> "link"
                else -> ""
            }

            btnSubmit.isEnabled = false
            btnSubmit.text = "Submitting..."

            // Convert skills string to list
            val skillsList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            submitApplication(name, email, marks, skillsList, finalResumeUrl, finalResumeType, reason, dialog)
        }

        dialog.show()
    }

    private fun submitApplication(
        name: String,
        email: String,
        marks: String,
        candidateSkills: List<String>,
        resumeUrl: String,
        resumeType: String,
        reason: String,
        dialog: BottomSheetDialog
    ) {
        val job = currentJob ?: return
        val applicantId = auth.currentUser?.uid ?: "unknown_user"

        // Calculate dynamic match score based on the skills provided in the dialog
        val scoreResult = com.example.matchmyskills.util.MatchingEngine.calculateMatchScore(candidateSkills, job)

        val applicationId = UUID.randomUUID().toString()
        val normalizedType = if (job.opportunityType.equals("INTERNSHIP", ignoreCase = true)) {
            "INTERNSHIP"
        } else {
            "JOB"
        }

        val applicationData = hashMapOf(
            "id" to applicationId,
            "applicationId" to applicationId,
            "opportunityId" to job.id,
            "externalOpportunityId" to job.id,
            "jobId" to job.id,
            "opportunityType" to normalizedType,
            "source" to job.source,
            "opportunityTitle" to job.title,
            "companyName" to job.companyName,
            "recruiterId" to job.recruiterId,
            "candidateId" to applicantId,
            "userId" to applicantId,
            "candidateName" to name,
            "name" to name,
            "applicantName" to name,
            "candidateEmail" to email,
            "email" to email,
            "applicantEmail" to email,
            "candidateMarks" to marks,
            "resumeUrl" to resumeUrl,
            "resumeType" to resumeType,
            "resumeText" to "",
            "candidateReason" to reason,
            "candidateSkills" to candidateSkills,
            "skills" to candidateSkills,
            "matchScore" to scoreResult.matchScore,
            "coreMatchCount" to scoreResult.coreMatchCount,
            "optionalMatchCount" to scoreResult.optionalMatchCount,
            "matchedSkills" to scoreResult.matchedSkills,
            "missingSkills" to scoreResult.missingSkills,
            "status" to "Pending",
            "timestamp" to FieldValue.serverTimestamp(),
            "appliedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("applications")
            .document(applicationId)
            .set(applicationData)
            .addOnSuccessListener {
                createRecruiterNotification(
                    recruiterId = job.recruiterId,
                    applicantName = name,
                    opportunityTitle = job.title,
                    jobId = job.id
                )
                Toast.makeText(this, "Application Submitted Successfully!", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                setAppliedState()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Application Failed: ${e.message}", Toast.LENGTH_LONG).show()
                dialog.findViewById<Button>(R.id.btnSubmitApplication)?.let {
                    it.isEnabled = true
                    it.text = "Submit Application"
                }
            }
    }

    private fun createRecruiterNotification(
        recruiterId: String,
        applicantName: String,
        opportunityTitle: String,
        jobId: String
    ) {
        if (recruiterId.isBlank()) return

        val notificationData = hashMapOf(
            "recruiterId" to recruiterId,
            "message" to "$applicantName applied to $opportunityTitle",
            "jobId" to jobId,
            "timestamp" to FieldValue.serverTimestamp(),
            "isRead" to false
        )

        db.collection("notifications")
            .add(notificationData)
            .addOnFailureListener { error ->
                android.util.Log.e(
                    "NotificationWrite",
                    "Failed to create recruiter notification for recruiterId=$recruiterId",
                    error
                )
                // Keep application success unaffected if notification write fails.
            }
    }

    private fun checkIfAlreadyApplied() {
        val job = currentJob ?: return
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
            .whereEqualTo("opportunityId", job.id)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    setAppliedState()
                } else {
                    checkLegacyJobApplication(userId, job.id)
                }
            }
            .addOnFailureListener {
                setReadyToApplyState()
            }
    }

    private fun checkLegacyJobApplication(userId: String, jobId: String) {
        db.collection("applications")
            .whereEqualTo("candidateId", userId)
            .whereEqualTo("jobId", jobId)
            .limit(1)
            .get()
            .addOnSuccessListener { legacyResult ->
                if (!legacyResult.isEmpty) {
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
