package io.nekohasekai.sagernet.vpn

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FacebookSdk.setClientToken("30d63da7ac404d3a92fe9c04a1baf590")
        FirebaseApp.initializeApp(this)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

    }
}
