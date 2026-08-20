package com.example.mycards.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object Add : Screen("add?cardId={cardId}&initialValue={initialValue}&initialFormat={initialFormat}&initialName={initialName}&initialOwner={initialOwner}") {
        fun createRoute(
            cardId: Long = -1L,
            initialValue: String = "",
            initialFormat: String = "",
            initialName: String = "",
            initialOwner: String = ""
        ): String {
            val encodedVal = UriUtil.encode(initialValue)
            val encodedFmt = UriUtil.encode(initialFormat)
            val encodedName = UriUtil.encode(initialName)
            val encodedOwner = UriUtil.encode(initialOwner)
            return "add?cardId=$cardId&initialValue=$encodedVal&initialFormat=$encodedFmt&initialName=$encodedName&initialOwner=$encodedOwner"
        }
    }
    object Detail : Screen("detail/{cardId}") {
        fun createRoute(cardId: Long) = "detail/$cardId"
    }
    object Settings : Screen("settings?importUri={importUri}") {
        fun createRoute(importUri: String = ""): String {
            val encodedUri = UriUtil.encode(importUri)
            return "settings?importUri=$encodedUri"
        }
    }
}

object UriUtil {
    fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")
    fun decode(value: String): String = java.net.URLDecoder.decode(value, "UTF-8")
}
