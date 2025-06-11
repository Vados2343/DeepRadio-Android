package com.myradio.deepradio.domain

import android.content.Context
import com.myradio.deepradio.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepository @Inject constructor(
    private val context: Context
) {
    // «сырая» StateFlow без учёта избранного
    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    init {
        loadStations()
        loadFavorites()
    }

    private fun loadStations() {
        _stations.value = RadioStation.createSampleStations()
        updateCategories()
    }

    private fun loadFavorites() {
        val prefs = context.getSharedPreferences("radio_prefs", Context.MODE_PRIVATE)
        _favorites.value = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(station: RadioStation) {
        val current = _favorites.value.toMutableSet()
        if (!current.add(station.name)) {
            current.remove(station.name)
        }
        _favorites.value = current

        context.getSharedPreferences("radio_prefs", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("favorites", current)
            .apply()

        // Обновляем поток станций с новым флагом isFavorite
        _stations.value = _stations.value.map { item ->
            item.copy(isFavorite = current.contains(item.name))
        }
    }

    /**
     * Основной метод для получения всех станций со статусом избранного.
     */
    fun getAllStations(): StateFlow<List<RadioStation>> = combine(
        stations, favorites
    ) { list, favs ->
        list.map { it.copy(isFavorite = favs.contains(it.name)) }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Main),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _stations.value
    )

    /**
     * Фильтрация по категории и поисковому запросу.
     */
    fun getFilteredStations(category: String?, searchQuery: String): Flow<List<RadioStation>> {
        return combine(getAllStations(), favorites) { list, favs ->
            list.filter { station ->
                val matchesCategory = category == null
                        || category == "All"
                        || (category == "Favorites" && favs.contains(station.name))
                        || station.categories.contains(category)
                val matchesSearch = searchQuery.isEmpty()
                        || station.name.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
            }.sortedByDescending { it.isFavorite }
        }
    }

    /**
     * Добавляет новую кастомную станцию в список.
     */
    fun addCustomStation(station: RadioStation) {
        _stations.value = _stations.value + station
        updateCategories()
    }

    /**
     * Поиск станции по URL потока.
     */
    fun getStationByUrl(url: String): RadioStation? =
        _stations.value.find { it.streamUrl == url }

    private fun updateCategories() {
        val all = _stations.value
            .flatMap { it.categories }
            .distinct()
            .sorted()
        _categories.value = listOf("All") + all + listOf("Favorites")
    }
}
