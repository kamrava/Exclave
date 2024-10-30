package io.nekohasekai.sagernet.vpn

import io.nekohasekai.sagernet.R
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import io.nekohasekai.sagernet.databinding.ActivityWelcomeBinding
import java.util.Locale

class WelcomeActivity : BaseThemeActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLanguageSelector()

        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupLanguageSelector() {
        val languages = listOf(getString(R.string.language_english), getString(R.string.language_persian))
        val languageCodes = listOf("en", "fa")

        binding.spLanguageSelector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Set the spinner's selected position to the current language
        val currentLanguageCode = getSharedPreferences("app_settings", MODE_PRIVATE)
            .getString("language", Locale.getDefault().language)
        val currentLanguageIndex = languageCodes.indexOf(currentLanguageCode)
        if (currentLanguageIndex != -1) {
            binding.spLanguageSelector.setSelection(currentLanguageIndex)
        }

        binding.spLanguageSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedLanguage = languageCodes[position]
                if (selectedLanguage != currentLanguageCode) {
                    setLanguageLocale(selectedLanguage)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setLanguageLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)

        // Save language selection in SharedPreferences
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        sharedPreferences.edit().putString("language", languageCode).apply()

        // Restart activity to apply language changes
        val refresh = Intent(this, WelcomeActivity::class.java)
        refresh.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(refresh)
        finish()
    }
}
