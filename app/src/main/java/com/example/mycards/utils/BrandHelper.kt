package com.example.mycards.utils

import androidx.annotation.DrawableRes
import com.example.mycards.R

data class BrandInfo(
    val brandName: String,
    @DrawableRes val iconResId: Int?
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

    fun getBrandInfo(cardName: String): BrandInfo {
        val nameLower = cardName.trim().lowercase()

        return when {
            nameLower.contains("esselunga") -> BrandInfo("Esselunga", R.drawable.ic_brand_esselunga)
            nameLower.contains("conad") -> BrandInfo("Conad", R.drawable.ic_brand_conad)
            nameLower.contains("coop") -> BrandInfo("Coop", R.drawable.ic_brand_coop)
            nameLower.contains("lidl") -> BrandInfo("Lidl", R.drawable.ic_brand_lidl)
            nameLower.contains("eurospin") -> BrandInfo("Eurospin", R.drawable.ic_brand_eurospin)
            nameLower.contains("iperal") -> BrandInfo("Iperal", R.drawable.ic_brand_iperal)
            nameLower.contains("carrefour") -> BrandInfo("Carrefour", R.drawable.ic_brand_carrefour)
            nameLower.contains("decathlon") -> BrandInfo("Decathlon", R.drawable.ic_brand_decathlon)
            nameLower.contains("ikea") -> BrandInfo("IKEA", R.drawable.ic_brand_ikea)
            nameLower.contains("tigot") -> BrandInfo("Tigotà", R.drawable.ic_brand_tigota)
            nameLower.contains("crai") -> BrandInfo("Crai", R.drawable.ic_brand_crai)
            nameLower.contains("md") -> BrandInfo("MD", R.drawable.ic_brand_md)
            nameLower.contains("pam") -> BrandInfo("Pam", R.drawable.ic_brand_pam)
            nameLower.contains("bennet") -> BrandInfo("Bennet", R.drawable.ic_brand_bennet)
            nameLower.contains("despar") || nameLower.contains("spar") -> BrandInfo("Despar", R.drawable.ic_brand_despar)
            nameLower.contains("sephora") -> BrandInfo("Sephora", R.drawable.ic_brand_sephora)
            nameLower.contains("unieuro") -> BrandInfo("Unieuro", R.drawable.ic_brand_unieuro)
            nameLower.contains("mediaworld") -> BrandInfo("MediaWorld", R.drawable.ic_brand_mediaworld)
            nameLower.contains("leroy") -> BrandInfo("Leroy Merlin", R.drawable.ic_brand_leroymerlin)
            else -> BrandInfo(cardName, null)
        }
    }
}
