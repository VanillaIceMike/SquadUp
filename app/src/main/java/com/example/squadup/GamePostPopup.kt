package com.example.squadup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
    private val firestore by lazy { FirebaseFirestore.getInstance() }
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
        Toast.makeText(context, "Opening post: $postId", Toast.LENGTH_SHORT).show()


        respondButton.setOnClickListener {
            postId?.let { id ->
                updatePlayersCount(id)
            }
            dismiss()
        }

        declineButton.setOnClickListener { dismiss() }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment_container) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        postId?.let { loadGamePostDetails(it) }

        return view
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
                } else {
                    Toast.makeText(context, "Game post not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePlayersCount(postId: String) {
        val postRef = firestore.collection("game_posts").document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentNumPlayers = snapshot.getLong("numPlayers") ?: 0
            val currentPlayersResponded = snapshot.getLong("numPlayersResponded") ?: 0

            if (currentNumPlayers > 0) {  // Only allow responding if there are players needed
                transaction.update(postRef, "numPlayers", currentNumPlayers - 1)
                transaction.update(postRef, "numPlayersResponded", currentPlayersResponded + 1)
            } else {
                throw IllegalStateException("No more players needed for this game post.")
            }
        }.addOnSuccessListener {
            Toast.makeText(context, "Thanks for responding. Player count updated!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to respond to post: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
    }
}