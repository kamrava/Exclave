package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.vpn.repositories.AdRepository

class WelcomeActivity : BaseThemeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // get init app settings from api server
        //AppRepository.getSettings()

//        AdRepository.checkAdConsent(this@WelcomeActivity)

        val loginBtn = findViewById<Button>(R.id.btn_login)
        loginBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        val regBtn = findViewById<Button>(R.id.btn_register)
        regBtn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}