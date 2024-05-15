package com.example.squadup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var noInternetDialog: NoInternetDialog

    private val networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context?.let {
                checkNetworkStatus(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        checkNetworkStatus(this)
    }

    override fun onResume() {
        super.onResume()
        checkNetworkStatus(this)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkChangeReceiver)
    }

    private fun checkNetworkStatus(context: Context) {
        if (!isNetworkAvailable(context)) {
            showNoInternetDialog()
        } else {
            hideNoInternetDialog()
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun showNoInternetDialog() {
        if (!::noInternetDialog.isInitialized) {
            noInternetDialog = NoInternetDialog(this)
        }
        if (!noInternetDialog.isShowing) {
            noInternetDialog.show()
        }
    }

    private fun hideNoInternetDialog() {
        if (::noInternetDialog.isInitialized && noInternetDialog.isShowing) {
            noInternetDialog.dismiss()
        }
    }
}


