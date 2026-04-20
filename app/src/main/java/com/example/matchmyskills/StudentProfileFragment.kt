package com.example.matchmyskills

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.FrameLayout
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class StudentProfileFragment : Fragment(R.layout.fragment_student_profile) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var isEditMode = false
    private var currentUser: Map<String, Any>? = null

    private lateinit var tvStudentName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvCgpa: TextView
    private lateinit var cgStudentSkills: ChipGroup
    private lateinit var ivProfile: ShapeableImageView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnChangePhoto: FloatingActionButton
    private lateinit var cardProfileInfo: CardView
    private lateinit var cardEditMode: CardView
    private lateinit var etName: TextInputEditText
    private lateinit var etCgpa: TextInputEditText
    private lateinit var etSkills: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var llEditControls: LinearLayout

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadProfileImage(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        loadUserData()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        tvStudentName = view.findViewById(R.id.tv_student_name)
        tvEmail = view.findViewById(R.id.tv_email)
        tvCgpa = view.findViewById(R.id.tv_cgpa)
        cgStudentSkills = view.findViewById(R.id.cg_student_skills)
        ivProfile = view.findViewById(R.id.iv_profile)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnChangePhoto = view.findViewById(R.id.btn_change_photo)
        cardProfileInfo = view.findViewById(R.id.card_profile_info)
        cardEditMode = view.findViewById(R.id.card_edit_mode)
        etName = view.findViewById(R.id.et_name)
        etCgpa = view.findViewById(R.id.et_cgpa)
        etSkills = view.findViewById(R.id.et_skills)
        btnSave = view.findViewById(R.id.btn_save)
        btnCancel = view.findViewById(R.id.btn_cancel)
        llEditControls = view.findViewById(R.id.ll_edit_controls)
    }

    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener { toggleEditMode() }
        btnChangePhoto.setOnClickListener { imagePicker.launch("image/*") }
        btnLogout.setOnClickListener { showLogoutConfirmation() }
        btnSave.setOnClickListener { saveProfileChanges() }
        btnCancel.setOnClickListener { toggleEditMode() }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    currentUser = doc.data
                    val name = doc.getString("name") ?: "Student"
                    val email = doc.getString("email") ?: ""
                    val cgpa = doc.getString("marks") ?: "Not specified"
                    val skills = doc.get("skills") as? List<String> ?: emptyList()

                    tvStudentName.text = name
                    tvEmail.text = email
                    tvCgpa.text = cgpa

                    loadProfileImageFromLocal()

                    // Populate edit fields
                    etName.setText(name)
                    etCgpa.setText(cgpa)
                    etSkills.setText(skills.joinToString(", "))

                    displaySkills(skills)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun displaySkills(skills: List<String>) {
        cgStudentSkills.removeAllViews()
        skills.forEach { skill ->
            val chip = Chip(requireContext()).apply {
                text = skill
                isCheckable = false
                isClickable = false
                setChipBackgroundColorResource(R.color.matchmyskills_primary)
                setTextColor(android.graphics.Color.WHITE)
            }
            cgStudentSkills.addView(chip)
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        if (isEditMode) {
            cardProfileInfo.visibility = View.GONE
            cardEditMode.visibility = View.VISIBLE
            btnEditProfile.visibility = View.GONE
            llEditControls.visibility = View.VISIBLE
            btnChangePhoto.visibility = View.VISIBLE
        } else {
            cardProfileInfo.visibility = View.VISIBLE
            cardEditMode.visibility = View.GONE
            btnEditProfile.visibility = View.VISIBLE
            llEditControls.visibility = View.GONE
            btnChangePhoto.visibility = View.GONE
            loadUserData()
        }
    }

    private fun saveProfileChanges() {
        val userId = auth.currentUser?.uid ?: return

        val name = etName.text.toString().trim()
        val cgpa = etCgpa.text.toString().trim()
        val skillsText = etSkills.text.toString().trim()
        val skills = skillsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (name.isEmpty()) {
            Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf(
            "name" to name,
            "marks" to cgpa,
            "skills" to skills
        )

        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                toggleEditMode()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun uploadProfileImage(uri: Uri) {
        btnChangePhoto.isEnabled = false
        Toast.makeText(context, "Saving profile photo...", Toast.LENGTH_SHORT).show()

        try {
            val bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                requireContext().contentResolver,
                uri
            )
            saveImageLocally(bitmap)
            Glide.with(this)
                .load(bitmap)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(ivProfile)
            Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("StudentProfileFragment", "Profile image save failed", e)
            Toast.makeText(context, "Failed to save image. Please try again.", Toast.LENGTH_LONG).show()
        } finally {
            btnChangePhoto.isEnabled = true
        }
    }

    private fun saveImageLocally(bitmap: Bitmap) {
        try {
            val imagesDir = File(requireContext().filesDir, "profile_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val imageFile = File(imagesDir, "profile_pic.jpg")
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
        } catch (e: Exception) {
            Log.e("StudentProfileFragment", "Failed to save image locally", e)
            throw e
        }
    }

    private fun loadProfileImageFromLocal() {
        try {
            val imageFile = File(requireContext().filesDir, "profile_images/profile_pic.jpg")
            if (imageFile.exists()) {
                Glide.with(this)
                    .load(imageFile)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(ivProfile)
            } else {
                ivProfile.setImageResource(R.drawable.ic_profile)
            }
        } catch (e: Exception) {
            Log.e("StudentProfileFragment", "Failed to load local image", e)
            ivProfile.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
