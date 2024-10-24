package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import io.nekohasekai.sagernet.databinding.ActivityPremiumBinding

class PremiumActivity : BaseThemeActivity() {
    private lateinit var binding: ActivityPremiumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivIconAngle.setOnClickListener { navigateToDashboard() }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}
