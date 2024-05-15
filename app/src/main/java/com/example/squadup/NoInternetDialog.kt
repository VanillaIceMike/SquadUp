package com.example.squadup

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.TextView

class NoInternetDialog(context: Context) : Dialog(context) {

    init {
        setCancelable(false)  // Make the dialog not cancelable
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_no_internet)

        // Initialize dialog views if needed
        val messageTextView: TextView = findViewById(R.id.tv_no_internet_message)
        messageTextView.text = "You are disconnected from the internet. Please reconnect to use the app."
    }
}
