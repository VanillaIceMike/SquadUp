package com.example.squadup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var gamePostAdapter: GamePostAdapter
    private val gamePosts = mutableListOf<GamePost>()
    private var listenerRegistration: ListenerRegistration? = null

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
        val createPostButton: View = findViewById(R.id.fab_add_game_post)

        val recyclerView = findViewById<RecyclerView>(R.id.game_posts_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        gamePostAdapter = GamePostAdapter(this, gamePosts)
        recyclerView.adapter = gamePostAdapter

        // Settings icon click listener
        settingsIcon.setOnClickListener {
            val intent = Intent(this, ProfileSetupActivity::class.java)
            startActivity(intent)
        }

        createPostButton.setOnClickListener {
            val intent = Intent(this, GamePostCreation::class.java)
            startActivity(intent)
        }

        setupBottomNavigationView()
        loadUserName(profileNameTextView)
        loadUserProfilePicture(userProfileImageView)
        setupGamePostsListener()
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

