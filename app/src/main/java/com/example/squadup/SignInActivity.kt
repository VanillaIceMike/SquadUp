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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class SignInActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var phoneNumber: String  // You need to store the user's phone number


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
        // Properly link the signInUser function to the onClick listener
        signInButton.setOnClickListener {
            signInUser()
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

    private fun signInUser() {
        val email = findViewById<EditText>(R.id.emailEt).text.toString().trim()
        val password = findViewById<EditText>(R.id.passET).text.toString().trim()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val phoneNumber = user?.phoneNumber ?: ""
                    if (phoneNumber.isNotEmpty()) {
                        startPhoneNumberVerification(phoneNumber)
                    } else {
                        Toast.makeText(this, "Phone number not found. Please ensure your account has a phone number associated.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Sign in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }


    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(phoneAuthCallbacks())
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun phoneAuthCallbacks() = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-verification case
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            println("Verification failed: ${e.localizedMessage}")
            Toast.makeText(this@SignInActivity, "Verification failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            val intent = Intent(this@SignInActivity, CodeVerificationActivity2::class.java)
            intent.putExtra("verificationId", verificationId)
            startActivity(intent)
        }
    }

    private fun showVerificationCodeDialog(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
        // Implementation similar to the one in SignUpActivity
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Redirect the user to the main activity or another activity as necessary
                Toast.makeText(this, "Successfully signed in", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sign in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
