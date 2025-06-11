package com.myradio.deepradio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myradio.deepradio.domain.PlaybackMode
import com.myradio.deepradio.domain.getDescription
import com.myradio.deepradio.domain.getDisplayName
import com.myradio.deepradio.domain.getRecommendation
import com.myradio.deepradio.presentation.SettingsViewModel
import com.myradio.deepradio.presentation.theme.AppTheme
import com.myradio.deepradio.presentation.theme.DeepRadioTheme
import com.myradio.deepradio.presentation.theme.getDescription
import com.myradio.deepradio.presentation.theme.getDisplayName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем edge-to-edge режим (убираем "подбородок")
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // ✅ ИСПРАВЛЕНО: Используем hiltViewModel() вместо viewModel
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DeepRadioTheme(appTheme = uiState.selectedTheme) {
                SettingsScreen(
                    viewModel = viewModel,
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
    viewModel: SettingsViewModel,
    onBackPress: () -> Unit,
    onStopService: () -> Unit,
    onCloseApp: () -> Unit,
    onCheckVersion: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAbout by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf<String?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPlaybackModeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // ✅ УЛУЧШЕННЫЙ TOP APP BAR
        EnhancedTopAppBar(
            onBackPress = onBackPress,
            onMenuClick = { showDialog = "drawer" },
            currentTheme = uiState.selectedTheme
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ✅ СТАТИСТИКА ПРИЛОЖЕНИЯ - НОВАЯ СЕКЦИЯ
            StatsCard()

            // Theme Settings Card
            SettingsCard(
                title = "Внешний вид",
                icon = Icons.Default.Palette,
                description = "Настройка темы и интерфейса"
            ) {
                ActionButton(
                    title = "Тема приложения",
                    subtitle = "Текущая: ${uiState.selectedTheme.getDisplayName()}",
                    icon = Icons.Default.ColorLens,
                    onClick = { showThemeDialog = true },
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Playback Settings Card
            SettingsCard(
                title = "Воспроизведение",
                icon = Icons.Default.PlayArrow,
                description = "Настройки качества и режимов"
            ) {
                // ✅ РЕЖИМ ВОСПРОИЗВЕДЕНИЯ
                ActionButton(
                    title = "Режим воспроизведения",
                    subtitle = "Текущий: ${uiState.selectedPlaybackMode?.getDisplayName() ?: "Мгновенный"}",
                    icon = Icons.Default.Speed,
                    onClick = { showPlaybackModeDialog = true },
                    color = MaterialTheme.colorScheme.secondary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

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
                icon = Icons.Default.Tune,
                description = "Настройки поведения интерфейса"
            ) {
                SettingItem(
                    title = "Автоповорот экрана",
                    subtitle = "Автоматически поворачивать экран",
                    icon = Icons.Default.ScreenRotation,
                    isChecked = uiState.autoRotateEnabled,
                    onCheckedChange = { viewModel.setAutoRotate(it) }
                )
            }

            // ✅ НОВАЯ СЕКЦИЯ: УВЕДОМЛЕНИЯ
            SettingsCard(
                title = "Уведомления",
                icon = Icons.Default.Notifications,
                description = "Настройки уведомлений и управления"
            ) {
                SettingItem(
                    title = "Показывать в панели уведомлений",
                    subtitle = "Управление воспроизведением через уведомления",
                    icon = Icons.Default.NotificationsActive,
                    isChecked = true, // Всегда включено для радио
                    onCheckedChange = { /* Заглушка */ }
                )
            }

            // App Management Card
            SettingsCard(
                title = "Управление приложением",
                icon = Icons.Default.Settings,
                description = "Системные функции и контроль"
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
                icon = Icons.Default.Info,
                description = "О приложении и обновлениях"
            ) {
                ActionButton(
                    title = "Проверить обновления",
                    subtitle = "Проверить наличие новой версии в Play Store",
                    icon = Icons.Default.SystemUpdateAlt,
                    onClick = onCheckVersion,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // ✅ УЛУЧШЕННАЯ VERSION INFO
            EnhancedVersionCard()
        }
    }

    // Dialog for menu
    if (showDialog == "drawer") {
        ModalBottomSheet(
            onDismissRequest = { showDialog = null },
            windowInsets = WindowInsets(0)
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

    // Theme Selection Dialog
    if (showThemeDialog) {
        EnhancedThemeSelectionDialog(
            currentTheme = uiState.selectedTheme,
            onThemeSelected = { theme ->
                viewModel.setTheme(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Playback Mode Selection Dialog
    if (showPlaybackModeDialog) {
        EnhancedPlaybackModeSelectionDialog(
            currentMode = uiState.selectedPlaybackMode ?: PlaybackMode.INSTANT,
            onModeSelected = { mode ->
                viewModel.setPlaybackMode(mode)
                showPlaybackModeDialog = false
            },
            onDismiss = { showPlaybackModeDialog = false }
        )
    }

    // About dialog
    if (showAbout) {
        ShowAboutApp(context = context) { showAbout = false }
    }
}

// ✅ УЛУЧШЕННЫЙ TOP APP BAR
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTopAppBar(
    onBackPress: () -> Unit,
    onMenuClick: () -> Unit,
    currentTheme: AppTheme
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Column {
                    Text(
                        "Настройки",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Тема: ${currentTheme.getDisplayName()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackPress) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
        },
        actions = {
            IconButton(onClick = onMenuClick) {
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
}

// ✅ НОВАЯ СТАТИСТИКА
@Composable
fun StatsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Radio,
                value = "134+",
                label = "Станций"
            )
            StatItem(
                icon = Icons.Default.Favorite,
                value = "12",
                label = "Избранных"
            )
            StatItem(
                icon = Icons.Default.Equalizer,
                value = "24/7",
                label = "Онлайн"
            )
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

// ✅ УЛУЧШЕННАЯ VERSION CARD
@Composable
fun EnhancedVersionCard() {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Radio,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Deep Radio",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Open Beta Test",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "Версия 2.1.0 (Build 210)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Последнее обновление: Июнь 2025",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// ✅ УЛУЧШЕННЫЙ ДИАЛОГ ВЫБОРА ТЕМЫ
@Composable
private fun EnhancedThemeSelectionDialog(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Выберите тему",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Выберите тему, которая подходит для ваших глаз и времени суток:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AppTheme.values().forEach { theme ->
                    EnhancedThemeOption(
                        theme = theme,
                        isSelected = currentTheme == theme,
                        onSelected = { onThemeSelected(theme) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}

@Composable
private fun EnhancedThemeOption(
    theme: AppTheme,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelected
            )

            Spacer(modifier = Modifier.width(12.dp))

            // ✅ ИСПРАВЛЕНО: Заменил Blur на существующие иконки
            Icon(
                imageVector = when (theme) {
                    AppTheme.LIGHT -> Icons.Default.LightMode
                    AppTheme.DARK -> Icons.Default.DarkMode
                    AppTheme.AMOLED -> Icons.Default.Brightness2
                    AppTheme.LIQUID_GLASS -> Icons.Default.Opacity // ✅ Заменил Blur
                },
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.getDisplayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = theme.getDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ✅ УЛУЧШЕННЫЙ ДИАЛОГ РЕЖИМОВ ВОСПРОИЗВЕДЕНИЯ
@Composable
private fun EnhancedPlaybackModeSelectionDialog(
    currentMode: PlaybackMode,
    onModeSelected: (PlaybackMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Режим воспроизведения",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Выберите режим воспроизведения в зависимости от качества вашего интернета:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PlaybackMode.values().forEach { mode ->
                    EnhancedPlaybackModeOption(
                        mode = mode,
                        isSelected = currentMode == mode,
                        onSelected = { onModeSelected(mode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}

@Composable
private fun EnhancedPlaybackModeOption(
    mode: PlaybackMode,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelected,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.secondary
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = when (mode) {
                        PlaybackMode.INSTANT -> Icons.Default.Bolt
                        PlaybackMode.BUFFERED -> Icons.Default.HourglassBottom
                        PlaybackMode.SMART -> Icons.Default.Psychology
                    },
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mode.getDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Подробное описание
            Column(
                modifier = Modifier.padding(start = 56.dp, top = 8.dp)
            ) {
                Text(
                    text = mode.getDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 ${mode.getRecommendation()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
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
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
        modifier = Modifier
            .padding(bottom = 32.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
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