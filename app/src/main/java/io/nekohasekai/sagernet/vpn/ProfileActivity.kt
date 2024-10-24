package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import io.nekohasekai.sagernet.databinding.ActivityProfileBinding
import io.nekohasekai.sagernet.vpn.repositories.SocialAuthRepository

class ProfileActivity : BaseThemeActivity() {
    private lateinit var binding: ActivityProfileBinding // Declare binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the current user from Firebase Auth
        val currentUser = SocialAuthRepository.firebaseAuth.currentUser
        if (currentUser != null) {
            binding.displayNameTextView.text = "Display Name: ${currentUser.displayName}"
            binding.emailTextView.text = "Email: ${currentUser.email}"
        }

        // Set click listener for the sign out button using binding
        binding.signOutButton.setOnClickListener {
            SocialAuthRepository.facebookLoginManager.logOut()
            SocialAuthRepository.firebaseAuth.signOut()
            SocialAuthRepository.googleSignInClient.signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

