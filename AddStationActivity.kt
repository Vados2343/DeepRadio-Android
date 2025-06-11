package com.myradio.deepradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myradio.deepradio.presentation.theme.DeepRadioTheme
import kotlinx.coroutines.launch

class AddStationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeepRadioTheme {
                AddStationScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var stationName by remember { mutableStateOf("") }
    var stationUrl by remember { mutableStateOf("") }
    var stationApiUrl by remember { mutableStateOf("") }
    var stationCategory by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Добавить станцию",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Предложите добавить новую радиостанцию в наше приложение. Мы рассмотрим ваше предложение и добавим станцию в следующем обновлении.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            OutlinedTextField(
                value = stationName,
                onValueChange = { stationName = it },
                label = { Text("Название станции *") },
                placeholder = { Text("Например: Radio Record") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage.isNotEmpty() && stationName.isEmpty()
            )

            OutlinedTextField(
                value = stationUrl,
                onValueChange = { stationUrl = it },
                label = { Text("URL потока *") },
                placeholder = { Text("https://example.com/stream.mp3") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage.isNotEmpty() && stationUrl.isEmpty()
            )

            OutlinedTextField(
                value = stationApiUrl,
                onValueChange = { stationApiUrl = it },
                label = { Text("API URL (необязательно)") },
                placeholder = { Text("https://example.com/api/nowplaying") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = stationCategory,
                onValueChange = { stationCategory = it },
                label = { Text("Категория") },
                placeholder = { Text("Например: Rock, Pop, Jazz") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    errorMessage = ""

                    if (stationName.isEmpty() || stationUrl.isEmpty()) {
                        errorMessage = "Название и URL потока обязательны для заполнения"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            // Здесь будет логика отправки предложения
                            // Пока просто показываем успешный диалог
                            kotlinx.coroutines.delay(1500) // Имитация отправки
                            isLoading = false
                            showSuccessDialog = true
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Ошибка при отправке: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("Отправка...")
                    }
                } else {
                    Text("Предложить станцию", fontSize = 16.sp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Советы:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Убедитесь, что поток работает и доступен\n" +
                                "• Укажите правильную категорию для лучшей организации\n" +
                                "• API URL поможет получать информацию о текущих треках",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBack()
            },
            title = { Text("Успешно отправлено!") },
            text = {
                Text("Ваше предложение отправлено разработчикам. Мы рассмотрим его и добавим станцию в следующем обновлении.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}