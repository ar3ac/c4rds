package com.example.mycards.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.mycards.MainActivity
import com.example.mycards.R
import com.example.mycards.data.model.CardEntity

object ShortcutHelper {

    fun createCardShortcut(context: Context, card: CardEntity) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Scorciatoie non supportate sul tuo launcher", Toast.LENGTH_SHORT).show()
            return
        }

        val brandInfo = BrandHelper.getBrandInfo(card.name)
        val iconResId = brandInfo.iconResId ?: R.mipmap.ic_launcher

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mycards://card/${card.id}"), context, MainActivity::class.java).apply {
            putExtra("cardId", card.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val shortcutLabel = if (card.owner.isNotBlank()) "${card.name} (${card.owner})" else card.name

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "card_shortcut_${card.id}")
            .setShortLabel(card.name)
            .setLongLabel(shortcutLabel)
            .setIcon(IconCompat.createWithResource(context, iconResId))
            .setIntent(intent)
            .build()

        val success = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
        if (success) {
            Toast.makeText(context, "Scorciatoia di \"${card.name}\" creata sulla Home", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateDynamicShortcuts(context: Context, cards: List<CardEntity>) {
        val shortcuts = cards.take(4).map { card ->
            val brandInfo = BrandHelper.getBrandInfo(card.name)
            val iconResId = brandInfo.iconResId ?: R.mipmap.ic_launcher

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mycards://card/${card.id}"), context, MainActivity::class.java).apply {
                putExtra("cardId", card.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val shortcutLabel = if (card.owner.isNotBlank()) "${card.name} (${card.owner})" else card.name

            ShortcutInfoCompat.Builder(context, "dynamic_card_${card.id}")
                .setShortLabel(card.name)
                .setLongLabel(shortcutLabel)
                .setIcon(IconCompat.createWithResource(context, iconResId))
                .setIntent(intent)
                .build()
        }

        try {
            ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
