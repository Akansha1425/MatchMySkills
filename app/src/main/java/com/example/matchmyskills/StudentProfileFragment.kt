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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

class StudentProfileFragment : Fragment(R.layout.fragment_student_profile) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var isEditMode = false
    private var currentUser: Map<String, Any>? = null

    private lateinit var tvStudentName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvEmail: TextView
    private lateinit var cgStudentSkills: ChipGroup
    private lateinit var ivProfile: ShapeableImageView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnChangePhoto: FloatingActionButton
    private lateinit var tvProjects: TextView
    private lateinit var tvLinkedin: TextView
    private lateinit var tvGithub: TextView
    private lateinit var tvCompletionPercent: TextView
    private lateinit var pbCompletion: com.google.android.material.progressindicator.LinearProgressIndicator
    private lateinit var tvPendingCount: TextView
    private lateinit var tvShortlistedCount: TextView
    private lateinit var tvTotalApplied: TextView
    
    private lateinit var cardProfileInfo: CardView
    private lateinit var cardEditMode: CardView
    private lateinit var etName: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etSkills: TextInputEditText
    private lateinit var etLinkedin: TextInputEditText
    private lateinit var etProjects: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var etGithub: android.widget.EditText
    private lateinit var etResume: android.widget.EditText
    private lateinit var btnViewResume: MaterialButton

    private var currentProfileImageUrl: String? = null
    
    private var resumeUrl: String? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadProfileImage(it)
        }
    }

    private var tempImageUri: Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { uploadProfileImage(it) }
        }
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(context, "Camera permission required for taking photos", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchLocation()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        loadUserData()
        loadApplicationStatus()
        setupClickListeners()
        
        // Fetch location like dashboard
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (com.example.matchmyskills.util.LocationHelper.isLocationPermissionGranted(requireContext())) {
            fetchLocation()
        } else {
            requestLocationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchLocation() {
        com.example.matchmyskills.util.LocationHelper.fetchLocation(requireContext(), object : com.example.matchmyskills.util.LocationHelper.LocationCallback {
            override fun onLocationFetched(city: String, state: String) {
                if (!isAdded) return
                val location = "$city, $state"
                tvLocation.text = location
                
                // Also update Firestore if the bio/location was not set
                updateLocationInFirestore(location)
            }

            override fun onLocationError(message: String) {
                Log.e("StudentProfile", "Location error: $message")
            }
        })
    }

    private fun updateLocationInFirestore(location: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (doc.exists() && doc.getString("location").isNullOrBlank()) {
                db.collection("users").document(userId).update("location", location)
            }
        }
    }

    private fun initializeViews(view: View) {
        tvStudentName = view.findViewById(R.id.tv_student_name)
        tvBio = view.findViewById(R.id.tv_bio)
        tvLocation = view.findViewById(R.id.tv_location)
        tvEmail = view.findViewById(R.id.tv_email)
        cgStudentSkills = view.findViewById(R.id.cg_student_skills)
        ivProfile = view.findViewById(R.id.iv_profile)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnChangePhoto = view.findViewById(R.id.btn_change_photo)
        tvProjects = view.findViewById(R.id.tv_projects)
        tvLinkedin = view.findViewById(R.id.tv_linkedin)
        tvGithub = view.findViewById(R.id.tv_github)
        tvCompletionPercent = view.findViewById(R.id.tv_completion_percent)
        pbCompletion = view.findViewById(R.id.pb_completion)
        tvPendingCount = view.findViewById(R.id.tv_pending_count)
        tvShortlistedCount = view.findViewById(R.id.tv_shortlisted_count)
        tvTotalApplied = view.findViewById(R.id.tv_total_applied)

        cardProfileInfo = view.findViewById(R.id.card_profile_info)
        cardEditMode = view.findViewById(R.id.card_edit_mode)
        
        etName = view.findViewById(R.id.et_name)
        etBio = view.findViewById(R.id.et_bio)
        etLocation = view.findViewById(R.id.et_location)
        etSkills = view.findViewById(R.id.et_skills)
        etLinkedin = view.findViewById(R.id.et_linkedin)
        etGithub = view.findViewById(R.id.et_github)
        etResume = view.findViewById(R.id.et_resume)
        etProjects = view.findViewById(R.id.et_projects)
        
        btnSave = view.findViewById(R.id.btn_save)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnViewResume = view.findViewById(R.id.btn_view_resume)
    }

    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener { toggleEditMode() }
        btnChangePhoto.setOnClickListener { showImageSourceDialog() }
        btnLogout.setOnClickListener { showLogoutConfirmation() }
        btnSave.setOnClickListener { saveProfileChanges() }
        btnCancel.setOnClickListener { toggleEditMode() }
        btnViewResume.setOnClickListener { openResume() }
        
        tvLinkedin.setOnClickListener { openUrl(tvLinkedin.text.toString()) }
        tvGithub.setOnClickListener { openUrl(tvGithub.text.toString()) }

        ivProfile.setOnClickListener {
            currentProfileImageUrl?.let { url ->
                val intent = android.content.Intent(requireContext(), ImagePreviewActivity::class.java)
                intent.putExtra("image_url", url)
                startActivity(intent)
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Gallery", "Camera")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Profile Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> imagePicker.launch("image/*")
                    1 -> checkCameraPermission()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) 
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        try {
            val photoFile = java.io.File(requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "profile_temp_${System.currentTimeMillis()}.jpg")
            if (!photoFile.parentFile.exists()) {
                photoFile.parentFile.mkdirs()
            }
            tempImageUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "com.example.matchmyskills.fileprovider",
                photoFile
            )
            cameraLauncher.launch(tempImageUri!!)
        } catch (e: Exception) {
            Log.e("StudentProfile", "Failed to create photo file", e)
            Toast.makeText(context, "Failed to open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrl(url: String) {
        if (url.isNullOrBlank() || url == "Not linked") return
        try {
            var finalUrl = url.trim()
            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                finalUrl = "https://$finalUrl"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openResume() {
        if (resumeUrl.isNullOrBlank()) {
            Toast.makeText(context, "No resume link provided", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resumeUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val profileImageUrl = doc.getString("profileImage") ?: doc.getString("profileImageUrl")
                    currentProfileImageUrl = profileImageUrl
                    
                    if (isAdded) {
                        displayProfileData(doc)
                    }
                }
            }
    }

    private fun displayProfileData(doc: com.google.firebase.firestore.DocumentSnapshot) {
        try {
            currentUser = doc.data
            val name = doc.getString("name") ?: "Student"
            val email = doc.getString("email") ?: ""
            val bio = doc.getString("bio") ?: "No bio yet"
            val location = doc.getString("location") ?: "Not specified"
            val skills = doc.get("skills") as? List<String> ?: emptyList()
            val projects = doc.getString("projects") ?: "No projects listed"
            val linkedin = doc.getString("linkedin") ?: "Not linked"
            val github = doc.getString("github") ?: "Not linked"
            resumeUrl = doc.getString("resumeUrl")

            tvStudentName.text = name
            tvBio.text = bio
            tvLocation.text = location
            tvEmail.text = email
            tvProjects.text = projects
            tvLinkedin.text = linkedin
            tvGithub.text = github

            if (!currentProfileImageUrl.isNullOrBlank()) {
                Glide.with(this@StudentProfileFragment)
                    .load(currentProfileImageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivProfile)
            } else {
                loadProfileImageFromLocal()
            }

            // Populate edit fields
            etName.setText(name)
            etBio.setText(bio)
            etLocation.setText(location)
            etSkills.setText(skills.joinToString(", "))
            etLinkedin.setText(linkedin)
            etGithub.setText(github)
            etResume.setText(resumeUrl ?: "")
            etProjects.setText(projects)

            displaySkills(skills)
            calculateProfileCompletion(doc.data ?: emptyMap())
        } catch (e: Exception) {
            Log.e("StudentProfile", "Error in displayProfileData", e)
        }
    }

    private fun loadApplicationStatus() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("applications")
            .whereEqualTo("candidateId", userId)
            .get()
            .addOnSuccessListener { docs ->
                if (!isAdded) return@addOnSuccessListener
                
                val total = docs.size()
                val pending = docs.count { it.getString("status") == "Pending" }
                val shortlisted = docs.count { it.getString("status") == "Shortlisted" }
                
                tvTotalApplied.text = total.toString()
                tvPendingCount.text = pending.toString()
                tvShortlistedCount.text = shortlisted.toString()
            }
    }

    private fun calculateProfileCompletion(data: Map<String, Any>) {
        val coreFields = listOf("name", "bio", "location", "skills", "projects", "linkedin", "github", "resumeUrl")
        var completed = 0
        
        coreFields.forEach { field ->
            val value = data[field]
            if (value != null) {
                val strValue = value.toString().trim()
                if (strValue.isNotEmpty() && 
                    strValue != "Not specified" && 
                    strValue != "Not linked" && 
                    strValue != "No bio yet" && 
                    strValue != "No projects listed") {
                    
                    if (value is List<*>) {
                        if (value.isNotEmpty()) completed++
                    } else {
                        completed++
                    }
                }
            }
        }
        
        // Profile Image is a bonus but counts towards the final 100%
        val imgUrl = data["profileImage"] ?: data["profileImageUrl"]
        if (imgUrl != null) {
            val url = imgUrl.toString()
            if (url.isNotEmpty()) completed++
        }

        // We have 8 form fields + 1 photo = 9 possible points.
        // If the user has 8 or more, we'll call it 100% to avoid rounding issues if they've filled the form.
        val totalPossible = coreFields.size // 8
        val percent = ((completed.toFloat() / totalPossible.toFloat()) * 100).toInt().coerceAtMost(100)
        
        tvCompletionPercent.text = "$percent%"
        pbCompletion.progress = percent
    }

    private fun displaySkills(skills: List<String>) {
        cgStudentSkills.removeAllViews()
        skills.forEach { skill ->
            val chip = Chip(requireContext()).apply {
                text = skill
                isCheckable = false
                isClickable = false
                setChipBackgroundColorResource(R.color.matchmyskills_primary_container)
                setTextColor(resources.getColor(R.color.matchmyskills_primary, null))
            }
            cgStudentSkills.addView(chip)
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        if (isEditMode) {
            findViewById<View>(R.id.card_completion).visibility = View.GONE
            findViewById<View>(R.id.card_profile_info).visibility = View.GONE
            findViewById<View>(R.id.card_app_status).visibility = View.GONE
            findViewById<View>(R.id.card_links).visibility = View.GONE
            cardEditMode.visibility = View.VISIBLE
            btnChangePhoto.visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.card_completion).visibility = View.VISIBLE
            findViewById<View>(R.id.card_profile_info).visibility = View.VISIBLE
            findViewById<View>(R.id.card_app_status).visibility = View.VISIBLE
            findViewById<View>(R.id.card_links).visibility = View.VISIBLE
            cardEditMode.visibility = View.GONE
            btnChangePhoto.visibility = View.GONE
            loadUserData()
        }
    }

    private fun <T : View> findViewById(id: Int): T {
        return view?.findViewById(id) ?: throw IllegalStateException("View not found")
    }

    private fun saveProfileChanges() {
        val userId = auth.currentUser?.uid ?: return

        val name = etName.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val skillsText = etSkills.text.toString().trim()
        val skills = skillsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val linkedin = etLinkedin.text.toString().trim()
        val github = etGithub.text.toString().trim()
        val resumeLink = etResume.text.toString().trim()
        val projects = etProjects.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf(
            "name" to name,
            "bio" to bio,
            "location" to location,
            "skills" to skills,
            "linkedin" to linkedin,
            "github" to github,
            "resumeUrl" to resumeLink,
            "projects" to projects
        )

        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                toggleEditMode()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
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
        if (!isAdded) return

        btnChangePhoto.isEnabled = false
        Toast.makeText(context, "Uploading profile photo to Cloudinary...", Toast.LENGTH_SHORT).show()

        val userId = auth.currentUser?.uid ?: return

        // Preview immediately
        Glide.with(this@StudentProfileFragment)
            .load(uri)
            .circleCrop()
            .into(ivProfile)

        com.cloudinary.android.MediaManager.get().upload(uri)
            .unsigned("matchmyskills_upload")
            .option("public_id", "profile_$userId")
            .callback(object : com.cloudinary.android.callback.UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    val updateData = mapOf(
                        "profileImage" to imageUrl,
                        "profileImageUrl" to imageUrl
                    )
                    db.collection("users").document(userId)
                        .update(updateData)
                        .addOnSuccessListener {
                            if (isAdded) {
                                Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                                btnChangePhoto.isEnabled = true
                            }
                        }
                }

                override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                    if (isAdded) {
                        Toast.makeText(context, "Upload failed: ${error.description}", Toast.LENGTH_LONG).show()
                        btnChangePhoto.isEnabled = true
                    }
                }

                override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
            }).dispatch()
    }

    private fun loadProfileImageFromLocal() {
        // Fallback or placeholder handling
        ivProfile.setImageResource(R.drawable.ic_profile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
