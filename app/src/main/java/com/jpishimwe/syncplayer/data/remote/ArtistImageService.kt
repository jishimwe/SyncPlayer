package com.jpishimwe.syncplayer.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ArtistImageService"

@Singleton
open class ArtistImageService
    @Inject
    constructor() {
        open suspend fun fetchArtistImageUrl(artistName: String): String? =
            withContext(Dispatchers.IO) {
                try {
                    val encoded = URLEncoder.encode(artistName, "UTF-8")
                    val requestUrl = "https://api.deezer.com/search/artist?q=$encoded&limit=1"
                    Log.e(TAG, "Fetching image for '$artistName': $requestUrl")
                    val url = URL(requestUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000
                    connection.requestMethod = "GET"

                    try {
                        val responseCode = connection.responseCode
                        if (responseCode != 200) {
                            Log.w(TAG, "HTTP $responseCode for '$artistName'")
                            return@withContext null
                        }
                        val body = connection.inputStream.bufferedReader().readText()
                        val json = JSONObject(body)
                        val data = json.optJSONArray("data")
                        if (data == null || data.length() == 0) {
                            Log.e(TAG, "No results for '$artistName'")
                            return@withContext null
                        }
                        val artist = data.getJSONObject(0)
                        val pictureXl = artist.optString("picture_xl")
                        val result = pictureXl.ifEmpty { null }
                        Log.e(TAG, "Got image for '$artistName': $result")
                        result
                    } finally {
                        connection.disconnect()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch image for '$artistName': ${e::class.simpleName}: ${e.message}", e)
                    null
                }
            }
    }
