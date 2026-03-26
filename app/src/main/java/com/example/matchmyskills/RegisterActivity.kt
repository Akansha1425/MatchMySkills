package com.example.matchmyskills


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class RegisterActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
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
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        // In your onCreate method
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroupRegisterType)
        val socialLayout = findViewById<LinearLayout>(R.id.layout_social_signup) // Add this ID to your XML wrapper
        val btnAction = findViewById<Button>(R.id.btn_create)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)
        val tvTitle = findViewById<TextView>(R.id.tv_title)
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
                    R.id.btnRegAdmin -> {
                        tvTitle.text = "Admin Panel"
                        selectedRole = "admin"
                        socialLayout.visibility = View.GONE
                    }
                }
            }
        }
        val tvSignIn = findViewById<TextView>(R.id.tv_sign_in)

        tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)

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
            etConfirmPassword.setOnFocusChangeListener { _, _ ->
                etConfirmPassword.error = null
            }

            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password == confirmPassword) {
                etConfirmPassword.error = null
            }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {

                    val userId = auth.currentUser?.uid

                    val userMap = hashMapOf(
                        "email" to email,
                        "role" to selectedRole
                    )

                    firestore.collection("users")
                        .document(userId!!)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

    }
}