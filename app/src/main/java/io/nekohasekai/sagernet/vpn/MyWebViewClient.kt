package io.nekohasekai.sagernet.vpn

import android.webkit.WebView
import android.webkit.WebViewClient

class MyWebViewClient : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        // Return false to allow the WebView to load the URL
        return false
    }
}