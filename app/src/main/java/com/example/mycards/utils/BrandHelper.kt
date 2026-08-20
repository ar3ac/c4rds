package com.example.mycards.utils

import androidx.annotation.DrawableRes
import com.example.mycards.R

data class BrandInfo(
    val brandName: String,
    @DrawableRes val iconResId: Int? = null,
    val localFilePath: String? = null
)

object BrandHelper {

    val knownBrands = listOf(
        BrandInfo("Esselunga", R.drawable.ic_brand_esselunga),
        BrandInfo("Conad", R.drawable.ic_brand_conad),
        BrandInfo("Coop", R.drawable.ic_brand_coop),
        BrandInfo("Lidl", R.drawable.ic_brand_lidl),
        BrandInfo("Eurospin", R.drawable.ic_brand_eurospin),
        BrandInfo("Iperal", R.drawable.ic_brand_iperal),
        BrandInfo("Carrefour", R.drawable.ic_brand_carrefour),
        BrandInfo("Decathlon", R.drawable.ic_brand_decathlon),
        BrandInfo("IKEA", R.drawable.ic_brand_ikea),
        BrandInfo("Tigotà", R.drawable.ic_brand_tigota),
        BrandInfo("Crai", R.drawable.ic_brand_crai),
        BrandInfo("MD", R.drawable.ic_brand_md),
        BrandInfo("Pam", R.drawable.ic_brand_pam),
        BrandInfo("Bennet", R.drawable.ic_brand_bennet),
        BrandInfo("Despar", R.drawable.ic_brand_despar),
        BrandInfo("Sephora", R.drawable.ic_brand_sephora),
        BrandInfo("Unieuro", R.drawable.ic_brand_unieuro),
        BrandInfo("MediaWorld", R.drawable.ic_brand_mediaworld),
        BrandInfo("Leroy Merlin", R.drawable.ic_brand_leroymerlin)
    )

    fun getAllBrands(): List<BrandInfo> {
        val cloud = CloudBrandManager.cloudBrands.map {
            BrandInfo(it.name, null, it.localFilePath)
        }
        return knownBrands + cloud
    }

    fun getBrandInfo(cardName: String): BrandInfo {
        val nameLower = cardName.trim().lowercase()

        // Prima controlliamo i brand hardcoded locali per massima priorità
        when {
            nameLower.contains("esselunga") -> return BrandInfo("Esselunga", R.drawable.ic_brand_esselunga)
            nameLower.contains("conad") -> return BrandInfo("Conad", R.drawable.ic_brand_conad)
            nameLower.contains("coop") -> return BrandInfo("Coop", R.drawable.ic_brand_coop)
            nameLower.contains("lidl") -> return BrandInfo("Lidl", R.drawable.ic_brand_lidl)
            nameLower.contains("eurospin") -> return BrandInfo("Eurospin", R.drawable.ic_brand_eurospin)
            nameLower.contains("iperal") -> return BrandInfo("Iperal", R.drawable.ic_brand_iperal)
            nameLower.contains("carrefour") -> return BrandInfo("Carrefour", R.drawable.ic_brand_carrefour)
            nameLower.contains("decathlon") -> return BrandInfo("Decathlon", R.drawable.ic_brand_decathlon)
            nameLower.contains("ikea") -> return BrandInfo("IKEA", R.drawable.ic_brand_ikea)
            nameLower.contains("tigot") -> return BrandInfo("Tigotà", R.drawable.ic_brand_tigota)
            nameLower.contains("crai") -> return BrandInfo("Crai", R.drawable.ic_brand_crai)
            nameLower.contains("md") -> return BrandInfo("MD", R.drawable.ic_brand_md)
            nameLower.contains("pam") -> return BrandInfo("Pam", R.drawable.ic_brand_pam)
            nameLower.contains("bennet") -> return BrandInfo("Bennet", R.drawable.ic_brand_bennet)
            nameLower.contains("despar") || nameLower.contains("spar") -> return BrandInfo("Despar", R.drawable.ic_brand_despar)
            nameLower.contains("sephora") -> return BrandInfo("Sephora", R.drawable.ic_brand_sephora)
            nameLower.contains("unieuro") -> return BrandInfo("Unieuro", R.drawable.ic_brand_unieuro)
            nameLower.contains("mediaworld") -> return BrandInfo("MediaWorld", R.drawable.ic_brand_mediaworld)
            nameLower.contains("leroy") -> return BrandInfo("Leroy Merlin", R.drawable.ic_brand_leroymerlin)
        }

        // Se non trovato, cerchiamo nei cloud brands
        val cloudBrand = CloudBrandManager.cloudBrands.find { cloud ->
            cloud.keywords.any { keyword -> nameLower.contains(keyword) }
        }

        if (cloudBrand != null) {
            return BrandInfo(cloudBrand.name, null, cloudBrand.localFilePath)
        }

        // Nessun match
        return BrandInfo(cardName, null, null)
    }
}
