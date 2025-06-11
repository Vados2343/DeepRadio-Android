package com.myradio.deepradio.domain

import android.util.Log
import com.myradio.deepradio.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataFetcher @Inject constructor() {

    companion object {
        private const val TAG = "MetadataFetcher"
        private const val CONNECTION_TIMEOUT = 1200
        private const val READ_TIMEOUT = 1200

        // Ключи для поиска в JSON
        private val SONG_KEYS = listOf("title", "song", "name", "track_title", "iName", "song_name", "name_translit")
        private val ARTIST_KEYS = listOf("artist", "singer", "iArtist", "track_artist", "artist_name")
        private val ALBUM_KEYS = listOf("album", "album_name", "disc")
        private val GENRE_KEYS = listOf("genre", "style", "category")
    }

    suspend fun fetchMetadata(station: RadioStation): MediaManager.SongMetadata {
        return withContext(Dispatchers.IO) {
            try {
                // Если нет URL API, возвращаем пустые метаданные
                if (station.apiUrl.isBlank()) {
                    return@withContext MediaManager.SongMetadata()
                }

                // Получаем данные из API
                val connection = URL(station.apiUrl).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECTION_TIMEOUT
                    readTimeout = READ_TIMEOUT
                    setRequestProperty("User-Agent", "DeepRadio/2.0")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bytes = inputStream.readBytes()
                    val responseString = detectAndDecode(bytes)

                    // Парсим JSON
                    val json: Any = try {
                        JSONObject(responseString)
                    } catch (e: Exception) {
                        try {
                            JSONArray(responseString)
                        } catch (e2: Exception) {
                            Log.e(TAG, "Invalid JSON format from ${station.name}")
                            return@withContext MediaManager.SongMetadata()
                        }
                    }

                    // Извлекаем информацию
                    val extractedInfo = extractSongInfo(json)

                    return@withContext MediaManager.SongMetadata(
                        title = extractedInfo["song"] ?: "",
                        artist = extractedInfo["artist"] ?: "",
                        album = extractedInfo["album"] ?: "",
                        genre = extractedInfo["genre"] ?: ""
                    )
                }

                connection.disconnect()
                MediaManager.SongMetadata()

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching metadata for ${station.name}: ${e.message}")
                MediaManager.SongMetadata()
            }
        }
    }

    private fun extractSongInfo(jsonElement: Any): Map<String, String?> {
        var foundSongName: String? = null
        var foundArtistName: String? = null
        var foundAlbumName: String? = null
        var foundGenreName: String? = null

        when (jsonElement) {
            is JSONObject -> {
                val keysIterator = jsonElement.keys()
                while (keysIterator.hasNext()) {
                    val key = keysIterator.next()
                    val value = jsonElement.opt(key) ?: continue

                    // Проверяем прямые совпадения ключей
                    when {
                        key in SONG_KEYS && foundSongName == null && value is String -> {
                            foundSongName = value.takeIf { it.isNotBlank() }
                        }
                        key in ARTIST_KEYS && foundArtistName == null && value is String -> {
                            foundArtistName = value.takeIf { it.isNotBlank() }
                        }
                        key in ALBUM_KEYS && foundAlbumName == null && value is String -> {
                            foundAlbumName = value.takeIf { it.isNotBlank() }
                        }
                        key in GENRE_KEYS && foundGenreName == null && value is String -> {
                            foundGenreName = value.takeIf { it.isNotBlank() }
                        }
                    }

                    // Рекурсивно ищем во вложенных объектах
                    if (value is JSONObject || value is JSONArray) {
                        val result = extractSongInfo(value)
                        if (foundSongName == null) foundSongName = result["song"]
                        if (foundArtistName == null) foundArtistName = result["artist"]
                        if (foundAlbumName == null) foundAlbumName = result["album"]
                        if (foundGenreName == null) foundGenreName = result["genre"]
                    }
                }
            }
            is JSONArray -> {
                // Для массивов проверяем каждый элемент
                for (i in 0 until jsonElement.length()) {
                    val value = jsonElement.opt(i) ?: continue
                    val result = extractSongInfo(value)
                    if (foundSongName == null) foundSongName = result["song"]
                    if (foundArtistName == null) foundArtistName = result["artist"]
                    if (foundAlbumName == null) foundAlbumName = result["album"]
                    if (foundGenreName == null) foundGenreName = result["genre"]

                    // Если нашли основную информацию, можем прервать поиск
                    if (foundSongName != null && foundArtistName != null) {
                        break
                    }
                }
            }
        }

        return mapOf(
            "song" to foundSongName,
            "artist" to foundArtistName,
            "album" to foundAlbumName,
            "genre" to foundGenreName
        )
    }

    private fun detectAndDecode(bytes: ByteArray): String {
        val encodingsToTry = listOf("UTF-8", "Windows-1251", "ISO-8859-1")

        for (encoding in encodingsToTry) {
            try {
                val decodedString = String(bytes, charset(encoding))
                if (isValidString(decodedString)) {
                    return decodedString
                }
            } catch (e: Exception) {
                // Пробуем следующую кодировку
            }
        }

        // Если ничего не подошло, используем UTF-8
        return String(bytes, charset("UTF-8"))
    }

    private fun isValidString(text: String): Boolean {
        // Проверяем, что строка не содержит символов замены
        return !text.contains("�") && !text.contains("\\u")
    }
}