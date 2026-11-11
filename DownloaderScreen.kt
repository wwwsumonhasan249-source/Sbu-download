package com.example.sbudownloader

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import java.io.File

data class FormatItem(val formatId: String, val label: String, val ext: String, val resolution: String?, val note: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var formats by remember { mutableStateOf<List<FormatItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var statusText by remember { mutableStateOf("লিংক পেস্ট করুন") }

    LaunchedEffect(LinkBridge.pendingUrl) {
        LinkBridge.pendingUrl?.let { incoming ->
            url = incoming
            LinkBridge.pendingUrl = null
            isLoading = true
            scope.launch(Dispatchers.IO) {
                try {
                    val info = YoutubeDL.getInstance().getInfo(incoming)
                    val parsed = info.formats?.mapNotNull { f ->
                        val id = f.formatId ?: return@mapNotNull null
                        if (f.vcodec == "none" && f.acodec == "none") return@mapNotNull null
                        FormatItem(id, "${f.ext?.uppercase()} ${f.resolution ?: ""}", f.ext ?: "?", f.resolution, f.formatNote ?: "")
                    } ?: emptyList()
                    withContext(Dispatchers.Main) { formats = parsed; isLoading = false }
                } catch (e: Exception) { withContext(Dispatchers.Main) { isLoading = false; statusText = "ব্যর্থ" } }
            }
        }
    }

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("ভিডিও লিংক") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { 
            if(url.isNotBlank()) {
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    try {
                        val info = YoutubeDL.getInstance().getInfo(url)
                        val parsed = info.formats?.mapNotNull { f ->
                            val id = f.formatId ?: return@mapNotNull null
                            FormatItem(id, "${f.ext?.uppercase()} ${f.resolution ?: ""}", f.ext ?: "?", f.resolution, f.formatNote ?: "")
                        } ?: emptyList()
                        withContext(Dispatchers.Main) { formats = parsed; isLoading = false }
                    } catch (e: Exception) { withContext(Dispatchers.Main) { isLoading = false } }
                }
            }
        }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("পার্স করুন") }
        
        if (downloadProgress != null) {
            LinearProgressIndicator(progress = { (downloadProgress!! / 100f) }, modifier = Modifier.fillMaxWidth())
        }
        Text(statusText, style = MaterialTheme.typography.bodySmall)

        LazyColumn {
            items(formats) { fmt ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt.label, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            downloadProgress = 0f
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SBU Downloader").apply { mkdirs() }
                                    val req = YoutubeDLRequest(url).addOption("-f", fmt.formatId).addOption("-o", "${dir.absolutePath}/%(title)s.%(ext)s")
                                    
                                    // ব্র্যান্ডিং যুক্ত করার FFmpeg কমান্ড
                                    req.addOption(
                                        "--postprocessor-args",
                                        "ffmpeg:-vf \"drawtext=text='SBU Browser':" +
                                            "x=w-tw-20:y=20:fontsize=22:fontcolor=white:fontstyle=bold:" +
                                            "box=1:boxcolor=black@0.6:boxborderw=8\"," +
                                            "drawtext=text='Developer\\: MD Sumon Hasan':" +
                                            "x=w-tw-20:y=20+th+14:fontsize=15:fontcolor=yellow@0.95\""
                                    )
                                    
                                    YoutubeDL.getInstance().execute(req) { p, _, _ -> downloadProgress = p }
                                    withContext(Dispatchers.Main) { downloadProgress = null; Toast.makeText(context, "সফল!", Toast.LENGTH_SHORT).show() }
                                } catch (e: Exception) { withContext(Dispatchers.Main) { downloadProgress = null } }
                            }
                        }) { Text("ডাউনলোড") }
                    }
                }
            }
        }
    }
}