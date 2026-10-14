package com.smizflark.aigile.messenger.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.BuildConfig
import com.smizflark.aigile.messenger.R

/** One open-source library the app uses: its name, licence and the page with the licence text. */
data class Licence(val name: String, val licence: String, val url: String)

/** The main libraries from app/build.gradle.kts (AII-220). Keep in step when a dependency is added. */
val LICENCES = listOf(
    Licence("Jetpack Compose and Material 3", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    Licence("AndroidX (Activity, Lifecycle, Navigation, Biometric)", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    Licence("WorkManager", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    Licence("OkHttp", "Apache 2.0", "https://github.com/square/okhttp/blob/master/LICENSE.txt"),
    Licence("kotlinx.serialization", "Apache 2.0", "https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt"),
    Licence("kotlinx.coroutines", "Apache 2.0", "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt"),
    Licence("Coil", "Apache 2.0", "https://github.com/coil-kt/coil/blob/main/LICENSE.txt"),
    Licence("ZXing Android Embedded", "Apache 2.0", "https://github.com/journeyapps/zxing-android-embedded/blob/master/LICENSE"),
)

/** About (AII-220): app name, version and the open-source licences; tap one to read it in the browser. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            title = { Text(stringResource(R.string.about)) },
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.build_number, BuildConfig.VERSION_CODE), color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(stringResource(R.string.licences), style = MaterialTheme.typography.titleMedium)
            LICENCES.forEach { l ->
                Column(Modifier.fillMaxWidth().clickable {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(l.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }.padding(vertical = 10.dp).testTag("licence")) {
                    Text(l.name)
                    Text(l.licence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
