package io.nekohasekai.sagernet.vpn.repositories

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import io.nekohasekai.sagernet.R
import java.util.Locale

object LanguageSelectorRepository {

    fun setupLanguageSelector(
        context: Context,
        activity: Activity,
        languageSpinner: Spinner
    ) {
        val languages = listOf(
            context.getString(R.string.language_english),
            context.getString(R.string.language_Arabic),
            context.getString(R.string.language_German),
            context.getString(R.string.language_Spanish),
            context.getString(R.string.language_French),
            context.getString(R.string.language_Indonesian),
            context.getString(R.string.language_Italian),
            context.getString(R.string.language_Norwegian),
            context.getString(R.string.language_Russian),
            context.getString(R.string.language_Turkish),
            context.getString(R.string.language_Ukrainian),
            context.getString(R.string.language_Chinese),
            context.getString(R.string.language_Taiwanese)
        )
        val languageCodes = listOf("en", "ar", "de", "es", "fr", "in", "it", "no", "ru", "tr", "uk", "zh", "tw")

        languageSpinner.adapter = ArrayAdapter(
            context,
            R.layout.spinner_item,
            languages
        ).apply {
            setDropDownViewResource(R.layout.spinner_each_item)
        }

        // Set the spinner's selected position to the current language
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentLanguageCode =
            sharedPreferences.getString("language", Locale.getDefault().language)
        val currentLanguageIndex = languageCodes.indexOf(currentLanguageCode)
        if (currentLanguageIndex != -1) {
            languageSpinner.setSelection(currentLanguageIndex)
        }

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: android.view.View,
                position: Int,
                id: Long
            ) {
                val selectedLanguage = languageCodes[position]
                if (selectedLanguage != currentLanguageCode) {
                    setLanguageLocale(context, activity, selectedLanguage)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setLanguageLocale(context: Context, activity: Activity, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // Save language selection in SharedPreferences
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("language", languageCode).apply()

        // Restart activity to apply language changes
        val refresh = Intent(context, activity::class.java)
        refresh.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        activity.startActivity(refresh)
        activity.finish()
    }
}
