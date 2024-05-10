package com.example.squadup

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class GamePostDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var sportTypeTextView: TextView
    private lateinit var numPlayersTextView: TextView
    private lateinit var timeframeTextView: TextView
    private lateinit var authorImageView: ImageView
    private lateinit var authorNameTextView: TextView
    private lateinit var respondButton: Button
    private lateinit var backButton: Button
    private var postId: String? = null
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_post_details)

        // Initialize views
        sportTypeTextView = findViewById(R.id.text_view_sport_type)
        numPlayersTextView = findViewById(R.id.text_view_num_players)
        timeframeTextView = findViewById(R.id.text_view_timeframe)
        authorImageView = findViewById(R.id.image_view_author)
        authorNameTextView = findViewById(R.id.text_view_author_name)
        respondButton = findViewById(R.id.button_respond)
        backButton = findViewById(R.id.button_back)

        // Get postId from intent
        postId = intent.getStringExtra("POST_ID")

        // Set up back button
        backButton.setOnClickListener {
            finish()
        }

        // Set up respond button
        respondButton.setOnClickListener {
            postId?.let { id ->
                updatePlayersCount(id)
            }
        }

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment_container) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Load game post details
        postId?.let { loadGamePostDetails(it) }
    }

    private fun loadGamePostDetails(postId: String) {
        firestore.collection("game_posts").document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    sportTypeTextView.text = document.getString("sportType") ?: "Unknown"
                    val numPlayers = document.getLong("numPlayers") ?: 0
                    numPlayersTextView.text = "Players Wanted: $numPlayers"
                    timeframeTextView.text = document.getString("timeframe") ?: "Unknown"

                    val location = document.get("location") as? Map<String, Double>
                    val latitude = location?.get("latitude") ?: 0.0
                    val longitude = location?.get("longitude") ?: 0.0
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15f))
                    googleMap.addMarker(
                        MarkerOptions().position(LatLng(latitude, longitude)).title("Game Location")
                    )

                    val userInfo = document.get("userInfo") as? Map<*, *>
                    authorNameTextView.text = userInfo?.get("displayName") as? String ?: "Anonymous"
                    val profilePicUri = userInfo?.get("profilePicUri") as? String
                    if (!profilePicUri.isNullOrEmpty()) {
                        Glide.with(this).load(profilePicUri).into(authorImageView)
                    } else {
                        authorImageView.setImageResource(R.drawable.profile_pic_placeholder)
                    }
                } else {
                    Toast.makeText(this, "Game post not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePlayersCount(postId: String) {
        val docRef = firestore.collection("game_posts").document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentPlayers = snapshot.getLong("numPlayers") ?: 0
            if (currentPlayers > 0) {
                transaction.update(docRef, "numPlayers", currentPlayers - 1)
            } else {
                throw IllegalStateException("No players available to respond.")
            }
        }
            .addOnSuccessListener {
                Toast.makeText(this, "You have responded to the game post!", Toast.LENGTH_SHORT).show()
                loadGamePostDetails(postId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
    }
}



