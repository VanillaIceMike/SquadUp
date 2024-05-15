package com.example.squadup

import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.widget.Button
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val signInButton: Button = findViewById(R.id.signInButton)
        signInButton.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        val signUpButton: Button = findViewById(R.id.signUpButton)
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        FirebaseMessaging.getInstance().subscribeToTopic("game-updates")
            .addOnCompleteListener { task ->
                var msg = "Subscription successful"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("Firebase FCM", msg)
            }

        FirebaseMessaging.getInstance().subscribeToTopic("response-updates")
            .addOnCompleteListener { task ->
                var msg = "Subscription successful"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("Firebase FCM", msg)
            }
    }
}

