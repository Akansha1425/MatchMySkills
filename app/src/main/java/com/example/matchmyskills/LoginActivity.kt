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
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: AuthRepository

    private lateinit var googleSignInClient: GoogleSignInClient
    private var selectedRole: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroupUserType)
        val socialLayout = findViewById<LinearLayout>(R.id.layout_social_signup)
        val tvRegister = findViewById<TextView>(R.id.tv_sign_up)
        val btn = findViewById<Button>(R.id.btn_login)
        val etemail = findViewById<TextInputEditText>(R.id.et_email)
        val etpassword = findViewById<TextInputEditText>(R.id.et_password)
        val tvTitle = findViewById<TextView>(R.id.tv_title)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnStudent -> {
                        selectedRole = "student"
                        socialLayout.visibility = View.VISIBLE
                        tvTitle.text = "Student Login"
                    }
                    R.id.btnRecruiter -> {
                        tvTitle.text = "Recruiter Login"
                        selectedRole = "recruiter"
                        socialLayout.visibility = View.VISIBLE
                    }
                }
            }
        }

        btn.setOnClickListener {
            val email = etemail.text.toString().trim()
            val password = etpassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading state
            progressBar.visibility = View.VISIBLE
            btn.isEnabled = false

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener
                    // Use unified navigation
                    FirebaseFirestore.getInstance().collection("users").document(userId).get()
                        .addOnSuccessListener { doc ->
                            val role = doc.getString("role")
                            navigateByRole(userId, role)
                        }
                        .addOnFailureListener {
                            progressBar.visibility = View.GONE
                            btn.isEnabled = true
                            Toast.makeText(this, "Fetch role failed", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    btn.isEnabled = true
                    Toast.makeText(this, "Login Failed: ${it.message}", Toast.LENGTH_LONG).show()
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

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun navigateByRole(uid: String, selectedRole: String?) {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // New user or missing profile, go to onboarding
                    navigateToOnboarding()
                    return@addOnSuccessListener
                }

                val role = doc.getString("role") ?: selectedRole ?: "student"
                if (role.equals("recruiter", ignoreCase = true)) {
                    val companyName = doc.getString("companyName")
                    if (companyName.isNullOrEmpty()) {
                        navigateToOnboarding()
                    } else {
                        navigateToMain()
                    }
                } else if (role.equals("admin", ignoreCase = true)) {
                    val intent = Intent(this, AdminDashboard::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Student
                    val intent = Intent(this, StudentDashboard::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
                Toast.makeText(this, "Error fetching user data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
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
}