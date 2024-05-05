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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val buttonTakePhoto = findViewById<Button>(R.id.button_take_photo)
        val buttonChooseImage = findViewById<Button>(R.id.button_choose_image)
        val buttonSkip = findViewById<Button>(R.id.button_skip)

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
            imageViewProfile.setImageURI(uri) // Handle the loaded image directly to an ImageView
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }
}