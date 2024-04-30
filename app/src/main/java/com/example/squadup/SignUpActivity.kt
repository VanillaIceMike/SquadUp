package com.example.squadup

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.squadup.databinding.ActivitySignUpBinding
import android.content.Intent
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import java.util.concurrent.TimeUnit
import com.hbb20.CountryCodePicker



class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var ccp: CountryCodePicker


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        ccp = binding.countryCodePicker
        ccp.registerCarrierNumberEditText(binding.phoneEt)

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passET.text.toString().trim()
            val confirmPass = binding.confirmPassEt.text.toString().trim()
            val username = binding.usernameEt.text.toString().trim()
            val phoneNumber = ccp.fullNumberWithPlus.trim()


            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty() && username.isNotEmpty() && phoneNumber.isNotEmpty()) {
                if (pass == confirmPass) {
                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Update the user profile with their display name
                            val user = firebaseAuth.currentUser
                            val profileUpdates = userProfileChangeRequest {
                                displayName = username
                            }
                            user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileUpdateTask ->
                                if (profileUpdateTask.isSuccessful) {
                                    verifyPhoneNumber(phoneNumber) // Trigger phone number verification
                                } else {
                                    Toast.makeText(this, "Failed to update user profile.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun verifyPhoneNumber(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            // Link the phone number if the user exists and is signed in.
            user.linkWithCredential(credential).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Phone number linked", Toast.LENGTH_SHORT).show()
                    val signInIntent = Intent(this, SignInActivity::class.java)
                    signInIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(signInIntent)
                } else {
                    Toast.makeText(this, "Linking phone number failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Handle the case where there is no user signed in (optional based on your flow).
            Toast.makeText(this, "No user account found to link", Toast.LENGTH_SHORT).show()
        }
    }



    private fun showVerificationCodeDialog(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.activity_code_verification)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val codeEditText: EditText = dialog.findViewById(R.id.codeEt)
        val verifyButton: Button = dialog.findViewById(R.id.verifyBtn)
        val backButton: Button = dialog.findViewById(R.id.backBtn)

        verifyButton.setOnClickListener {
            val code = codeEditText.text.toString().trim()
            if (code.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
            }
        }

        backButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Optionally sign in the user directly if instant verification is successful
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@SignUpActivity, "Verification failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            // Show dialog with input for verification code
            showVerificationCodeDialog(verificationId, token)
        }
    }
}
