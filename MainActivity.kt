package com.myradio.deepradio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myradio.deepradio.domain.MediaManager
import com.myradio.deepradio.presentation.theme.DeepRadioTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var mediaManager: MediaManager

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var updateManager: UpdateManager

    private val requestBluetoothPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                showPermissionSnackbar("Bluetooth permission denied")
            }
        }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                showPermissionSnackbar("Notification permission denied")
            }
        }

    private val autoRotateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.myradio.deepradio.AUTO_ROTATE_CHANGED") {
                val enabled = intent.getBooleanExtra("enabled", true)
                requestedOrientation = if (enabled) {
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                } else {
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPermissions()
        setupOrientation()
        registerAutoRotateReceiver()

        setContent {
            DeepRadioTheme {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen(
        viewModel: MainViewModel = hiltViewModel()
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        var showAbout by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.checkForUpdates()
            adManager.loadInterstitialAd()
        }

        BackHandler(enabled = drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                NavigationDrawerContent(
                    onItemSelected = { item ->
                        scope.launch { drawerState.close() }
                        handleDrawerNavigation(item, showAbout = { showAbout = true })
                    }
                )
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                RadioMainScreen(
                    onMenuClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open()
                            else drawerState.close()
                        }
                    }
                )

                if (adManager.showAdBlockerDialog.value) {
                    AdBlockerDetectedDialog(adManager = adManager)
                }

                if (updateManager.showUpdateDialog) {
                    UpdateDialog(updateManager = updateManager)
                }
            }
        }

        if (showAbout) {
            ShowAboutApp(
                context = LocalContext.current,
                onDismiss = { showAbout = false }
            )
        }

        // Handle messages - показываем как Toast вместо Snackbar
        LaunchedEffect(uiState.message) {
            uiState.message?.let { message ->
                // Можно добавить Toast или другой способ показа сообщений
                viewModel.clearMessage()
            }
        }
    }

    @Composable
    private fun NavigationDrawerContent(
        onItemSelected: (String) -> Unit
    ) {
        ModalDrawerSheet(
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Radio,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "OPEN BETA TEST",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Menu items
                val menuItems = listOf(
                    Triple("О приложении", Icons.Default.Info, "about"),
                    Triple("Добавить станцию", Icons.Default.Add, "add"),
                    Triple("Настройки", Icons.Default.Settings, "settings"),
                    Triple("Предложения", Icons.Default.Feedback, "suggestions"),
                    Triple("Политика конфиденциальности", Icons.Default.PrivacyTip, "privacy")
                )

                menuItems.forEach { (title, icon, action) ->
                    NavigationDrawerItem(
                        label = { Text(title) },
                        selected = false,
                        onClick = { onItemSelected(action) },
                        icon = { Icon(icon, contentDescription = title) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Contact info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:support@deepradio.site")
                        }
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Email app not found, ignore
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Связаться с нами",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "support@deepradio.site",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    private fun handleDrawerNavigation(action: String, showAbout: () -> Unit) {
        when (action) {
            "about" -> showAbout()
            "add" -> OpenAddStationActivity(this)
            "settings" -> OpenSettingsActivity(this)
            "suggestions" -> OpenSuggestionsActivity(this)
            "privacy" -> OpenPrivacyPolicy(this)
        }
    }

    private fun setupPermissions() {
        // Request Bluetooth permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetoothPermission.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupOrientation() {
        val enabled = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("auto_rotate", true)

        requestedOrientation = if (enabled) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun registerAutoRotateReceiver() {
        val filter = IntentFilter("com.myradio.deepradio.AUTO_ROTATE_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(autoRotateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(autoRotateReceiver, filter)
        }
    }

    private fun showPermissionSnackbar(message: String) {
        // Can be handled by displaying a toast or other notification method
        // Since we don't have snackbar anymore
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(autoRotateReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.let {
            // Pass to media manager if needed
        }
    }
}