package com.example.squadup

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class GamePostCreation : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleMap: GoogleMap
    private lateinit var marker: Marker
    private val defaultLocation = LatLng(37.3382, -121.8863)

    private lateinit var sportTypeEditText: EditText
    private lateinit var numPlayersEditText: EditText
    private lateinit var timeframeEditText: EditText
    private lateinit var addPostButton: Button
    private lateinit var backButton: Button
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_post_creation)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val profileNameTextView = findViewById<TextView>(R.id.user_name)
        val userProfileImageView = findViewById<ImageView>(R.id.user_profile_picture)

        sportTypeEditText = findViewById(R.id.edit_text_sport_type)
        numPlayersEditText = findViewById(R.id.edit_text_num_players)
        timeframeEditText = findViewById(R.id.edit_text_timeframe)
        addPostButton = findViewById(R.id.button_add_post)
        backButton = findViewById(R.id.button_back)

        loadUserName(profileNameTextView)
        loadUserProfilePicture(userProfileImageView)

        // Initialize the map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment_container) as SupportMapFragment
        mapFragment.getMapAsync(this)

        timeframeEditText.setOnClickListener { showDateTimePickerDialog() }

        addPostButton.setOnClickListener { addGamePost() }
        backButton.setOnClickListener { finish() }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Add a draggable marker at the default location (San Jose)
        marker = googleMap.addMarker(
            MarkerOptions()
                .position(defaultLocation)
                .draggable(true) // Ensure marker is draggable
                .title("Drag to choose location")
        )!!

        // Move the camera to the default location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

        // Disable map scrolling when dragging starts
        googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
                googleMap.uiSettings.isScrollGesturesEnabled = false
            }

            override fun onMarkerDrag(marker: Marker) {}

            override fun onMarkerDragEnd(marker: Marker) {
                googleMap.uiSettings.isScrollGesturesEnabled = true
                Toast.makeText(
                    this@GamePostCreation,
                    "Location selected: ${marker.position.latitude}, ${marker.position.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun addGamePost() {
        val sportType = sportTypeEditText.text.toString().trim()
        val numPlayersStr = numPlayersEditText.text.toString().trim()
        val timeframe = timeframeEditText.text.toString().trim()

        // Validate input
        if (sportType.isEmpty() || numPlayersStr.isEmpty() || timeframe.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        val numPlayers = numPlayersStr.toIntOrNull()
        if (numPlayers == null || numPlayers <= 0) {
            Toast.makeText(this, "Enter a valid number of players", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userInfo = mapOf(
            "uid" to user.uid,
            "displayName" to (user.displayName ?: "Anonymous"),
            "profilePicUri" to (user.photoUrl?.toString() ?: "")
        )

        // Create a new game post document in Firestore
        val newGamePost = hashMapOf(
            "sportType" to sportType,
            "numPlayers" to numPlayers,
            "numPlayersResponded" to 0,
            "timeframe" to timeframe,
            "location" to mapOf(
                "latitude" to marker.position.latitude,
                "longitude" to marker.position.longitude
            ),
            "userInfo" to userInfo
        )

        firestore.collection("game_posts")
            .add(newGamePost)
            .addOnSuccessListener { documentReference ->
                val documentId = documentReference.id
                Toast.makeText(
                    this,
                    "Game post added successfully with ID: $documentId",
                    Toast.LENGTH_SHORT
                ).show()
                finish() // Go back to the previous activity
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error adding game post: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showDateTimePickerDialog() {
        val calendar = Calendar.getInstance()

        // Date Picker Dialog
        val datePickerDialog = DatePickerDialog(this,
            { _, year, month, dayOfMonth ->
                selectedDateTime.set(year, month, dayOfMonth)
                showTimePickerDialog()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()

        // Time Picker Dialog
        val timePickerDialog = TimePickerDialog(this,
            { _, hourOfDay, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDateTime.set(Calendar.MINUTE, minute)

                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                timeframeEditText.setText(formatter.format(selectedDateTime.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
        )
        timePickerDialog.show()
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
}
