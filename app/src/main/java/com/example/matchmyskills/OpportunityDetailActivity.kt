package com.example.matchmyskills

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        findViewById<Button>(R.id.btnApplyNow).setOnClickListener {
            showApplicationDialog()
        }
    }

    private fun populateUI(hackathon: Hackathon) {
        findViewById<TextView>(R.id.tvDetailTitle).text = hackathon.title
        findViewById<TextView>(R.id.tvDetailOrganizer).text = "By ${hackathon.organizer}"
        findViewById<TextView>(R.id.tvDetailDescription).text = hackathon.description
        findViewById<TextView>(R.id.tvDetailEligibility).text = if(hackathon.eligibility.isEmpty()) "Open to all" else hackathon.eligibility
        findViewById<TextView>(R.id.tvDetailTeamSize).text = hackathon.teamSize
        findViewById<TextView>(R.id.tvDetailMode).text = hackathon.mode
        findViewById<TextView>(R.id.tvDetailPrizePool).text = hackathon.prizePool
    }

    private fun showApplicationDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_apply_now, null)
        dialog.setContentView(view)

        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etMarks = view.findViewById<TextInputEditText>(R.id.etMarks)
        val etReason = view.findViewById<TextInputEditText>(R.id.etReason)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitApplication)

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val marks = etMarks.text.toString().trim()
            val reason = etReason.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || marks.isEmpty() || reason.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            btnSubmit.text = "Submitting..."

            submitApplication(name, email, marks, reason, dialog)
        }

        dialog.show()
    }

    private fun submitApplication(name: String, email: String, marks: String, reason: String, dialog: BottomSheetDialog) {
        val hackathon = currentHackathon ?: return
        val applicantId = auth.currentUser?.uid ?: "unknown_user"

        val applicationData = hashMapOf(
            "id" to UUID.randomUUID().toString(),
            "opportunityId" to hackathon.id,
            "opportunityTitle" to hackathon.title,
            "recruiterId" to hackathon.recruiterId,
            "candidateId" to applicantId,
            "candidateName" to name,
            "candidateEmail" to email,
            "candidateMarks" to marks,
            "candidateReason" to reason,
            "status" to "Pending",
            "appliedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("applications")
            .document(applicationData["id"] as String)
            .set(applicationData)
            .addOnSuccessListener {
                Toast.makeText(this, "Application Submitted Successfully!", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                finish() // optionally closing detail view after applying
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Application Failed: ${e.message}", Toast.LENGTH_LONG).show()
                dialog.findViewById<Button>(R.id.btnSubmitApplication)?.let {
                    it.isEnabled = true
                    it.text = "Submit Application"
                }
            }
    }
}
