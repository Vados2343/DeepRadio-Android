package com.myradio.deepradio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myradio.deepradio.RadioStation
import com.myradio.deepradio.domain.MediaManager
import com.myradio.deepradio.presentation.RadioViewModel
import com.myradio.deepradio.presentation.components.ExpandedPlayer
import com.myradio.deepradio.presentation.components.MiniPlayer
import com.myradio.deepradio.presentation.components.VoiceResultDialog
import com.myradio.deepradio.presentation.components.WaveAnimation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RadioMainScreen(
    onMenuClick: () -> Unit,
    viewModel: RadioViewModel = hiltViewModel()
) {
    // Собираем все состояния
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val currentMetadata by viewModel.currentMetadata.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val expandedPlayer by viewModel.expandedPlayer.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredStations by viewModel.filteredStations.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val voiceResult by viewModel.showVoiceResult.collectAsStateWithLifecycle()

    // Локальные состояния UI
    var showSearch by remember { mutableStateOf(false) }
    var showPlaybackModeIndicator by remember { mutableStateOf(false) }

    // Обработка Back кнопки
    BackHandler(enabled = expandedPlayer || showSearch) {
        when {
            expandedPlayer -> viewModel.onExpandedPlayerChange(false)
            showSearch -> {
                showSearch = false
                viewModel.onSearchQueryChange("")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ✅ УЛУЧШЕННЫЙ ЗАГОЛОВОК
            AnimatedVisibility(
                visible = !expandedPlayer,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                EnhancedTopBar(
                    onMenuClick = onMenuClick,
                    onSearchClick = { showSearch = !showSearch },
                    onViewModeToggle = { viewModel.onViewModeToggle() },
                    viewMode = viewMode,
                    showSearch = showSearch,
                    isPlaying = playbackState.isPlaying,
                    currentStation = currentStation
                )
            }

            // ✅ УЛУЧШЕННАЯ ПОИСКОВАЯ СТРОКА
            AnimatedVisibility(
                visible = showSearch && !expandedPlayer,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                EnhancedSearchBar(
                    searchQuery = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClose = {
                        showSearch = false
                        viewModel.onSearchQueryChange("")
                    }
                )
            }

            // ✅ УЛУЧШЕННЫЕ КАТЕГОРИИ
            AnimatedVisibility(
                visible = !expandedPlayer,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                EnhancedCategoryChips(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = viewModel::onCategorySelect,
                    favoritesCount = favorites.size
                )
            }

            // ✅ ИНДИКАТОР РЕЖИМА ВОСПРОИЗВЕДЕНИЯ
            AnimatedVisibility(
                visible = showPlaybackModeIndicator && !expandedPlayer,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                PlaybackModeIndicator(
                    onDismiss = { showPlaybackModeIndicator = false }
                )
            }

            // ✅ УЛУЧШЕННЫЙ КОНТЕНТ СТАНЦИЙ
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = viewMode,
                    transitionSpec = {
                        fadeIn(tween(300)) + scaleIn(tween(300), 0.95f) with
                                fadeOut(tween(150)) + scaleOut(tween(150), 1.05f)
                    },
                    label = "viewModeTransition"
                ) { mode ->
                    when (mode) {
                        MediaManager.ViewMode.LIST -> {
                            EnhancedStationsList(
                                stations = filteredStations,
                                currentStation = currentStation,
                                playbackState = playbackState,
                                isBuffering = isBuffering,
                                favorites = favorites,
                                onStationClick = viewModel::onStationClick,
                                onFavoriteClick = viewModel::onFavoriteClick
                            )
                        }
                        MediaManager.ViewMode.GRID -> {
                            EnhancedStationsGrid(
                                stations = filteredStations,
                                currentStation = currentStation,
                                playbackState = playbackState,
                                isBuffering = isBuffering,
                                favorites = favorites,
                                onStationClick = viewModel::onStationClick,
                                onFavoriteClick = viewModel::onFavoriteClick
                            )
                        }
                    }
                }
            }
        }

        // ✅ ИСПРАВЛЕННЫЙ MINI PLAYER с правильными кнопками
        AnimatedVisibility(
            visible = !expandedPlayer && currentStation != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            currentStation?.let { station ->
                MiniPlayer(
                    station = station,
                    metadata = currentMetadata,
                    isPlaying = playbackState.isPlaying,
                    isBuffering = isBuffering,
                    onPlayPause = viewModel::onPlayPauseClick,
                    onNext = viewModel::onNextClick, // ✅ ИСПРАВЛЕНО
                    onPrevious = viewModel::onPreviousClick, // ✅ ИСПРАВЛЕНО
                    onExpand = { viewModel.onExpandedPlayerChange(true) }
                )
            }
        }

        // ✅ ИСПРАВЛЕННЫЙ EXPANDED PLAYER с правильными кнопками
        AnimatedVisibility(
            visible = expandedPlayer,
            enter = slideInVertically { it } + fadeIn(tween(400)),
            exit = slideOutVertically { it } + fadeOut(tween(300))
        ) {
            currentStation?.let { station ->
                ExpandedPlayer(
                    station = station,
                    metadata = currentMetadata,
                    isPlaying = playbackState.isPlaying,
                    isBuffering = isBuffering,
                    onPlayPause = viewModel::onPlayPauseClick,
                    onNext = viewModel::onNextClick, // ✅ ИСПРАВЛЕНО
                    onPrevious = viewModel::onPreviousClick, // ✅ ИСПРАВЛЕНО
                    onCollapse = { viewModel.onExpandedPlayerChange(false) },
                    onShare = viewModel::onShareClick
                )
            }
        }

        // ✅ VOICE RESULT DIALOG
        voiceResult?.let { result ->
            VoiceResultDialog(
                result = result,
                onDismiss = viewModel::dismissVoiceResult
            )
        }

        // ✅ FLOATING ACTION BUTTON для быстрого доступа
        AnimatedVisibility(
            visible = !expandedPlayer && currentStation == null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showPlaybackModeIndicator = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ✅ УЛУЧШЕННЫЙ TOP BAR
@Composable
fun EnhancedTopBar(
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onViewModeToggle: () -> Unit,
    viewMode: MediaManager.ViewMode,
    showSearch: Boolean,
    isPlaying: Boolean,
    currentStation: RadioStation?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = if (currentStation != null) 8.dp else 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая часть
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Open menu",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box {
                            Icon(
                                Icons.Default.Radio,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )

                            // Индикатор воспроизведения
                            if (isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            Color.Green,
                                            CircleShape
                                        )
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                "Deep Radio",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            if (currentStation != null) {
                                Text(
                                    "♪ ${currentStation.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Правая часть
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onViewModeToggle) {
                        Icon(
                            if (viewMode == MediaManager.ViewMode.GRID)
                                Icons.Default.ViewList
                            else
                                Icons.Default.GridView,
                            contentDescription = "Toggle view",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (showSearch) "Close search" else "Search",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// ✅ УЛУЧШЕННАЯ ПОИСКОВАЯ СТРОКА
@Composable
fun EnhancedSearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "🔍 Поиск радиостанций...",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close search"
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

// ✅ УЛУЧШЕННЫЕ КАТЕГОРИИ
@Composable
fun EnhancedCategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    favoritesCount: Int
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = when (category) {
                "All" -> selectedCategory == null
                "Favorites" -> selectedCategory == "Favorites"
                else -> selectedCategory == category
            }

            FilterChip(
                selected = isSelected,
                onClick = {
                    when (category) {
                        "All" -> onCategorySelect(null)
                        else -> onCategorySelect(category)
                    }
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            category,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (category == "Favorites" && favoritesCount > 0) {
                            Badge {
                                Text(favoritesCount.toString())
                            }
                        }
                    }
                },
                leadingIcon = when (category) {
                    "Favorites" -> {
                        {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    "All" -> {
                        {
                            Icon(
                                Icons.Default.Radio,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    else -> null
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ✅ ИНДИКАТОР РЕЖИМА ВОСПРОИЗВЕДЕНИЯ
@Composable
fun PlaybackModeIndicator(
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onDismiss() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Режим воспроизведения",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Перейдите в Настройки → Воспроизведение для изменения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ✅ УЛУЧШЕННЫЙ СПИСОК СТАНЦИЙ
@Composable
fun EnhancedStationsList(
    stations: List<RadioStation>,
    currentStation: RadioStation?,
    playbackState: MediaManager.PlaybackState,
    isBuffering: Boolean,
    favorites: Set<String>,
    onStationClick: (RadioStation) -> Unit,
    onFavoriteClick: (RadioStation) -> Unit
) {
    if (stations.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = stations,
                key = { it.name }
            ) { station ->
                EnhancedStationListItem(
                    station = station,
                    isCurrentStation = station == currentStation,
                    isPlaying = station == currentStation && playbackState.isPlaying,
                    isBuffering = station == currentStation && isBuffering,
                    isFavorite = favorites.contains(station.name),
                    onStationClick = { onStationClick(station) },
                    onFavoriteClick = { onFavoriteClick(station) }
                )
            }
        }
    }
}

// ✅ УЛУЧШЕННАЯ СЕТКА СТАНЦИЙ
@Composable
fun EnhancedStationsGrid(
    stations: List<RadioStation>,
    currentStation: RadioStation?,
    playbackState: MediaManager.PlaybackState,
    isBuffering: Boolean,
    favorites: Set<String>,
    onStationClick: (RadioStation) -> Unit,
    onFavoriteClick: (RadioStation) -> Unit
) {
    val configuration = LocalConfiguration.current
    val columns = if (configuration.screenWidthDp > 600) 3 else 2

    if (stations.isEmpty()) {
        EmptyState()
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 100.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = stations,
                key = { it.name }
            ) { station ->
                EnhancedStationGridItem(
                    station = station,
                    isCurrentStation = station == currentStation,
                    isPlaying = station == currentStation && playbackState.isPlaying,
                    isBuffering = station == currentStation && isBuffering,
                    isFavorite = favorites.contains(station.name),
                    onStationClick = { onStationClick(station) },
                    onFavoriteClick = { onFavoriteClick(station) }
                )
            }
        }
    }
}

// ✅ EMPTY STATE
@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                "Ничего не найдено",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                "Попробуйте изменить поисковый запрос\nили выберите другую категорию",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ✅ УЛУЧШЕННЫЙ ЭЛЕМЕНТ СПИСКА
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedStationListItem(
    station: RadioStation,
    isCurrentStation: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isFavorite: Boolean,
    onStationClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isCurrentStation) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "scale"
    )

    val animatedColor by animateColorAsState(
        targetValue = when {
            isCurrentStation && isPlaying -> MaterialTheme.colorScheme.primaryContainer
            isCurrentStation -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "color"
    )

    Card(
        onClick = onStationClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale),
        colors = CardDefaults.cardColors(containerColor = animatedColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentStation) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = painterResource(station.iconResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                when {
                    isBuffering -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    isCurrentStation && isPlaying -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            WaveAnimation()
                        }
                    }
                }

                // Индикатор избранного
                if (isFavorite) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.categories.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Статус воспроизведения
                if (isCurrentStation) {
                    Text(
                        text = when {
                            isBuffering -> "⏳ Загрузка..."
                            isPlaying -> "▶️ Играет"
                            else -> "⏸️ Пауза"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ✅ УЛУЧШЕННЫЙ ЭЛЕМЕНТ СЕТКИ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedStationGridItem(
    station: RadioStation,
    isCurrentStation: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isFavorite: Boolean,
    onStationClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isCurrentStation) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "scale"
    )

    Card(
        onClick = onStationClick,
        modifier = Modifier
            .aspectRatio(1f)
            .scale(animatedScale),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentStation) 8.dp else 2.dp
        ),
        border = if (isCurrentStation && isPlaying) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(station.iconResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f
                        )
                    )
            )

            when {
                isBuffering -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
                isCurrentStation && isPlaying -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        WaveAnimation()
                    }
                }
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFE91E63) else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.categories.firstOrNull() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // Статус в сетке
                if (isCurrentStation) {
                    Text(
                        text = when {
                            isBuffering -> "⏳"
                            isPlaying -> "▶️"
                            else -> "⏸️"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}