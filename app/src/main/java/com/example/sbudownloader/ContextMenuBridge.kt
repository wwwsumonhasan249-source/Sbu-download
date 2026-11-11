package com.example.sbudownloader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class ContextMenuBridge(
    private val context: Context,
    private val onDownload: (url: String, mimeType: String, fileName: String) -> Unit
) {
    @JavascriptInterface
    fun requestDownload(url: String, mimeType: String, fileName: String) {
        if (url.isBlank()) {
            showToast("লিংক পাওয়া যায়নি")
            return
        }
        if (url.startsWith("blob:", true) || url.startsWith("data:", true)) {
            showToast("এই লিংকটি সরাসরি ডাউনলোড করা যাবে না")
            return
        }
        onDownload(url, mimeType.ifBlank { "application/octet-stream" }, fileName.ifBlank { "file_${System.currentTimeMillis()}" })
    }

    @JavascriptInterface
    fun requestCopy(text: String) {
        if (text.isBlank()) return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Link", text))
        showToast("কপি হয়েছে")
    }

    @JavascriptInterface
    fun showToast(message: String) {
        (context as? android.app.Activity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}