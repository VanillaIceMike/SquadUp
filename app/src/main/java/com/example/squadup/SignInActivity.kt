package com.example.squadup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_sign_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()

        val signInButton: Button = findViewById(R.id.signInButton)
        signInButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        val signUpText: TextView = findViewById(R.id.textView)
        signUpText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        val forgotPasswordButton: Button = findViewById(R.id.forgotPasswordButton)
        forgotPasswordButton.setOnClickListener { view ->
            val resetMail = EditText(view.context)
            val passwordResetDialog = AlertDialog.Builder(view.context)
            passwordResetDialog.setTitle("Reset Password?")
            passwordResetDialog.setMessage("Enter Your Email To Receive Reset Link.")
            passwordResetDialog.setView(resetMail)

            passwordResetDialog.setPositiveButton("Yes") { _, _ ->
                val email = resetMail.text.toString()
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("Email Reset", "Email sent.")
                            Toast.makeText(this@SignInActivity, "Reset link sent to your email.", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("Email Reset", "Failed to send email.", task.exception)
                            Toast.makeText(this@SignInActivity, "Failed to send reset link.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            passwordResetDialog.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            passwordResetDialog.create().show()
        }

    }
}
