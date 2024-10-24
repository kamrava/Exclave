package io.nekohasekai.sagernet.vpn.nav

import android.os.Bundle
import io.nekohasekai.sagernet.databinding.ActivityPrivacyPolicyBinding
import io.nekohasekai.sagernet.vpn.BaseThemeActivity

class PrivacyPolicyActivity : BaseThemeActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding // Declare binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ivPrivacyPolicyIconAngle = binding.ivPrivacyPolicyIconAngle

        ivPrivacyPolicyIconAngle.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }
}

