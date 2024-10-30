package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import io.nekohasekai.sagernet.databinding.ActivityWelcomeBinding
import io.nekohasekai.sagernet.vpn.repositories.LanguageSelectorRepository

class WelcomeActivity : BaseThemeActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        LanguageSelectorRepository.setupLanguageSelector(this, this, binding.spLanguageSelector)

        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
