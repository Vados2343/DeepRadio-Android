package com.myradio.deepradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myradio.deepradio.presentation.theme.DeepRadioTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SuggestionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeepRadioTheme {
                SuggestionsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    onBack: () -> Unit,
    viewModel: SuggestionsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state by viewModel.suggestionsState.collectAsStateWithLifecycle()

    var stationName by remember { mutableStateOf("") }
    var stationStream by remember { mutableStateOf("") }
    var stationIcon by remember { mutableStateOf("") }
    var songStream by remember { mutableStateOf("") }
    var suggestionText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (state) {
            is SuggestionsState.Success -> {
                // Показываем сообщение об успехе и закрываем активность
                scope.launch {
                    kotlinx.coroutines.delay(1000)
                    onBack()
                }
            }
            is SuggestionsState.Error -> {
                // Ошибка уже обрабатывается в UI
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Предложения",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Menu, contentDescription = "Меню")
                }
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Помощь")
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
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Предложите добавление желаемой станции в наше приложение. Мы постараемся добавить её в следующем обновлении.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Station Information Section
            Text(
                text = "Информация о станции",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = stationName,
                onValueChange = { stationName = it },
                label = { Text("Название станции") },
                placeholder = { Text("Например: Radio Record") },
                leadingIcon = { Icon(Icons.Default.Radio, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Название станции" },
                singleLine = true
            )

            OutlinedTextField(
                value = stationStream,
                onValueChange = { stationStream = it },
                label = { Text("URL потока") },
                placeholder = { Text("https://example.com/stream.mp3") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "URL потока" },
                singleLine = true
            )

            OutlinedTextField(
                value = stationIcon,
                onValueChange = { stationIcon = it },
                label = { Text("URL иконки (необязательно)") },
                placeholder = { Text("https://example.com/icon.png") },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "URL иконки" },
                singleLine = true
            )

            OutlinedTextField(
                value = songStream,
                onValueChange = { songStream = it },
                label = { Text("API для метаданных (необязательно)") },
                placeholder = { Text("https://example.com/api/nowplaying") },
                leadingIcon = { Icon(Icons.Default.Api, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "API для метаданных" },
                singleLine = true
            )

            // Suggestions Section
            Text(
                text = "Ваши предложения",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                BasicTextField(
                    value = suggestionText,
                    onValueChange = { suggestionText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(16.dp)
                        .semantics { contentDescription = "Ваши предложения" },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    decorationBox = { innerTextField ->
                        if (suggestionText.isEmpty()) {
                            Text(
                                text = "Здесь вы можете написать ваши предложения по улучшению приложения, запросы на добавление функций или сообщить об ошибках.",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Error state
            if (state is SuggestionsState.Error) {
                val errorMessage = (state as SuggestionsState.Error).message
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Кнопка отправки
            Button(
                onClick = {
                    if (validateFields(stationName, stationStream, suggestionText)) {
                        viewModel.sendSuggestion(
                            stationName = stationName.ifEmpty { null },
                            stationStream = stationStream.ifEmpty { null },
                            stationIcon = stationIcon.ifEmpty { null },
                            songStream = songStream.ifEmpty { null },
                            suggestion = suggestionText.ifEmpty { null }  // <-- исправлено имя параметра
                        )
                    } else {
                        scope.launch {
                            viewModel.showSnackbar("Заполните либо данные станции (название + поток), либо предложения.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is SuggestionsState.Loading
            ) {
                if (state is SuggestionsState.Loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Отправка...")
                    }
                } else {
                    Text("Отправить предложение", fontSize = 16.sp)
                }
            }

            // Карточка с подсказками
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lightbulb,              // <-- заменили несуществующий Tips
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Советы:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Убедитесь, что поток работает и доступен\n" +
                                "• Укажите правильное название станции\n" +
                                "• API URL поможет получать информацию о треках\n" +
                                "• Опишите подробно ваши предложения",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Menu Dialog
    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = { showDialog = false }
        ) {
            NavigationMenu(
                onAbout = {
                    showDialog = false
                    // Логика показа "О приложении"
                },
                onAdd = {
                    showDialog = false
                    OpenAddStationActivity(context)
                },
                onSettings = {
                    showDialog = false
                    OpenSettingsActivity(context)
                },
                onPrivacy = {
                    showDialog = false
                    OpenPrivacyPolicy(context)
                }
            )
        }
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    "Помощь",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Вы можете предложить добавление желаемой станции в наше приложение или написать свои предложения по улучшению. Мы рассмотрим все предложения и постараемся реализовать лучшие из них в следующих обновлениях.")
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Понятно")
                }
            },
            icon = {
                Icon(
                    Icons.Default.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}

@Composable
private fun NavigationMenu(
    onAbout: () -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
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
            Triple("Настройки", Icons.Default.Settings, onSettings),
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

fun validateFields(stationName: String, stationStream: String, suggestionText: String): Boolean {
    return (stationName.isNotEmpty() && stationStream.isNotEmpty()) || suggestionText.isNotEmpty()
}