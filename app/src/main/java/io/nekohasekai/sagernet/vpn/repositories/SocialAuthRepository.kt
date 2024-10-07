package io.nekohasekai.sagernet.vpn.repositories

import android.annotation.SuppressLint
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth

object SocialAuthRepository {
    @SuppressLint("StaticFieldLeak")
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var facebookLoginManager: LoginManager
    lateinit var firebaseAuth: FirebaseAuth

    fun initializeFacebookLoginManager()  {
        facebookLoginManager = LoginManager.getInstance()
    }
}