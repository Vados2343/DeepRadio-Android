@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.myradio.deepradio.presentation.screens
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myradio.deepradio.RadioStation
import com.myradio.deepradio.domain.MediaManager
import com.myradio.deepradio.presentation.RadioViewModel
import com.myradio.deepradio.presentation.components.ExpandedPlayer
import com.myradio.deepradio.presentation.components.MiniPlayer
import com.myradio.deepradio.presentation.components.VoiceResultDialog
import com.myradio.deepradio.presentation.components.WaveAnimation
import com.myradio.deepradio.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RadioMainScreen(
    viewModel: RadioViewModel = hiltViewModel()
) {
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
    val voiceResult by viewModel.showVoiceResult.collectAsStateWithLifecycle()

    var showSearch by remember { mutableStateOf(false) }

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
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = !expandedPlayer,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    ModernTopBar(
                        showSearch = showSearch,
                        searchQuery = searchQuery,
                        viewMode = viewMode,
                        onSearchToggle = { showSearch = !showSearch },
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        onViewModeToggle = viewModel::onViewModeToggle
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !expandedPlayer && currentStation != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    currentStation?.let { station ->
                        MiniPlayer(
                            station = station,
                            metadata = currentMetadata,
                            isPlaying = playbackState.isPlaying,
                            isBuffering = isBuffering,
                            onPlayPause = viewModel::onPlayPauseClick,
                            onNext = viewModel::onNextClick,
                            onPrevious = viewModel::onPreviousClick,
                            onExpand = { viewModel.onExpandedPlayerChange(true) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Categories
                AnimatedVisibility(
                    visible = !expandedPlayer,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    CategoryChips(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelect = viewModel::onCategorySelect
                    )
                }

                // Stations
                AnimatedContent(
                    targetState = viewMode,
                    transitionSpec = {
                        fadeIn() with fadeOut()
                    },
                    label = "viewModeTransition"
                ) { mode ->
                    when (mode) {
                        MediaManager.ViewMode.LIST -> {
                            StationsList(
                                stations = filteredStations,
                                currentStation = currentStation,
                                playbackState = playbackState,
                                onStationClick = viewModel::onStationClick,
                                onFavoriteClick = viewModel::onFavoriteClick
                            )
                        }
                        MediaManager.ViewMode.GRID -> {
                            StationsGrid(
                                stations = filteredStations,
                                currentStation = currentStation,
                                playbackState = playbackState,
                                onStationClick = viewModel::onStationClick,
                                onFavoriteClick = viewModel::onFavoriteClick
                            )
                        }
                    }
                }
            }
        }

        // Expanded Player
        AnimatedVisibility(
            visible = expandedPlayer,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            currentStation?.let { station ->
                ExpandedPlayer(
                    station = station,
                    metadata = currentMetadata,
                    isPlaying = playbackState.isPlaying,
                    isBuffering = isBuffering,
                    onPlayPause = viewModel::onPlayPauseClick,
                    onNext = viewModel::onNextClick,
                    onPrevious = viewModel::onPreviousClick,
                    onCollapse = { viewModel.onExpandedPlayerChange(false) },
                    onShare = viewModel::onShareClick
                )
            }
        }

        // Voice Result Dialog
        voiceResult?.let { result ->
            VoiceResultDialog(
                result = result,
                onDismiss = viewModel::dismissVoiceResult
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    showSearch: Boolean,
    searchQuery: String,
    viewMode: MediaManager.ViewMode,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onViewModeToggle: () -> Unit
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (showSearch) 0.dp else 8.dp,
        animationSpec = tween(300),
        label = "elevation"
    )

    Surface(
        shadowElevation = animatedElevation,
        color = MaterialTheme.colorScheme.primary
    ) {
        TopAppBar(
            title = {
                AnimatedContent(
                    targetState = showSearch,
                    transitionSpec = {
                        fadeIn() + slideInHorizontally() with
                                fadeOut() + slideOutHorizontally()
                    },
                    label = "searchTransition"
                ) { searching ->
                    if (searching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text("Search stations...", style = MaterialTheme.typography.bodyLarge)
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "Deep Radio",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { /* Open drawer */ },
                    modifier = Modifier.graphicsLayer {
                        rotationZ = if (showSearch) 180f else 0f
                    }
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            actions = {
                AnimatedVisibility(
                    visible = !showSearch,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Row {
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
                        IconButton(onClick = onSearchToggle) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = showSearch,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(onClick = {
                        onSearchToggle()
                        onSearchQueryChange("")
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close search",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
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
}

@Composable
fun CategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit
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
                    Text(
                        category,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = if (category == "Favorites") {
                    {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun StationsList(
    stations: List<RadioStation>,
    currentStation: RadioStation?,
    playbackState: MediaManager.PlaybackState,
    onStationClick: (RadioStation) -> Unit,
    onFavoriteClick: (RadioStation) -> Unit
) {
    if (stations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Ничего не найдено",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = stations,
                key = { it.name }
            ) { station ->
                StationListItem(
                    station = station,
                    isCurrentStation = station == currentStation,
                    isPlaying = station == currentStation && playbackState.isPlaying,
                    onStationClick = { onStationClick(station) },
                    onFavoriteClick = { onFavoriteClick(station) }
                )
            }
        }
    }
}

@Composable
fun StationsGrid(
    stations: List<RadioStation>,
    currentStation: RadioStation?,
    playbackState: MediaManager.PlaybackState,
    onStationClick: (RadioStation) -> Unit,
    onFavoriteClick: (RadioStation) -> Unit
) {
    if (stations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Ничего не найдено",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = stations,
                key = { it.name }
            ) { station ->
                StationGridItem(
                    station = station,
                    isCurrentStation = station == currentStation,
                    isPlaying = station == currentStation && playbackState.isPlaying,
                    onStationClick = { onStationClick(station) },
                    onFavoriteClick = { onFavoriteClick(station) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationListItem(
    station: RadioStation,
    isCurrentStation: Boolean,
    isPlaying: Boolean,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = painterResource(station.iconResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                if (isCurrentStation && isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        WaveAnimation()
                    }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (station.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (station.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationGridItem(
    station: RadioStation,
    isCurrentStation: Boolean,
    isPlaying: Boolean,
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

            // Gradient overlay
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

            // Playing indicator
            if (isCurrentStation && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    WaveAnimation()
                }
            }

            // Favorite button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.3f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (station.isFavorite) Color(0xFFE91E63) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Station info
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
            }
        }
    }
}