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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class NotificationsDisplayActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications_display)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val profileNameTextView = findViewById<TextView>(R.id.user_name)
        val userProfileImageView = findViewById<ImageView>(R.id.user_profile_picture)

        auth = FirebaseAuth.getInstance()

        setupBottomNavigationView()
        loadUserName(profileNameTextView)
        loadUserProfilePicture(userProfileImageView)
        setupRecyclerView()

        // Example notifications
        loadNotifications()
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

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                    true
                }
                R.id.navigation_addPost -> {
                    val intent = Intent(this, GamePostCreation::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                    true
                }
                R.id.navigation_notifcations -> {
                    // Current activity
                    true
                }
                else -> false
            }
        }

        // Set the Notifications item as selected
        bottomNavigationView.selectedItemId = R.id.navigation_notifcations
    }

    private fun setupRecyclerView() {
        notificationRecyclerView = findViewById(R.id.notification_recycler_view)
        notificationAdapter = NotificationAdapter(notificationList)
        notificationRecyclerView.adapter = notificationAdapter
        notificationRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadNotifications() {
        // This method should load notifications from your data source (e.g., Firebase Firestore)
        // Here are some example notifications
        val exampleNotifications = listOf(
            Notification("Game Created", "A new game has been created.", System.currentTimeMillis()),
            Notification("Game Response", "Someone responded to your game post.", System.currentTimeMillis() - 3600000)
        )

        notificationList.addAll(exampleNotifications)
        notificationAdapter.notifyDataSetChanged()
    }
}
