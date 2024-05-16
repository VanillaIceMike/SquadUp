package com.example.squadup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.squadup.databinding.ActivityCodeVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class CodeVerificationActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityCodeVerificationBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCodeVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()

        val verificationId = intent.getStringExtra("verificationId")

        binding.verifyBtn.setOnClickListener {
            val code = binding.codeEt.text.toString().trim()
            if (code.isNotEmpty()) {
                verifyCode(verificationId, code)
            } else {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Set the click listener for the back button to take the user back to SignInActivity
        val backButton: Button = findViewById(R.id.backBtn)
        backButton.setOnClickListener {
            // Redirect back to SignInActivity
            val signInIntent = Intent(this, SignInActivity::class.java)
            startActivity(signInIntent)
            finish()
        }
    }

    private fun verifyCode(verificationId: String?, code: String) {
        if (verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneAuthCredential(credential)
        } else {
            Toast.makeText(this, "Verification ID is null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification successful", Toast.LENGTH_SHORT).show()
                    // Redirect the user to HomeActivity
                    val homeIntent = Intent(this, HomeActivity::class.java)
                    homeIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(homeIntent)
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(
                        this,
                        "Verification failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}

