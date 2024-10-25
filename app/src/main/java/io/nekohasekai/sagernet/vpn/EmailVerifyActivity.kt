package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.ActivityEmailVerifyBinding
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class EmailVerifyActivity : BaseThemeActivity() {

    lateinit var binding: ActivityEmailVerifyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email: String = intent.getStringExtra("email").toString()
        val password: String = intent.getStringExtra("password").toString()

        binding.btnVerify.setOnClickListener {

            binding.btnVerify.visibility = View.INVISIBLE
            binding.tvValidationError.visibility = View.INVISIBLE

            val verifyCode = binding.txtVerifyCode.text.toString()
            if (verifyCode.isNotEmpty()) {

                binding.laProgressBarVerify.playInProgressAnimation()

                // Perform login asynchronously
                lifecycleScope.launch(Dispatchers.IO) {
                    performVerify(email, password, verifyCode)
                }
            } else {
                binding.tvValidationError.visibility = View.VISIBLE
                binding.tvValidationError.text = getString(R.string.enter_your_verify_code)
                binding.laProgressBarVerify.playErrorAnimation {
                    binding.btnVerify.visibility = View.VISIBLE
                }
            }
        }

        binding.tvResendVerifyCode.setOnClickListener {
            // Change button text
            binding.tvResendVerifyCode.text = getString(R.string.resending_verify_code)
            binding.tvResendVerifyCode.isEnabled = false
            binding.tvResendVerifyCode.isClickable = false

            lifecycleScope.launch(Dispatchers.IO) {
                AuthRepository.checkEmailAvailabilityAndSendCode(email)

                // Update UI on the main thread after login completes
                withContext(Dispatchers.Main) {
                    // Revert button text and hide progress bar
                    binding.tvResendVerifyCode.text = getString(R.string.resend_verify_code)
                    binding.tvResendVerifyCode.isEnabled = true
                    binding.tvResendVerifyCode.isClickable = true
                    Toast.makeText(
                        this@EmailVerifyActivity,
                        "Verification code has been sent. Please check your email inbox",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun performVerify(email: String, password: String, verifyCode: String) {
        runBlocking {
            try {
                withContext(Dispatchers.IO) {
                    val responseCode = AuthRepository.verify(email, password, verifyCode)
                    when (responseCode) {
                        200 -> {
                            runOnUiThread {
                                lifecycleScope.launch {
                                    AppRepository.getServersAndImport(this@EmailVerifyActivity)
                                    navigateToDashboardActivity()
                                }
                            }
                        }

                        else -> {
                            runOnUiThread {
                                binding.tvValidationError.visibility = View.VISIBLE
                                binding.tvValidationError.text =
                                    getString(R.string.Verify_Code_is_wrong)
                                binding.laProgressBarVerify.playErrorAnimation {
                                    binding.btnVerify.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Request failed: ${e.message}")
            }
        }
    }

    private fun navigateToDashboardActivity() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}