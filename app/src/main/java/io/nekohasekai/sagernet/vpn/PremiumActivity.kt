package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import io.nekohasekai.sagernet.R

class PremiumActivity : BaseThemeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)

        val ivIconAngle = findViewById<ImageView>(R.id.ivIconAngle)
        ivIconAngle.setOnClickListener { navigateToDashboard() }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}