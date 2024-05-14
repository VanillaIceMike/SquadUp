package com.example.squadup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GamePostPopup : DialogFragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var sportTypeTextView: TextView
    private lateinit var numPlayersTextView: TextView
    private lateinit var numPlayersRespondedTextView: TextView
    private lateinit var timeframeTextView: TextView
    private lateinit var authorImageView: ImageView
    private lateinit var authorNameTextView: TextView
    private lateinit var respondButton: Button
    private lateinit var declineButton: Button
    private var postId: String? = null
    private var accepted = false
    private var declined = false
    private var authorId: String? = null
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserId by lazy { FirebaseAuth.getInstance().currentUser?.uid }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_game_post_popup, container, false)
        sportTypeTextView = view.findViewById(R.id.text_view_sport_type)
        numPlayersTextView = view.findViewById(R.id.text_view_num_players)
        numPlayersRespondedTextView = view.findViewById(R.id.text_view_num_responded)
        timeframeTextView = view.findViewById(R.id.text_view_timeframe)
        authorImageView = view.findViewById(R.id.image_view_author)
        authorNameTextView = view.findViewById(R.id.text_view_author_name)
        respondButton = view.findViewById(R.id.button_accept)
        declineButton = view.findViewById(R.id.button_decline)

        postId = arguments?.getString("POST_ID")

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment_container) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        postId?.let {
            loadGamePostDetails(it)
        }

        return view
    }

    private fun isPostAccepted(postId: String): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("accepted_$postId", false)
    }

    private fun isPostDeclined(postId: String): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("declined_$postId", false)
    }

    private fun setButtonStates(isAccepted: Boolean, isDeclined: Boolean) {
        accepted = isAccepted
        declined = isDeclined
        respondButton.isEnabled = !isAccepted
        declineButton.isEnabled = isAccepted
        respondButton.text = if (isAccepted) "Accepted" else "Accept"
        respondButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), if (isAccepted) android.R.color.darker_gray else android.R.color.holo_green_light)
        )
        declineButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), if (isAccepted) android.R.color.holo_red_light else android.R.color.darker_gray)
        )
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
                    val numPlayerResponded = document.getLong("numPlayersResponded") ?: 0
                    numPlayersRespondedTextView.text = "Players Responded: $numPlayerResponded"

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

                    // Get author ID
                    authorId = userInfo?.get("userId") as? String

                    // Check if the current user is the author and disable buttons if so
                    if (currentUserId == authorId) {
                        disableResponseButtons()
                    } else {
                        setupButtonListeners()
                    }

                    // Set initial button states
                    setButtonStates(isPostAccepted(postId), isPostDeclined(postId))
                } else {
                    Toast.makeText(requireContext(), "Game post not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtonListeners() {
        respondButton.setOnClickListener {
            postId?.let { id -> updatePlayersCount(id, true) }
        }

        declineButton.setOnClickListener {
            postId?.let { id -> updatePlayersCount(id, false) }
        }
    }

    private fun disableResponseButtons() {
        respondButton.isEnabled = false
        declineButton.isEnabled = false
        respondButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        )
        declineButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        )
    }

    private fun updatePlayersCount(postId: String, isAccepting: Boolean) {
        val postRef = firestore.collection("game_posts").document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentNumPlayers = snapshot.getLong("numPlayers") ?: 0
            val currentPlayersResponded = snapshot.getLong("numPlayersResponded") ?: 0
            val hasAccepted = snapshot.getBoolean("hasAccepted") ?: false
            val hasDeclined = snapshot.getBoolean("hasDeclined") ?: false

            if (isAccepting) {
                if (!hasAccepted) {
                    if (currentNumPlayers > 0) {
                        transaction.update(postRef, "numPlayers", currentNumPlayers - 1)
                        transaction.update(postRef, "numPlayersResponded", currentPlayersResponded + 1)
                        transaction.update(postRef, "hasAccepted", true)
                        transaction.update(postRef, "hasDeclined", false) // Reset declined state
                    } else {
                        throw IllegalStateException("No more players needed for this game post.")
                    }
                }
            } else {
                if (hasAccepted) { // Only decline if previously accepted
                    transaction.update(postRef, "numPlayers", currentNumPlayers + 1)
                    transaction.update(postRef, "numPlayersResponded", currentPlayersResponded - 1)
                    transaction.update(postRef, "hasAccepted", false)
                    transaction.update(postRef, "hasDeclined", true)
                }
            }
        }.addOnSuccessListener {
            if (isAdded) {
                val message = if (isAccepting) "Thanks for responding. Player count updated!" else "You've declined the post."
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putBoolean("accepted_$postId", isAccepting).apply()
                sharedPreferences.edit().putBoolean("declined_$postId", !isAccepting).apply()

                setButtonStates(isAccepting, !isAccepting)
                updateUIAfterResponse(postId)
            }
        }.addOnFailureListener { e ->
            if (isAdded) {
                Toast.makeText(requireContext(), "Failed to respond to post: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUIAfterResponse(postId: String) {
        firestore.collection("game_posts").document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val numPlayers = document.getLong("numPlayers") ?: 0
                    numPlayersTextView.text = "Players Wanted: $numPlayers"
                    val numPlayersResponded = document.getLong("numPlayersResponded") ?: 0
                    numPlayersRespondedTextView.text = "Players Responded: $numPlayersResponded"
                }
            }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
    }
}


