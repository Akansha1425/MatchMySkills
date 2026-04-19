package com.example.matchmyskills

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentProfileFragment : Fragment(R.layout.fragment_student_profile) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etMarks: TextInputEditText
    private lateinit var etSkills: TextInputEditText
    private lateinit var cgSkills: ChipGroup
    private lateinit var btnSave: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        etName = view.findViewById<TextInputEditText>(R.id.etStudentName)
        etEmail = view.findViewById<TextInputEditText>(R.id.etStudentEmail)
        etMarks = view.findViewById<TextInputEditText>(R.id.etStudentMarks)
        etSkills = view.findViewById<TextInputEditText>(R.id.etStudentSkills)
        cgSkills = view.findViewById<ChipGroup>(R.id.cgStudentSkills)
        btnSave = view.findViewById<MaterialButton>(R.id.btnStudentSave)
        
        loadUserData()
        
        btnSave.setOnClickListener {
            saveUserData()
        }
        
        etSkills.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateSkillsPreview()
            }
        }
    }
    
    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etName.setText(doc.getString("name"))
                    etEmail.setText(doc.getString("email"))
                    etMarks.setText(doc.getString("marks"))
                    val skills = doc.get("skills") as? List<String> ?: emptyList()
                    etSkills.setText(skills.joinToString(", "))
                    updateSkillsPreview()
                }
            }
    }
    
    private fun updateSkillsPreview() {
        val skillsText = etSkills.text.toString()
        val skills = skillsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        cgSkills.removeAllViews()
        skills.forEach { skill ->
            val chip = Chip(requireContext()).apply {
                text = skill
                isCheckable = false
            }
            cgSkills.addView(chip)
        }
    }
    
    private fun saveUserData() {
        val userId = auth.currentUser?.uid ?: return
        val name = etName.text.toString().trim()
        val marks = etMarks.text.toString().trim()
        val skillsText = etSkills.text.toString()
        val skills = skillsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        if (name.isEmpty()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        btnSave.isEnabled = false
        btnSave.text = "Saving..."
        
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "marks" to marks,
            "skills" to skills
        )
        
        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "Save Profile"
                updateSkillsPreview()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "Save Profile"
            }
    }
}
