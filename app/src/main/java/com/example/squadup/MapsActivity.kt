package com.example.squadup

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import java.util.Locale


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequestCode = 1000
    private lateinit var clusterManager: ClusterManager<SportsPosting>


    data class SportsPosting(
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
        val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionRequestCode)
        }

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
            addGameMarkers()

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentUserLocation = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 14f))
                }
            }
        }
    }

    private fun getSportsPostings(): List<SportsPosting> {
        return listOf(
            SportsPosting("Pickleball", LatLng(37.3341954, -121.8801998)),
            SportsPosting("Spikeball", LatLng(37.334774, -121.883428)),
            SportsPosting("Spikeball", LatLng(37.327838, -121.892740)),
            SportsPosting("Soccer", LatLng(37.321568, -121.865880)),
            SportsPosting("Tennis", LatLng(37.335107, -121.865930)),
            SportsPosting("Soccer", LatLng(37.333621, -121.897648)),
            SportsPosting("Tennis", LatLng(37.334548, -121.897828)),
            SportsPosting("Baseball", LatLng(37.347458, -121.872388)),
            SportsPosting("Soccer", LatLng(37.348693, -121.870857)),
            SportsPosting("Basketball", LatLng(37.356830, -121.875342)),
            SportsPosting("Soccer", LatLng(37.358983, -121.876360)),
            SportsPosting("Golf", LatLng(37.377586, -121.889187)),
            SportsPosting("Golf", LatLng(37.346729, -121.851398))
        )
    }

    private fun setupClusterManager() {
        // Initialize the ClusterManager
        clusterManager = ClusterManager<SportsPosting>(this, mMap)

        // Set a custom renderer that uses the custom icons defined in the SportsPosting class
        clusterManager.renderer = object : DefaultClusterRenderer<SportsPosting>(this, mMap, clusterManager) {
            override fun onBeforeClusterItemRendered(item: SportsPosting, markerOptions: MarkerOptions) {
                // Use the getMarkerIcon method from the SportsPosting instance to set the marker icon
                markerOptions.icon(getIconForType(item.type))
            }
        }

        // Set the map's listeners for the cluster manager
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)
    }

    private fun addGameMarkers() {
        val sportsPostings = getSportsPostings() // Assuming this function returns your postings
        // Just add the items to the cluster manager
        sportsPostings.forEach { posting ->
            clusterManager.addItem(posting)
        }
        clusterManager.cluster() // Force a re-cluster
    }

    private fun setupBottomNavigationView() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Listener for item selection in the BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Placeholder for Home
                    Toast.makeText(this, "Home feature under development", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_messages -> {
                    // Placeholder for Messages
                    Toast.makeText(this, "Messages feature under development", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_maps -> {
                    // Already in Maps Activity, no action needed
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
        bottomNavigationView.setSelectedItemId(R.id.navigation_maps)
    }

}