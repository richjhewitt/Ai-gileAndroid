package com.smizflark.aigile.messenger.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WearHome() } }
    }
}

@Composable
private fun WearHome() {
    val ctx = LocalContext.current
    var chats by remember { mutableStateOf<List<WearChat>>(emptyList()) }
    // One look on start, then every change the phone sends. No Play services on the watch = stay on "Waiting".
    LaunchedEffect(Unit) {
        runCatching {
            val buf = Wearable.getDataClient(ctx).dataItems.await()
            try {
                buf.firstOrNull { it.uri.path == WearChats.PATH }?.data?.let { chats = WearChats.parse(String(it)) }
            } finally { buf.release() }
        }
    }
    DisposableEffect(Unit) {
        val client = runCatching { Wearable.getDataClient(ctx) }.getOrNull()
        val l = DataClient.OnDataChangedListener { events: DataEventBuffer ->
            events.forEach { e -> if (e.dataItem.uri.path == WearChats.PATH) e.dataItem.data?.let { chats = WearChats.parse(String(it)) } }
        }
        runCatching { client?.addListener(l) }
        onDispose { runCatching { client?.removeListener(l) } }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        if (chats.isEmpty()) {
            Text(stringResource(R.string.waiting), textAlign = TextAlign.Center)
        } else {
            Text(if (chats.size == 1) stringResource(R.string.chats_one) else stringResource(R.string.chats_count, chats.size), textAlign = TextAlign.Center)
            Text(chats.first().name, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
