package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.ActivityEmailVerifyBinding
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class EmailVerify : BaseThemeActivity() {
    private lateinit var binding: ActivityEmailVerifyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerifyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val email: String = intent.getStringExtra("email").toString()
        val password: String = intent.getStringExtra("password").toString()


        binding.btnVerify.setOnClickListener {
            val verifyCode = binding.txtVerifyCode.text.toString()

            if (verifyCode.isEmpty()) {
                return@setOnClickListener
            }

            // Change button text
            binding.btnVerify.text = getString(R.string.verifying)
            // Show progress bar
            binding.progressBarVerify.visibility = View.VISIBLE

            // Perform register asynchronously
            GlobalScope.launch(Dispatchers.IO) {
                performVerify(email, password, verifyCode)

                // Update UI on the main thread after login completes
                withContext(Dispatchers.Main) {
                    // Revert button text and hide progress bar
                    binding.btnVerify.text = getString(R.string.verify)
                    binding.progressBarVerify.visibility = View.GONE
                }
            }
        }

        binding.tvResendVerifyCode.setOnClickListener {
            // Change button text
            binding.tvResendVerifyCode.text = getString(R.string.resending_verify_code)
            binding.tvResendVerifyCode.isEnabled = false
            binding.tvResendVerifyCode.isClickable = false

            GlobalScope.launch(Dispatchers.IO) {
                AuthRepository.checkEmailAvailabilityAndSendCode(email)

                // Update UI on the main thread after login completes
                withContext(Dispatchers.Main) {
                    // Revert button text and hide progress bar
                    binding.tvResendVerifyCode.text = getString(R.string.resend_verify_code)
                    binding.tvResendVerifyCode.isEnabled = true
                    binding.tvResendVerifyCode.isClickable = true
                    Toast.makeText(this@EmailVerify, "Verification code has been sent. Please check your email inbox", Toast.LENGTH_LONG).show()
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
                                binding.tvValidationError.visibility = View.INVISIBLE
                                navigateToDashboardActivity()
                            }
                        }
                        else -> {
                            runOnUiThread {
                                binding.tvValidationError.text = "Verify Code was wrong!"
                                binding.tvValidationError.visibility = View.VISIBLE
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