package com.example.squadup

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var imageViewProfile: ImageView
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private lateinit var selectedDate: Calendar

    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var userName: EditText
    private lateinit var password: EditText
    private lateinit var dob: EditText
    private lateinit var bio: EditText
    private lateinit var logout: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)
        enableEdgeToEdge()

        selectedDate = Calendar.getInstance()

        imageViewProfile = findViewById(R.id.image_profile)
        val buttonEditImage = findViewById<Button>(R.id.button_edit_photo)
        val buttonSkip = findViewById<ImageView>(R.id.button_skip)
        val buttonUpdate = findViewById<Button>(R.id.updatebtn)
        firstName = findViewById<EditText>(R.id.firstName)
        lastName = findViewById<EditText>(R.id.lastName)
        userName = findViewById<EditText>(R.id.userName)
        password = findViewById<EditText>(R.id.password)
        dob = findViewById<EditText>(R.id.dob)
        bio = findViewById<EditText>(R.id.bio)
        logout = findViewById(R.id.logOut)

        loadExistingProfilePicture()
        getProfileData()

        buttonEditImage.setOnClickListener {
            showDialog()
        }

        dob.setOnClickListener { showDatePickerDialog() }

        buttonSkip.setOnClickListener { finish() }

        buttonUpdate.setOnClickListener {
            postProfileValues()
        }

        logout.setOnClickListener {
            firebaseAuth.signOut()
            val homeIntent = Intent(this, MainActivity::class.java)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(homeIntent)
        }

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

    private fun showDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog, null)
        val dialogBuilder = AlertDialog.Builder(this).apply {
            setView(dialogView)
        }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        // Accessing views from dialog layout
        val cameraButton = dialogView.findViewById<Button>(R.id.cameraBtn)
        val galleryButton = dialogView.findViewById<Button>(R.id.galleryBtn)

        // Button click listeners
        cameraButton.setOnClickListener {
            // Positive button clicked
            alertDialog.dismiss()

            ImagePicker.with(this)
                .cameraOnly()  // Restrict mode to only capture image using Camera
                .crop()        // Enable image cropping
                .compress(1024) // Image compression size in KB
                .maxResultSize(1080, 1080) // Final image resolution will be less than 1080x1080
                .start()
        }

        galleryButton.setOnClickListener {
            // Negative button clicked
            alertDialog.dismiss()

            ImagePicker.with(this)
                .galleryOnly() // Restrict mode to pick image only from gallery
                .crop()        // Enable image cropping
                .compress(1024) // Image compression size in KB
                .maxResultSize(1080, 1080) // Final image resolution will be less than 1080x1080
                .start()
        }
    }

    private fun getProfileData(){

        if (firebaseAuth.currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users_profile")
            .document(firebaseAuth.currentUser?.uid.toString()).get()
            .addOnSuccessListener { doc ->

                val firstNameStr = doc.get("firstName")
                val lastNameStr = doc.get("lastName")
                val bioStr = doc.get("bio")
                val birthdayStr = doc.get("birthday")

                if(firstNameStr!=null){
                    firstName.setText(firstNameStr.toString())
                }

                if(lastNameStr!=null){
                    lastName.setText(lastNameStr.toString())
                }

                if(bioStr!=null){
                    bio.setText(bioStr.toString())
                }

                if(birthdayStr!=null){
                    dob.setText(birthdayStr.toString())
                }

                // If the user is authenticated
                firebaseAuth.currentUser?.let {
                    userName.setText(it.displayName ?: "Anonymous")
                } ?: run {
                    // Fallback if the user is not logged in
                    userName.setText("Guest")
                }

            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error adding profile post: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }

    }

    private fun postProfileValues(){

        val userInfo = mapOf(
            "uid" to firebaseAuth.currentUser?.uid,
            "displayName" to (firebaseAuth.currentUser?.displayName ?: "Anonymous"),
            "profilePicUri" to (firebaseAuth.currentUser?.photoUrl?.toString() ?: "")
        )

        // Create a new game post document in Firestore
        val profileValues = hashMapOf(
            "firstName" to firstName.text.toString(),
            "lastName" to lastName.text.toString(),
            "bio" to bio.text.toString(),
            "birthday" to dob.text.toString(),
            "userInfo" to userInfo
        )

        if(password.text.isNotEmpty() && password.length()>7){
            updatePassword(password.text.toString())
        }else{
            Toast.makeText(
                this,
                "Invalid Password, password not changed",
                Toast.LENGTH_SHORT
            ).show()
        }


        firestore.collection("users_profile")
            .document(firebaseAuth.currentUser?.uid.toString()).set(profileValues)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Profile post added successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error adding profile post: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updatePassword(newPassword: String){
        firebaseAuth.currentUser?.let { currentUser ->

            // Update the user's password
            currentUser.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Password updated successfully
                        Log.d("FirebaseAuth", "User password updated successfully")
                        // You may want to notify the user that their password was successfully changed
                    } else {
                        // Password update failed
                        Log.w("FirebaseAuth", "User password update failed", task.exception)
                        // Handle the error appropriately, such as displaying a message to the user
                    }
                }
        }
    }

    private fun showDatePickerDialog() {
        val datePickerListener = DatePickerDialog.OnDateSetListener { view: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectedDate.set(year, month, dayOfMonth)
            val formattedDate = formatDate(selectedDate.time)
            // Update the EditText or TextView with the formatted date
            dob.setText(formattedDate)
        }

        // Create a DatePickerDialog with the current date as the default selection
        val datePickerDialog = DatePickerDialog(this, datePickerListener, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))

        // Set the maximum date to the current date
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        // Show the DatePickerDialog
        datePickerDialog.show()
    }

    private fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }

}
