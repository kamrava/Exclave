package io.nekohasekai.sagernet.vpn

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.ActivityForceUpdateBinding

class ForceUpdateActivity : BaseThemeActivity() {
    lateinit var binding: ActivityForceUpdateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_force_update)

        binding = ActivityForceUpdateBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.btnForceUpdate.setOnClickListener {
            redirectToPlayStore()
        }

    }

    private fun redirectToPlayStore() {
        val appPackageName = packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (e: ActivityNotFoundException) {
            // Play Store app is not installed, open the website.
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }
}
