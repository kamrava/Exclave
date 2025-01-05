package io.nekohasekai.sagernet.vpn

import android.content.res.Configuration
import android.os.Bundle
import androidx.core.content.ContextCompat
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ui.ThemedActivity
import java.util.Locale

open class BaseThemeActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadLanguageLocale()

        // Change status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.navyBlue)

        // Change navigation bar color
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navyBlue)
    }
    private fun loadLanguageLocale() {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language", "en") // Default to English
        if (languageCode != null) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = Configuration()
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}
