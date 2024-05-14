package com.example.squadup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Assuming you have a button or some interaction in MainActivity
        // that leads to SignInActivity. If you don't, you need to add one in your XML.
        val signInButton: Button = findViewById(R.id.signInButton)
        signInButton.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        // Similarly, if there's a sign-up interaction.
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
