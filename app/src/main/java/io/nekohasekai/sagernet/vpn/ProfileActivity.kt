package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.vpn.repositories.SocialAuthRepository

class ProfileActivity : BaseThemeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val displayNameTextView: TextView = findViewById(R.id.displayNameTextView)
        val emailTextView: TextView = findViewById(R.id.emailTextView)
        val signOutButton: Button = findViewById(R.id.signOutButton)

        val currentUser = SocialAuthRepository.firebaseAuth.currentUser
        if (currentUser != null) {
            displayNameTextView.text = "Display Name: ${currentUser.displayName}"
            emailTextView.text = "Email: ${currentUser.email}"
        }

        signOutButton.setOnClickListener {
            SocialAuthRepository.facebookLoginManager.logOut()
            SocialAuthRepository.firebaseAuth.signOut()
            SocialAuthRepository.googleSignInClient.signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
