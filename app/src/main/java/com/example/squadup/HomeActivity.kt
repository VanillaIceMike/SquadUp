package com.example.squadup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.squadup.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val profileNameTextView = findViewById<TextView>(R.id.user_name)
        val userProfileImageView = findViewById<ImageView>(R.id.user_profile_picture)
        val settingsIcon = findViewById<ImageView>(R.id.settings_icon)

        // Load user profile information

        // Settings icon click listener
        settingsIcon.setOnClickListener {

        }

        // Setup bottom navigation
        setupBottomNavigationView()
        loadUserName(profileNameTextView)
        loadUserProfilePicture(userProfileImageView)

    }

    private fun loadUserName(profileNameTextView: TextView) {
        val user: FirebaseUser? = auth.currentUser

        // If the user is authenticated
        user?.let {
            profileNameTextView.text = it.displayName ?: "Anonymous"
        } ?: run {
            // Fallback if the user is not logged in
            profileNameTextView.text = "Guest"
        }
    }

    private fun loadUserProfilePicture(userProfileImageView: ImageView) {
        val user: FirebaseUser? = auth.currentUser

        // If the user is authenticated
        user?.let {
            val profilePictureUri: Uri? = it.photoUrl

            if (profilePictureUri != null) {
                userProfileImageView.setImageURI(profilePictureUri)
            } else {
                userProfileImageView.setImageResource(R.drawable.profile_pic_placeholder)
            }
        }
    }

    private fun setupBottomNavigationView() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Listener for item selection in the BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    true
                }
                R.id.navigation_messages -> {
                    true
                }
                R.id.navigation_maps -> {
                    val intent = Intent(this, MapsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_notifcations -> {
                    // Placeholder for Notifications
                    Toast.makeText(this, "Notifications feature under development", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        // Set the Maps item as selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_home)
    }
}