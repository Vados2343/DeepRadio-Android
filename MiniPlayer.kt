@file:OptIn(ExperimentalAnimationApi::class)
package com.myradio.deepradio.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myradio.deepradio.RadioStation
import com.myradio.deepradio.domain.MediaManager

// ✅ ПОЛНЫЙ УЛУЧШЕННЫЙ MINI PLAYER
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    station: RadioStation,
    metadata: MediaManager.SongMetadata?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current

    // ✅ СОСТОЯНИЯ ДЛЯ АНИМАЦИЙ
    var isPressed by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    // ✅ АНИМАЦИИ
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "mini_player_scale"
    )

    val elevationAnimation by animateDpAsState(
        targetValue = if (isPlaying) 16.dp else 8.dp,
        animationSpec = tween(300),
        label = "mini_player_elevation"
    )

    // ✅ ЦВЕТОВАЯ АНИМАЦИЯ
    val containerColor by animateColorAsState(
        targetValue = when {
            isPlaying -> MaterialTheme.colorScheme.primaryContainer
            isBuffering -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(400),
        label = "mini_player_color"
    )

    // ✅ ОСНОВНОЙ КОНТЕЙНЕР
    Surface(
        color = containerColor,
        shadowElevation = elevationAnimation,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .scale(scaleAnimation)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onExpand() }
                )
            }
    ) {
        // ✅ ГРАДИЕНТНЫЙ ФОНОВЫЙ ЭФФЕКТ
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ✅ УЛУЧШЕННАЯ ALBUM ART СЕКЦИЯ
                EnhancedAlbumArtSection(
                    station = station,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering
                )

                Spacer(Modifier.width(16.dp))

                // ✅ УЛУЧШЕННАЯ ИНФОРМАЦИОННАЯ СЕКЦИЯ
                EnhancedInfoSection(
                    station = station,
                    metadata = metadata,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))

                // ✅ УЛУЧШЕННАЯ СЕКЦИЯ УПРАВЛЕНИЯ
                EnhancedControlsSection(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious
                )
            }

            // ✅ ИНДИКАТОР ПРОГРЕССА (декоративный)
            if (isPlaying) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ✅ УЛУЧШЕННАЯ ALBUM ART СЕКЦИЯ
@Composable
private fun EnhancedAlbumArtSection(
    station: RadioStation,
    isPlaying: Boolean,
    isBuffering: Boolean
) {
    val rotationAnimation by rememberInfiniteTransition(label = "album_rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isPlaying) {
                        Modifier.graphicsLayer(rotationZ = rotationAnimation)
                    } else Modifier
                ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(station.iconResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // ✅ ОВЕРЛЕЙ ДЛЯ СОСТОЯНИЙ
                when {
                    isBuffering -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    isPlaying -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            MiniWaveAnimation()
                        }
                    }
                }
            }
        }

        // ✅ СТАТУС ИНДИКАТОР
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    when {
                        isBuffering -> MaterialTheme.colorScheme.tertiary
                        isPlaying -> Color.Green
                        else -> MaterialTheme.colorScheme.outline
                    },
                    CircleShape
                )
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
        ) {
            Icon(
                imageVector = when {
                    isBuffering -> Icons.Default.HourglassEmpty
                    isPlaying -> Icons.Default.PlayArrow
                    else -> Icons.Default.Pause
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

// ✅ МИНИ WAVE АНИМАЦИЯ
@Composable
private fun MiniWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_wave")

    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 100)
                ),
                label = "mini_height$index"
            )

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height((12 * animatedHeight).dp)
                    .background(
                        Color.White,
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

// ✅ УЛУЧШЕННАЯ ИНФОРМАЦИОННАЯ СЕКЦИЯ
@Composable
private fun EnhancedInfoSection(
    station: RadioStation,
    metadata: MediaManager.SongMetadata?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // ✅ ОСНОВНОЙ ЗАГОЛОВОК
        Text(
            text = metadata?.title?.takeIf { it.isNotBlank() } ?: station.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // ✅ ПОДЗАГОЛОВОК
        Text(
            text = when {
                !metadata?.artist.isNullOrBlank() -> metadata?.artist ?: ""
                else -> station.categories.joinToString(" • ")
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // ✅ СТАТУС С АНИМАЦИЕЙ
        AnimatedVisibility(
            visible = isBuffering || isPlaying,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            when {
                                isBuffering -> MaterialTheme.colorScheme.tertiary
                                isPlaying -> Color.Green
                                else -> MaterialTheme.colorScheme.outline
                            },
                            CircleShape
                        )
                ) {
                    // ✅ ПУЛЬСИРУЮЩАЯ АНИМАЦИЯ
                    if (isPlaying) {
                        val pulseAnimation by rememberInfiniteTransition(label = "pulse").animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_animation"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(pulseAnimation)
                                .background(Color.Green.copy(alpha = 0.6f), CircleShape)
                        )
                    }
                }

                Text(
                    text = when {
                        isBuffering -> "Загрузка..."
                        isPlaying -> "В эфире"
                        else -> "Пауза"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ✅ УЛУЧШЕННАЯ СЕКЦИЯ УПРАВЛЕНИЯ
@Composable
private fun EnhancedControlsSection(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ✅ КНОПКА ПРЕДЫДУЩАЯ
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPrevious()
            },
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Предыдущая",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        // ✅ ГЛАВНАЯ КНОПКА PLAY/PAUSE
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPlayPause()
            },
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                // ✅ АНИМИРОВАННАЯ ИКОНКА
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        scaleIn() + fadeIn() with scaleOut() + fadeOut()
                    },
                    label = "play_pause_icon"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Пауза" else "Воспроизвести",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // ✅ КНОПКА СЛЕДУЮЩАЯ
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNext()
            },
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Следующая",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ✅ ДОПОЛНИТЕЛЬНЫЙ КОМПОНЕНТ: VoiceResultDialog для голосовых команд
@Composable
fun VoiceResultDialog(
    result: com.myradio.deepradio.presentation.VoiceResult,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ✅ АНИМИРОВАННАЯ ИКОНКА
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = result.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = result.message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Понятно")
                }
            }
        }
    }
}

// ✅ ДОПОЛНИТЕЛЬНЫЙ КОМПОНЕНТ: WaveAnimation для общего использования
@Composable
fun WaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "main_wave")

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 120)
                ),
                label = "main_wave_height$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((24 * animatedHeight).dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}