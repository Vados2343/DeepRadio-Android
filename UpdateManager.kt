package com.myradio.deepradio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myradio.deepradio.domain.MediaManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaManager: MediaManager
) {
    val currentVersion: String
        get() = context.resources.getString(R.string.version).removePrefix("Версия: ")

    private val updateUrl = "https://raw.githubusercontent.com/Vados2343/UpdateManager/main/version"
    private val playStoreUrl = "https://play.google.com/store/apps/details?id=com.example.radioplayerbyvados2343"

    var showUpdateDialog by mutableStateOf(false)
        private set
    var latestVersion by mutableStateOf("")
        private set

    suspend fun checkForUpdates() {
        try {
            val latest = withContext(Dispatchers.IO) {
                java.net.URL(updateUrl).readText().trim()
            }
            latestVersion = latest
            if (isNewerVersion(latest, currentVersion)) {
                showUpdateDialog = true
            }
        } catch (_: Exception) {}
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val lp = latest.split(".").mapNotNull { it.toIntOrNull() }
        val cp = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(lp.size, cp.size)) {
            val l = lp.getOrNull(i) ?: 0
            val c = cp.getOrNull(i) ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun dismissUpdateDialog() {
        showUpdateDialog = false
    }

    fun openPlayStore(activity: Activity) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)))
        showUpdateDialog = false
    }

    fun closeApp(activity: Activity) {
        mediaManager.stop()
        showUpdateDialog = false
        activity.finishAffinity()
    }
}

@Composable
fun UpdateDialog(updateManager: UpdateManager) {
    if (!updateManager.showUpdateDialog) return

    var countdown by remember { mutableStateOf(30) }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        updateManager.closeApp(activity)
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdateAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Доступно обновление",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "v${updateManager.currentVersion}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )

                    Text(
                        " → ",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "v${updateManager.latestVersion}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "Приложение закроется через $countdown сек.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { (30 - countdown) / 30f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { updateManager.closeApp(activity) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Закрыть")
                    }

                    Button(
                        onClick = { updateManager.openPlayStore(activity) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Обновить")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Рекомендуем обновиться для получения новых функций и исправления ошибок",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}