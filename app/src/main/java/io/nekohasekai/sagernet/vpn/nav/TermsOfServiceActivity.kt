package io.nekohasekai.sagernet.vpn.nav

import android.os.Bundle
import io.nekohasekai.sagernet.databinding.ActivityTermsofServiceBinding
import io.nekohasekai.sagernet.vpn.BaseThemeActivity

class TermsOfServiceActivity : BaseThemeActivity() {
    private lateinit var binding: ActivityTermsofServiceBinding // ViewBinding instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsofServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Set click listener for the icon
        binding.ivTermsOfServiceIconAngle.setOnClickListener {
            // Set result OK to indicate the action was successful
            setResult(RESULT_OK)
            // Finish the current activity
            finish()
        }
    }
}

