package io.github.ar3ac.c4rds.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object CloudBrandManager {

    private const val CLOUD_BRANDS_URL = "https://ar3ac.github.io/c4rds-share/brands.json"
    
    data class CloudBrand(
        val name: String,
        val keywords: List<String>,
        val iconUrl: String,
        val localFilePath: String
    )

    var cloudBrands = listOf<CloudBrand>()
        private set

    suspend fun sync(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(CLOUD_BRANDS_URL).openConnection() as HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                if (connection.responseCode != 200) {
                    Log.d("CloudBrandManager", "Sync saltato (HTTP ${connection.responseCode})")
                    return@withContext
                }

                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                val newBrands = mutableListOf<CloudBrand>()

                val logosDir = File(context.filesDir, "cloud_logos")
                if (!logosDir.exists()) {
                    logosDir.mkdirs()
                }

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = obj.getString("name")
                    val iconUrl = obj.getString("iconUrl")
                    
                    val keywordsArray = obj.getJSONArray("keywords")
                    val keywords = mutableListOf<String>()
                    for (j in 0 until keywordsArray.length()) {
                        keywords.add(keywordsArray.getString(j).lowercase())
                    }

                    val filename = iconUrl.hashCode().toString() + ".png"
                    val file = File(logosDir, filename)

                    if (!file.exists()) {
                        try {
                            val imgConn = URL(iconUrl).openConnection() as HttpURLConnection
                            imgConn.connectTimeout = 5000
                            imgConn.readTimeout = 5000
                            val bytes = imgConn.inputStream.use { it.readBytes() }
                            file.writeBytes(bytes)
                        } catch (e: Exception) {
                            Log.e("CloudBrandManager", "Errore download logo $name: ${e.message}")
                        }
                    }

                    if (file.exists()) {
                        newBrands.add(CloudBrand(name, keywords, iconUrl, file.absolutePath))
                    }
                }

                cloudBrands = newBrands
                Log.d("CloudBrandManager", "Sincronizzati ${newBrands.size} brand dal cloud.")
            } catch (e: Exception) {
                Log.d("CloudBrandManager", "Nessun brand cloud aggiornato (${e.message})")
            }
        }
    }
}
