package io.nekohasekai.sagernet.vpn

import android.os.Bundle
import android.webkit.WebViewClient
import io.nekohasekai.sagernet.databinding.ActivityNotificationBinding


class NotificationActivity : BaseThemeActivity() {
    private lateinit var binding: ActivityNotificationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val intent = intent
        val url = intent.getStringExtra("link")

        // Enable JavaScript (if needed)
        binding.notificationWebView.settings.javaScriptEnabled = true
        binding.notificationWebView.settings.domStorageEnabled = true

        // Set a WebViewClient to handle page navigation within the WebView
        binding.notificationWebView.webViewClient = WebViewClient()

        // Load a web page
        if (url != null) {
            binding.notificationWebView.loadUrl(url)
        }
    }
}