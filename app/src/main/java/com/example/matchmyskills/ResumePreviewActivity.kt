package com.example.matchmyskills

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.net.URLEncoder

class ResumePreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resume_preview)

        val webView: WebView = findViewById(R.id.resume_web_view)
        val closeButton: MaterialButton = findViewById(R.id.btn_close_resume)

        val rawResumeUrl = intent.getStringExtra("resume_url")?.trim().orEmpty()
        if (!rawResumeUrl.startsWith("http://") && !rawResumeUrl.startsWith("https://")) {
            Toast.makeText(this, "Invalid resume URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val encodedResumeUrl = URLEncoder.encode(rawResumeUrl, "UTF-8")
        val previewUrl = "https://docs.google.com/gview?embedded=true&url=$encodedResumeUrl"

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                request: android.webkit.WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                Toast.makeText(this@ResumePreviewActivity, "Failed to open resume preview", Toast.LENGTH_SHORT).show()
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(previewUrl)

        closeButton.setOnClickListener { finish() }
    }

    override fun onDestroy() {
        findViewById<WebView>(R.id.resume_web_view)?.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            destroy()
        }
        super.onDestroy()
    }
}
