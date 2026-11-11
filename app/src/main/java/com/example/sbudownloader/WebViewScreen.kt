package com.example.sbudownloader

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private val downloaderSites = listOf("youtube.com", "youtu.be", "tiktok.com", "facebook.com", "instagram.com", "twitter.com", "vimeo.com")

private fun isDownloaderSite(url: String): Boolean {
    val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
    return downloaderSites.any { host == it || host.endsWith(".$it") }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(startUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var progress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val bridge = remember {
        ContextMenuBridge(context) { url, mime, fileName ->
            try {
                val req = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mime)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                }
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(req)
            } catch (e: Exception) { }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        overScrollMode = WebView.OVER_SCROLL_NEVER
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }
                    clearCache(true)
                    CookieManager.getInstance().setAcceptCookie(true)
                    addJavascriptInterface(bridge, "AndroidContextMenu")

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            isLoading = newProgress < 100
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (isDownloaderSite(url)) {
                                AlertDialog.Builder(ctx)
                                    .setTitle("ভিডিও ডাউনলোড")
                                    .setMessage("আপনি কি এই ভিডিওটি ডাউনলোড করতে চান?\n\n$url")
                                    .setPositiveButton("হ্যাঁ") { _, _ ->
                                        LinkBridge.pendingUrl = url
                                        LinkBridge.requestTabSwitch = true
                                    }
                                    .setNegativeButton("না") { _, _ -> view?.loadUrl(url) }
                                    .show()
                                return true
                            }
                            return false
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(LongPressInjector.JS, null)
                            isLoading = false
                        }
                    }
                    loadUrl(startUrl)
                }
            }
        )
        if (isLoading) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        }
    }
}