package com.example.squadup

import GamePostAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.android.material.switchmaterial.SwitchMaterial

class HomeActivity : AppCompatActivity(){

    private lateinit var auth: FirebaseAuth
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var gamePostAdapter: GamePostAdapter
    private val gamePosts = mutableListOf<GamePost>()
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var mapViewToggle: SwitchMaterial

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

        val recyclerView = findViewById<RecyclerView>(R.id.game_posts_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        gamePostAdapter = GamePostAdapter(this, gamePosts)
        recyclerView.adapter = gamePostAdapter

        setupBottomNavigationView()
        loadUserName(profileNameTextView)
        loadUserProfilePicture(userProfileImageView)
        setupGamePostsListener()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("Firebase FCM", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            Log.d("Firebase FCM","Fetching FCM registration token: $token")
        })

        mapViewToggle = findViewById(R.id.view_toggle_switch)
        mapViewToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Start the MapsActivity
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
                // Optionally reset the toggle switch to off after switching activity
                mapViewToggle.isChecked = false
            }
        }

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

    private fun setupGamePostsListener() {
        listenerRegistration = firestore.collection("game_posts")
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    Toast.makeText(
                        this,
                        "Error loading game posts: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    val gamePost = change.document.toObject(GamePost::class.java).copy(id = change.document.id)

                    when (change.type) {
                        DocumentChange.Type.ADDED -> gamePosts.add(gamePost)
                        DocumentChange.Type.MODIFIED -> {
                            val index = gamePosts.indexOfFirst { it.id == gamePost.id }
                            if (index != -1) {
                                gamePosts[index] = gamePost
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val index = gamePosts.indexOfFirst { it.id == gamePost.id }
                            if (index != -1) {
                                gamePosts.removeAt(index)
                            }
                        }
                    }
                }

                gamePostAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    private fun requestNotificationPermission() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Notification Permission")
                .setMessage("We need permission to send you notifications. Please allow this in the next prompt.")
                .setPositiveButton("OK") { dialog, which ->
                    val intent = Intent()
                    intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    intent.putExtra("android.provider.extra.APP_PACKAGE", packageName)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
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
                R.id.navigation_addPost -> {
                    val intent = Intent(this, GamePostCreation::class.java)
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

