// VpnDetectedDialog.kt
package com.myradio.deepradio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*      // material3
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VpnDetectedDialog(
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    // Полупрозрачный фон под диалог
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2A))
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .widthIn(min = 280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Иконка
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF80C3FF),
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp)
                )

                Text(
                    "Обнаружено VPN\nили прокси",
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    "Пожалуйста, отключите VPN или прокси для использования приложения",
                    color = Color(0xFFB0B0C0),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Кнопка Настройки
                    GradientButton(
                        text = "НАСТРОЙКИ",
                        modifier = Modifier.weight(1f),
                        onClick = onSettings
                    )
                    // Кнопка Выход
                    GradientButton(
                        text = "ВЫХОД",
                        modifier = Modifier.weight(1f),
                        onClick = onExit
                    )
                }
            }
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontSize = 16.sp)
        }
    }
}
