package com.example.matchmyskills

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.matchmyskills.repository.AuthRepository
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: AuthRepository

    private lateinit var googleSignInClient: GoogleSignInClient
    private var selectedRole: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroupRegisterType)
        val socialLayout = findViewById<LinearLayout>(R.id.layout_social_signup)
        val btnAction = findViewById<Button>(R.id.btn_create)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)
        val tvTitle = findViewById<TextView>(R.id.tv_title)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val tvSignIn = findViewById<TextView>(R.id.tv_sign_in)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnRegStudent -> {
                        tvTitle.text = "Student Login"
                        selectedRole = "student"
                        socialLayout.visibility = View.VISIBLE
                    }
                    R.id.btnRegRecruiter -> {
                        tvTitle.text = "Recruiter Login"
                        selectedRole = "recruiter"
                        socialLayout.visibility = View.VISIBLE
                    }
                }
            }
        }

        tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnAction.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            // Show loading state
            progressBar.visibility = View.VISIBLE
            btnAction.isEnabled = false

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    val userMap = hashMapOf(
                        "id" to userId,
                        "email" to email,
                        "role" to selectedRole,
                        "createdAt" to System.currentTimeMillis()
                    )

                    FirebaseFirestore.getInstance().collection("users")
                        .document(userId!!)
                        .set(userMap)
                        .addOnSuccessListener {
                            // Use unified navigation
                            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                                .addOnSuccessListener { doc ->
                                    val role = doc.getString("role")
                                    navigateByRole(userId, role)
                                }
                                .addOnFailureListener {
                                    progressBar.visibility = View.GONE
                                    btnAction.isEnabled = true
                                    Toast.makeText(this, "Fetch role failed", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            progressBar.visibility = View.GONE
                            btnAction.isEnabled = true
                            Toast.makeText(this, "Firestore Error: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    btnAction.isEnabled = true
                    Toast.makeText(this, "Registration Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Google Sign-In Configuration
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<View>(R.id.btn_google).setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, 1001)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE
        
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
                            .addOnSuccessListener { doc ->
                                if (!doc.exists()) {
                                    // NEW USER: SAVE SELECTED ROLE
                                    val userMap = hashMapOf(
                                        "id" to user.uid,
                                        "email" to user.email,
                                        "role" to selectedRole,
                                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(user.uid)
                                        .set(userMap, SetOptions.merge())
                                        .addOnSuccessListener { navigateByRole(user.uid, selectedRole) }
                                } else {
                                    // EXISTING USER: DO NOT OVERWRITE ROLE
                                    val existingRole = doc.getString("role") ?: selectedRole
                                    navigateByRole(user.uid, existingRole)
                                }
                            }
                    }
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateByRole(uid: String, role: String?) {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.GONE

        if (role.isNullOrEmpty()) {
            Toast.makeText(this, "No role assigned", Toast.LENGTH_SHORT).show()
            return
        }

        repository.saveUserRole(role)
        repository.setLoggedIn(true)

        when (role) {
            "recruiter" -> {
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val companyName = doc.getString("companyName")
                        if (companyName.isNullOrEmpty()) {
                            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        } else {
                            startActivity(Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        }
                        finish()
                    }
            }
            "student" -> {
                val intent = Intent(this, StudentDashboard::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}