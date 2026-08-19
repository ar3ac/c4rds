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

    private fun createShortcutIcon(context: Context, cardName: String, brandInfo: BrandInfo): IconCompat {
        val size = 192 // Dimensione standard per icone adattive
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        if (brandInfo.iconResId != null) {
            // Sfondo bianco
            paint.color = android.graphics.Color.WHITE
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, brandInfo.iconResId!!)
            if (drawable != null) {
                val padding = 32
                drawable.setBounds(padding, padding, size - padding, size - padding)
                drawable.draw(canvas)
            }
        } else {
            // Colori stile avatar
            val colors = listOf(
                android.graphics.Color.parseColor("#E53935"), // Red
                android.graphics.Color.parseColor("#8E24AA"), // Purple
                android.graphics.Color.parseColor("#3949AB"), // Indigo
                android.graphics.Color.parseColor("#039BE5"), // Light Blue
                android.graphics.Color.parseColor("#00897B"), // Teal
                android.graphics.Color.parseColor("#43A047"), // Green
                android.graphics.Color.parseColor("#F4511E")  // Deep Orange
            )
            val index = kotlin.math.abs(cardName.hashCode()) % colors.size
            paint.color = colors[index]
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

            val initial = if (cardName.isNotBlank()) cardName.trim().take(1).uppercase() else "?"
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.WHITE
                this.textSize = size * 0.5f
                this.typeface = android.graphics.Typeface.DEFAULT_BOLD
                this.textAlign = android.graphics.Paint.Align.CENTER
            }
            val xPos = (canvas.width / 2).toFloat()
            val yPos = (canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(initial, xPos, yPos, textPaint)
        }

        return IconCompat.createWithAdaptiveBitmap(bitmap)
    }

    fun createCardShortcut(context: Context, card: CardEntity) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Scorciatoie non supportate sul tuo launcher", Toast.LENGTH_SHORT).show()
            return
        }

        val brandInfo = BrandHelper.getBrandInfo(card.name)
        val icon = createShortcutIcon(context, card.name, brandInfo)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mycards://card/${card.id}"), context, MainActivity::class.java).apply {
            putExtra("cardId", card.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val shortcutLabel = if (card.owner.isNotBlank()) "${card.name} (${card.owner})" else card.name

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "card_shortcut_${card.id}")
            .setShortLabel(card.name)
            .setLongLabel(shortcutLabel)
            .setIcon(icon)
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
            val icon = createShortcutIcon(context, card.name, brandInfo)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mycards://card/${card.id}"), context, MainActivity::class.java).apply {
                putExtra("cardId", card.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val shortcutLabel = if (card.owner.isNotBlank()) "${card.name} (${card.owner})" else card.name

            ShortcutInfoCompat.Builder(context, "dynamic_card_${card.id}")
                .setShortLabel(card.name)
                .setLongLabel(shortcutLabel)
                .setIcon(icon)
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
