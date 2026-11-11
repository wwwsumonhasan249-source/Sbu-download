package com.example.sbudownloader

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Log.e("SBUDownloader", "Init failed", e)
            Toast.makeText(this, "ডাউনলোড ইঞ্জিন আরম্ভ ব্যর্থ", Toast.LENGTH_LONG).show()
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

object LinkBridge {
    var pendingUrl by mutableStateOf<String?>(null)
    var requestTabSwitch by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    LaunchedEffect(LinkBridge.requestTabSwitch) {
        if (LinkBridge.requestTabSwitch) {
            selectedTab = 1
            LinkBridge.requestTabSwitch = false
        }
    }
    val tabs = listOf(
        TabItem("Web", Icons.Default.Language),
        TabItem("Downloader", Icons.Default.Download)
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> WebViewScreen(startUrl = "https://sbu-minibrowser.kesug.com", modifier = Modifier.padding(padding))
            1 -> DownloaderScreen(modifier = Modifier.padding(padding))
        }
    }
}
data class TabItem(val label: String, val icon: ImageVector)