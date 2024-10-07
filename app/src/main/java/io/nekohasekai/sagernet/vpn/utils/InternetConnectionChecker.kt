package io.nekohasekai.sagernet.vpn.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import io.nekohasekai.sagernet.vpn.repositories.AdRepository
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import java.io.IOException


class InternetConnectionChecker(private val context: Context) {
    private val handler = Handler()
    private var isChecking = false

    private val checkInternetConnectionRunnable = object : Runnable {
        override fun run() {
            if (isConnected()) {
                println("HAMED_LOG_INTERNET_IS_ACTIVE")
                AdRepository.loadRewardedAd(context)
                // There is an active internet connection.
                // You can perform network operations here.
            } else {
                println("HAMED_LOG_INTERNET_IS_NOT_ACTIVE")
                // No internet connection.
                // Display a message or take appropriate action.
            }
            handler.postDelayed(this, 10000) // Check every 10 seconds
        }
    }

    fun startChecking() {
        if (!isChecking) {
            isChecking = true
            handler.post(checkInternetConnectionRunnable)
        }
    }

    fun stopChecking() {
        if (isChecking) {
            isChecking = false
            handler.removeCallbacks(checkInternetConnectionRunnable)
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo

        return networkInfo != null && networkInfo.isConnected
    }

    @Throws(InterruptedException::class, IOException::class)
    fun isConnected(): Boolean {
        val command = "ping -c 1 google.com"
        val status = Runtime.getRuntime().exec(command).waitFor() == 0
        AppRepository.setLastInternetSatus(status)
        return status
    }


}
