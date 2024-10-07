package io.nekohasekai.sagernet.vpn

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebViewClient
import io.nekohasekai.sagernet.databinding.ActivityForgotPasswordBinding
import io.nekohasekai.sagernet.vpn.repositories.AppRepository

class ForgotPasswordActivity : BaseThemeActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Enable JavaScript (if needed)
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true

        // Set a WebViewClient to handle page navigation within the WebView
        binding.webView.webViewClient = WebViewClient()

        // Load a web page
        val url = AppRepository.getUserResetPasswordUrl()
        binding.webView.loadUrl(url)
    }
}