package com.myradio.deepradio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myradio.deepradio.presentation.SettingsViewModel
import com.myradio.deepradio.presentation.theme.DeepRadioTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeepRadioTheme {
                SettingsScreen(
                    onBackPress = { finish() },
                    onStopService = { stopBackgroundService(this) },
                    onCloseApp = { closeApp(this) },
                    onCheckVersion = { checkAppVersion(this) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPress: () -> Unit,
    onStopService: () -> Unit,
    onCloseApp: () -> Unit,
    onCheckVersion: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Создаем ViewModel без Hilt
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAbout by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Настройки",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPress) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                IconButton(onClick = { showDialog = "drawer" }) {
                    Icon(Icons.Default.Menu, contentDescription = "Меню")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Playback Settings Card
            SettingsCard(
                title = "Воспроизведение",
                icon = Icons.Default.PlayArrow
            ) {
                SettingItem(
                    title = "Фоновое воспроизведение",
                    subtitle = "Продолжать воспроизведение в фоне",
                    icon = Icons.Default.PlayCircle,
                    isChecked = uiState.backgroundPlaybackEnabled,
                    onCheckedChange = { viewModel.setBackgroundPlayback(it) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingItem(
                    title = "Только через Bluetooth наушники",
                    subtitle = "Воспроизводить только при подключенных BT-наушниках",
                    icon = Icons.Default.Bluetooth,
                    isChecked = uiState.onlyBluetoothPlaybackEnabled,
                    onCheckedChange = { viewModel.setOnlyBluetoothPlayback(it) }
                )
            }

            // Interface Settings Card
            SettingsCard(
                title = "Интерфейс",
                icon = Icons.Default.Tune
            ) {
                SettingItem(
                    title = "Автоповорот экрана",
                    subtitle = "Автоматически поворачивать экран",
                    icon = Icons.Default.ScreenRotation,
                    isChecked = uiState.autoRotateEnabled,
                    onCheckedChange = { viewModel.setAutoRotate(it) }
                )
            }

            // App Management Card
            SettingsCard(
                title = "Управление приложением",
                icon = Icons.Default.Settings
            ) {
                Column {
                    ActionButton(
                        title = "Остановить фоновый сервис",
                        subtitle = "Полностью остановить воспроизведение в фоне",
                        icon = Icons.Default.Stop,
                        onClick = onStopService,
                        color = MaterialTheme.colorScheme.error
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    ActionButton(
                        title = "Закрыть приложение",
                        subtitle = "Полностью закрыть Deep Radio",
                        icon = Icons.Default.ExitToApp,
                        onClick = onCloseApp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // About & Updates Card
            SettingsCard(
                title = "Информация",
                icon = Icons.Default.Info
            ) {
                ActionButton(
                    title = "Проверить обновления",
                    subtitle = "Проверить наличие новой версии в Play Store",
                    icon = Icons.Default.SystemUpdateAlt,
                    onClick = onCheckVersion,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Version info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Deep Radio",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Версия ${context.getString(R.string.version).removePrefix("Версия: ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Open Beta Test",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Dialog for menu
    if (showDialog == "drawer") {
        ModalBottomSheet(
            onDismissRequest = { showDialog = null }
        ) {
            NavigationMenu(
                onAbout = {
                    showDialog = null
                    showAbout = true
                },
                onAdd = {
                    showDialog = null
                    OpenAddStationActivity(context)
                },
                onSuggestions = {
                    showDialog = null
                    OpenSuggestionsActivity(context)
                },
                onPrivacy = {
                    showDialog = null
                    OpenPrivacyPolicy(context)
                }
            )
        }
    }

    // About dialog
    if (showAbout) {
        ShowAboutApp(context = context) { showAbout = false }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun NavigationMenu(
    onAbout: () -> Unit,
    onAdd: () -> Unit,
    onSuggestions: () -> Unit,
    onPrivacy: () -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 32.dp)
    ) {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        listOf(
            Triple("О приложении", Icons.Default.Info, onAbout),
            Triple("Добавить станцию", Icons.Default.Add, onAdd),
            Triple("Предложения", Icons.Default.Feedback, onSuggestions),
            Triple("Политика конфиденциальности", Icons.Default.PrivacyTip, onPrivacy)
        ).forEach { (title, icon, action) ->
            NavigationDrawerItem(
                label = { Text(title) },
                selected = false,
                onClick = action,
                icon = { Icon(icon, contentDescription = title) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
    }
}

// Utility functions
fun stopBackgroundService(context: Context) {
    val intent = Intent(context, RadioPlaybackService::class.java)
    context.stopService(intent)
}

fun closeApp(activity: Activity) {
    val stopServiceIntent = Intent(activity, RadioPlaybackService::class.java)
    activity.stopService(stopServiceIntent)
    activity.finishAffinity()
    android.os.Process.killProcess(android.os.Process.myPid())
}

fun checkAppVersion(context: Context) {
    val url = "https://play.google.com/store/apps/details?id=com.myradio.deepradio"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}