package com.example.squadup

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var imageViewProfile: ImageView
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)
        enableEdgeToEdge()

        imageViewProfile = findViewById(R.id.image_profile)
        val buttonTakePhoto = findViewById<Button>(R.id.button_take_photo)
        val buttonChooseImage = findViewById<Button>(R.id.button_choose_image)
        val buttonSkip = findViewById<Button>(R.id.button_skip)

        loadExistingProfilePicture()

        buttonTakePhoto.setOnClickListener {
            ImagePicker.with(this)
                .cameraOnly()  // Restrict mode to only capture image using Camera
                .crop()        // Enable image cropping
                .compress(1024) // Image compression size in KB
                .maxResultSize(1080, 1080) // Final image resolution will be less than 1080x1080
                .start()
        }

        buttonChooseImage.setOnClickListener {
            ImagePicker.with(this)
                .galleryOnly() // Restrict mode to pick image only from gallery
                .crop()        // Enable image cropping
                .compress(1024) // Image compression size in KB
                .maxResultSize(1080, 1080) // Final image resolution will be less than 1080x1080
                .start()
        }


        buttonSkip.setOnClickListener { finish() }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                updateFirebaseUserProfile(uri)
                imageViewProfile.setImageURI(uri) // Handle the loaded image directly to an ImageView
            } else {
                Toast.makeText(this, "Unable to retrieve image URI.", Toast.LENGTH_SHORT).show()
            }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateFirebaseUserProfile(uri: Uri) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to update profile picture.", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadExistingProfilePicture() {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val photoUri: Uri? = user.photoUrl

            if (photoUri != null) {
                imageViewProfile.setImageURI(photoUri)
            } else {
                imageViewProfile.setImageResource(R.drawable.profile_pic_placeholder)
            }
        } else {
            imageViewProfile.setImageResource(R.drawable.profile_pic_placeholder)
        }
    }
}
