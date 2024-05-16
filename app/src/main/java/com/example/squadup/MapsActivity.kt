package com.example.squadup

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.squadup.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequestCode = 1000
    private lateinit var clusterManager: ClusterManager<SportsPosting>
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var auth: FirebaseAuth
    private lateinit var mapViewToggle: SwitchMaterial

    data class SportsPosting(
        val id: String,
        val type: String,
        val location: LatLng
    ) : ClusterItem {
        override fun getPosition(): LatLng {
            return location
        }

        override fun getTitle(): String? {
            return type
        }

        override fun getSnippet(): String? {
            return "Tap for details"
        }
    }

    private fun getIconForType(type: String): BitmapDescriptor {
        val drawableId = when (type.lowercase(Locale.ROOT)) {
            "pickleball" -> R.drawable.pickleballmarker
            "spikeball" -> R.drawable.spikeballmarker
            "soccer" -> R.drawable.soccermarker
            "tennis" -> R.drawable.tennismarker
            "baseball" -> R.drawable.baseballmarker
            "basketball" -> R.drawable.basketballmarker
            "golf" -> R.drawable.golfmarker
            "bowling" -> R.drawable.bowlingmarker
            "football" -> R.drawable.footballmarker
            "volleyball" -> R.drawable.volleyballmarker
            else -> R.drawable.plainmarker
        }
        return drawableToBitmap(drawableId)
    }

    private fun drawableToBitmap(drawableId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, drawableId)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        auth = FirebaseAuth.getInstance()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val profileNameTextView = findViewById<TextView>(R.id.user_name)
        val userProfileImageView = findViewById<ImageView>(R.id.user_profile_picture)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionRequestCode)
        }

        binding.viewToggleSwitch.isChecked = true


        binding.viewToggleSwitch.isChecked = true
        binding.viewToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                // Return to HomeActivity when unchecked
                startActivity(Intent(this, HomeActivity::class.java))
            }
        }

        loadUserName(profileNameTextView)
        loadUserProfilePicture(userProfileImageView)
        setupBottomNavigationView()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
        } else {
            Toast.makeText(this, "Location permission is needed to show your current location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true

            setupClusterManager()
            setupMarkersListener()

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentUserLocation = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 14f))
                }
            }
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun setupClusterManager() {
        // Initialize the ClusterManager
        clusterManager = ClusterManager<SportsPosting>(this, mMap)

        // Set a custom renderer that uses the custom icons defined in the SportsPosting class
        clusterManager.renderer = object : DefaultClusterRenderer<SportsPosting>(this, mMap, clusterManager) {
            override fun onBeforeClusterItemRendered(item: SportsPosting, markerOptions: MarkerOptions) {
                markerOptions.icon(getIconForType(item.type))
            }
        }

        // Set listeners for cluster manager
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)

        clusterManager.setOnClusterItemClickListener { sportsPosting ->
            val dialog = GamePostPopup().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", sportsPosting.id)  // Pass any other necessary data here
                }
            }
            dialog.show(supportFragmentManager, "GamePostPopup")
            true  // Return true to indicate that we've handled the event
        }
    }

    private fun setupMarkersListener() {
        firestore.collection("game_posts")
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    Toast.makeText(
                        this,
                        "Error loading game posts: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                clusterManager.clearItems()

                snapshots?.documents?.forEach { document ->
                    val id = document.id
                    val type = document.getString("sportType")?.lowercase(Locale.ROOT) ?: "unknown"
                    val location = document.get("location") as? Map<String, Double>
                    val latitude = location?.get("latitude") ?: 0.0
                    val longitude = location?.get("longitude") ?: 0.0
                    val posting = SportsPosting(id, type, LatLng(latitude, longitude))

                    clusterManager.addItem(posting)
                }

                clusterManager.cluster() // Force a re-cluster
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
                    val intent = Intent(this, NotificationsDisplayActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set the Maps item as selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_home)
    }
}
