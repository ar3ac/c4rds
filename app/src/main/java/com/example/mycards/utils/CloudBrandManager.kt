package com.example.mycards.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
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
                val jsonString = URL(CLOUD_BRANDS_URL).readText()
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

                    // Nome file basato sull'hash dell'URL
                    val filename = iconUrl.hashCode().toString() + ".png"
                    val file = File(logosDir, filename)

                    // Se non esiste, lo scarica
                    if (!file.exists()) {
                        try {
                            val bytes = URL(iconUrl).readBytes()
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
                Log.e("CloudBrandManager", "Errore sync brand dal cloud: ${e.message}")
            }
        }
    }
}
