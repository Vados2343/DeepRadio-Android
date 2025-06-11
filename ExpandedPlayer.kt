package com.myradio.deepradio.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myradio.deepradio.RadioStation
import com.myradio.deepradio.domain.MediaManager
import kotlin.math.abs

// ✅ ПОЛНЫЙ УЛУЧШЕННЫЙ EXPANDED PLAYER
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayer(
    station: RadioStation,
    metadata: MediaManager.SongMetadata?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCollapse: () -> Unit,
    onShare: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current

    // ✅ СОСТОЯНИЯ ДЛЯ СВАЙПОВ
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showSwipeHint by remember { mutableStateOf(true) }

    // ✅ СОСТОЯНИЯ ДЛЯ UI
    var showLyrics by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }

    // Анимация для возврата в исходное положение
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipe_animation"
    )

    // Пороги для срабатывания свайпов
    val swipeThreshold = 120f
    val dragThreshold = 40f

    // ✅ СКРЫТИЕ ПОДСКАЗКИ ЧЕРЕЗ 5 СЕКУНД
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        showSwipeHint = false
    }

    // ✅ ОБРАБОТКА BACK BUTTON
    BackHandler(onBack = onCollapse)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        showSwipeHint = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        isDragging = false
                        when {
                            offsetX > swipeThreshold -> {
                                // Свайп вправо - предыдущая станция
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPrevious()
                            }
                            offsetX < -swipeThreshold -> {
                                // Свайп влево - следующая станция
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNext()
                            }
                        }
                        offsetX = 0f
                    }
                ) { _, dragAmount ->
                    offsetX += dragAmount

                    // Вибрация при достижении порога
                    if (abs(offsetX) > dragThreshold && abs(offsetX - dragAmount) <= dragThreshold) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
    ) {
        // ✅ ФОНОВОЕ ИЗОБРАЖЕНИЕ С БЛЮРОМ
        Image(
            painter = painterResource(station.iconResId),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(50.dp)
                .alpha(0.2f)
                .scale(1.2f),
            contentScale = ContentScale.Crop
        )

        // ✅ ОСНОВНОЙ КОНТЕНТ С АНИМАЦИЕЙ СВАЙПА
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    translationX = animatedOffsetX
                    alpha = 1f - (abs(animatedOffsetX) / (swipeThreshold * 4))
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ УЛУЧШЕННЫЙ TOP BAR
            EnhancedTopBar(
                onCollapse = onCollapse,
                onShare = onShare,
                onShowDetails = { showDetails = true },
                currentSong = metadata?.title ?: station.name
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ✅ ALBUM ART С ИНДИКАТОРАМИ
            EnhancedAlbumArt(
                station = station,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                offsetX = offsetX,
                dragThreshold = dragThreshold,
                swipeThreshold = swipeThreshold,
                showSwipeHint = showSwipeHint
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ✅ ИНФОРМАЦИЯ О ТРЕКЕ
            EnhancedTrackInfo(
                station = station,
                metadata = metadata,
                isBuffering = isBuffering,
                onShowLyrics = { showLyrics = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            // ✅ ДОПОЛНИТЕЛЬНЫЕ ДЕЙСТВИЯ
            EnhancedActionRow(
                onShowEqualizer = { showEqualizer = true },
                onShowLyrics = { showLyrics = true },
                onShare = onShare
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ ОСНОВНЫЕ ЭЛЕМЕНТЫ УПРАВЛЕНИЯ
            EnhancedControlsSection(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ✅ ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ
            if (!metadata?.genre.isNullOrBlank() || !metadata?.album.isNullOrBlank()) {
                EnhancedMetadataRow(
                    station = station,
                    metadata = metadata
                )
            }
        }

        // ✅ ИНДИКАТОРЫ НАПРАВЛЕНИЯ СВАЙПА
        SwipeIndicators(
            offsetX = offsetX,
            dragThreshold = dragThreshold,
            swipeThreshold = swipeThreshold
        )

        // ✅ ПОДСКАЗКА О СВАЙПАХ
        AnimatedVisibility(
            visible = showSwipeHint,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SwipeHintCard()
        }
    }

    // ✅ ДИАЛОГИ
    if (showDetails) {
        StationDetailsDialog(
            station = station,
            metadata = metadata,
            onDismiss = { showDetails = false }
        )
    }

    if (showLyrics) {
        LyricsDialog(
            trackTitle = metadata?.title ?: station.name,
            artist = metadata?.artist ?: "Unknown Artist",
            onDismiss = { showLyrics = false }
        )
    }

    if (showEqualizer) {
        EqualizerDialog(
            onDismiss = { showEqualizer = false }
        )
    }
}

// ✅ УЛУЧШЕННЫЙ TOP BAR
@Composable
private fun EnhancedTopBar(
    onCollapse: () -> Unit,
    onShare: () -> Unit,
    onShowDetails: () -> Unit,
    currentSong: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onCollapse,
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Свернуть",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Сейчас играет",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = currentSong,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onShowDetails,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Подробности",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Поделиться",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ✅ УЛУЧШЕННЫЙ ALBUM ART
@Composable
private fun EnhancedAlbumArt(
    station: RadioStation,
    isPlaying: Boolean,
    isBuffering: Boolean,
    offsetX: Float,
    dragThreshold: Float,
    swipeThreshold: Float,
    showSwipeHint: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (abs(offsetX) > dragThreshold) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "album_scale"
    )

    Box(
        modifier = Modifier
            .size(300.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(station.iconResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // ✅ ОВЕРЛЕЙ ДЛЯ ВОСПРОИЗВЕДЕНИЯ/БУФЕРИЗАЦИИ
                when {
                    isBuffering -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    "Загрузка...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    isPlaying -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            EnhancedWaveAnimation()
                        }
                    }
                }

                // ✅ ИНДИКАТОР НАПРАВЛЕНИЯ СВАЙПА НА АЛЬБОМЕ
                if (abs(offsetX) > dragThreshold) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (offsetX > 0) Color.Green.copy(alpha = 0.3f)
                                else Color.Blue.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (offsetX > 0) Icons.Default.SkipPrevious else Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (offsetX > 0) "Предыдущая" else "Следующая",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ✅ АНИМИРОВАННАЯ ГРАНИЦА ПРИ СВАЙПЕ
        if (abs(offsetX) > swipeThreshold * 0.7f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 3.dp,
                        color = if (offsetX > 0) Color.Green else Color.Blue,
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }
    }
}

// ✅ УЛУЧШЕННАЯ WAVE АНИМАЦИЯ
@Composable
fun EnhancedWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "enhanced_wave")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 100)
                ),
                label = "wave_height$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((30 * animatedHeight).dp)
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

// ✅ УЛУЧШЕННАЯ ИНФОРМАЦИЯ О ТРЕКЕ
@Composable
private fun EnhancedTrackInfo(
    station: RadioStation,
    metadata: MediaManager.SongMetadata?,
    isBuffering: Boolean,
    onShowLyrics: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Название трека/станции
        Text(
            text = metadata?.title?.takeIf { it.isNotBlank() } ?: station.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Исполнитель/категории
        Text(
            text = metadata?.artist?.takeIf { it.isNotBlank() }
                ?: station.categories.joinToString(" • "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Статус
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isBuffering -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Text(
                text = when {
                    isBuffering -> "⏳ Загрузка..."
                    metadata?.title?.isNotBlank() == true -> "🎵 Прямой эфир"
                    else -> "📻 ${station.name}"
                },
                color = when {
                    isBuffering -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Кнопка текста песни (если есть)
        if (!metadata?.title.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onShowLyrics,
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Текст песни",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ✅ ДОПОЛНИТЕЛЬНЫЕ ДЕЙСТВИЯ
@Composable
private fun EnhancedActionRow(
    onShowEqualizer: () -> Unit,
    onShowLyrics: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            icon = Icons.Default.Equalizer,
            text = "Эквалайзер",
            onClick = onShowEqualizer
        )
        ActionButton(
            icon = Icons.Default.MusicNote,
            text = "Текст",
            onClick = onShowLyrics
        )
        ActionButton(
            icon = Icons.Default.Share,
            text = "Поделиться",
            onClick = onShare
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ✅ УЛУЧШЕННЫЕ ЭЛЕМЕНТЫ УПРАВЛЕНИЯ
@Composable
private fun EnhancedControlsSection(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous
        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Предыдущая",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp)
            )
        }

        // Play/Pause
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(88.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        // Next
        IconButton(
            onClick = onNext,
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Следующая",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

// ✅ ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ О МЕТАДАННЫХ
@Composable
private fun EnhancedMetadataRow(
    station: RadioStation,
    metadata: MediaManager.SongMetadata?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MetadataItem(
            icon = Icons.Default.Radio,
            label = "Станция",
            value = station.name
        )

        if (!metadata?.genre.isNullOrBlank()) {
            MetadataItem(
                icon = Icons.Default.MusicNote,
                label = "Жанр",
                value = metadata?.genre ?: ""
            )
        }

        if (!metadata?.album.isNullOrBlank()) {
            MetadataItem(
                icon = Icons.Default.Album,
                label = "Альбом",
                value = metadata?.album ?: ""
            )
        }
    }
}

@Composable
private fun MetadataItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ✅ ИНДИКАТОРЫ НАПРАВЛЕНИЯ СВАЙПА
@Composable
private fun SwipeIndicators(
    offsetX: Float,
    dragThreshold: Float,
    swipeThreshold: Float
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (abs(offsetX) > dragThreshold) {
            if (offsetX > 0) {
                // Левый индикатор (предыдущая)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(24.dp)
                        .size(64.dp)
                        .background(
                            Color.Green.copy(alpha = 0.9f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Предыдущая",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        if (offsetX > swipeThreshold * 0.8f) {
                            Text(
                                "Отпустите",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Правый индикатор (следующая)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(24.dp)
                        .size(64.dp)
                        .background(
                            Color.Blue.copy(alpha = 0.9f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Следующая",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        if (abs(offsetX) > swipeThreshold * 0.8f) {
                            Text(
                                "Отпустите",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ✅ ПОДСКАЗКА О СВАЙПАХ
@Composable
private fun SwipeHintCard() {
    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.SwipeLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Смахните влево/вправо для переключения станций",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Default.SwipeRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ✅ ДИАЛОГ ПОДРОБНОСТЕЙ СТАНЦИИ
@Composable
private fun StationDetailsDialog(
    station: RadioStation,
    metadata: MediaManager.SongMetadata?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Подробности о станции",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                DetailRow("Название", station.name)
                DetailRow("Категории", station.categories.joinToString(", "))
                if (!metadata?.title.isNullOrBlank()) {
                    DetailRow("Текущий трек", metadata?.title ?: "")
                }
                if (!metadata?.artist.isNullOrBlank()) {
                    DetailRow("Исполнитель", metadata?.artist ?: "")
                }
                if (!metadata?.album.isNullOrBlank()) {
                    DetailRow("Альбом", metadata?.album ?: "")
                }

                HorizontalDivider()

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
    }
}

// ✅ ДИАЛОГ ТЕКСТА ПЕСНИ
@Composable
private fun LyricsDialog(
    trackTitle: String,
    artist: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Текст песни",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$artist - $trackTitle",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Текст песни недоступен",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Для радиостанций текст песен не всегда доступен",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

// ✅ ДИАЛОГ ЭКВАЛАЙЗЕРА
@Composable
private fun EqualizerDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Эквалайзер",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                // Пресеты
                Text(
                    text = "Пресеты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                val presets = listOf("Обычный", "Поп", "Рок", "Джаз", "Классика")
                presets.forEach { preset ->
                    OutlinedButton(
                        onClick = { /* TODO: Применить пресет */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(preset)
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Настройки эквалайзера доступны в системных настройках звука",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}