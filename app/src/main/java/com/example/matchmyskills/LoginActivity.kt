package com.example.matchmyskills
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {


    lateinit var auth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
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
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroupUserType)
        val socialLayout = findViewById<LinearLayout>(R.id.layout_social_signup)
        val tvRegister = findViewById<TextView>(R.id.tv_sign_up)
        val btn = findViewById<Button>(R.id.btn_login)
        val etemail = findViewById<TextInputEditText>(R.id.et_email)
        val etpassword = findViewById<TextInputEditText>(R.id.et_password)
        val tvOr = findViewById<TextView>(R.id.tv_or_sign_in)
        val tvTitle = findViewById<TextView>(R.id.tv_title)

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

                    R.id.btnAdmin -> {
                        tvTitle.text = "Admin Panel"
                        selectedRole = "admin"
                        socialLayout.visibility = View.GONE
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


            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {

                    val userId = auth.currentUser?.uid



                    // 👉 Fetch user role from Firestore
                    firestore.collection("users").document(userId!!)
                        .get()
                        .addOnSuccessListener { document ->

                            val role = document.getString("role")
                            if (role == null) {
                                Toast.makeText(this, "Role not found!", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // 🔥 MAIN FIX: Role validation
                            if (role != selectedRole) {
                                Toast.makeText(this, "Invalid role selected!", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            when (role) {
                                "student" -> {
                                    startActivity(Intent(this, StudentDashboard::class.java))
                                    finish()
                                }
                                "recruiter" -> {
                                    startActivity(Intent(this, RecruiterDashboard::class.java))
                                    finish()
                                }
                                "admin" -> {
                                    startActivity(Intent(this, AdminDashboard::class.java))
                                    finish()
                                }
                            }
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
        // 👉 Navigate to Register screen
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}