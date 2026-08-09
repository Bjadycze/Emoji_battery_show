package com.emojibattery.free

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Path
import android.graphics.Typeface

/**
 * Vykreslí emoji nebo číslo do bitmapy.
 *
 * Pozn.: malou ikonu notifikace kreslí systém pouze z alfa kanálu, takže ve stavovém
 * řádku vznikne jednobarevná silueta emoji. V bublině a ve widgetu je emoji plnobarevné.
 */
object EmojiRenderer {

    fun emoji(glyph: String, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.78f
        }
        // Některá emoji (např. sekvence se ZWJ) jsou širší – zmenši, ať se vejdou.
        while (paint.measureText(glyph) > sizePx * 0.98f && paint.textSize > 4f) {
            paint.textSize -= 1f
        }
        val fm = paint.fontMetrics
        canvas.drawText(glyph, sizePx / 2f, sizePx / 2f - (fm.ascent + fm.descent) / 2f, paint)
        return bmp
    }

    /**
     * Kreslená baterie: délka výplně odpovídá procentům, barva jde plynule
     * z červené do zelené. Při [colored] = false vznikne bílá kresba, ze které
     * systém ve stavovém řádku udělá siluetu – úroveň zůstane čitelná.
     */
    fun battery(level: Int, charging: Boolean, sizePx: Int, colored: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val l = level.coerceIn(0, 100)

        val width = sizePx * 0.88f
        val height = sizePx * 0.54f
        val left = (sizePx - width) / 2f
        val top = (sizePx - height) / 2f
        val stroke = sizePx * 0.075f
        val radius = sizePx * 0.10f

        val body = RectF(left, top, left + width * 0.88f, top + height)
        val cap = RectF(body.right + stroke * 0.4f, top + height * 0.28f, left + width, top + height * 0.72f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (colored) levelColor(l) else android.graphics.Color.WHITE
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        canvas.drawRoundRect(body, radius, radius, paint)

        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cap, radius * 0.4f, radius * 0.4f, paint)

        val inset = stroke * 1.15f
        val usable = body.width() - 2 * inset
        val filled = usable * l / 100f
        if (filled > 1f) {
            val fill = RectF(
                body.left + inset,
                body.top + inset,
                body.left + inset + filled,
                body.bottom - inset
            )
            canvas.drawRoundRect(fill, radius * 0.35f, radius * 0.35f, paint)
        }

        if (charging) {
            // Blesk se do baterie „vyřízne“, aby byl vidět i v siluetě.
            val cx = body.centerX()
            val cy = body.centerY()
            val w = sizePx * 0.13f
            val h = height * 0.62f
            val bolt = Path().apply {
                moveTo(cx + w * 0.5f, cy - h / 2)
                lineTo(cx - w * 0.7f, cy + h * 0.08f)
                lineTo(cx + w * 0.05f, cy + h * 0.08f)
                lineTo(cx - w * 0.45f, cy + h / 2)
                lineTo(cx + w * 0.8f, cy - h * 0.06f)
                lineTo(cx - w * 0.02f, cy - h * 0.06f)
                close()
            }
            val cut = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = sizePx * 0.045f
            }
            canvas.drawPath(bolt, cut)
        }
        return bmp
    }

    /** 0 % sytě červená, 100 % zelená. */
    private fun levelColor(level: Int): Int {
        val hue = when {
            level <= 10 -> 0f
            else -> 8f + (level - 10) / 90f * 112f
        }
        return android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.88f, 0.95f))
    }

    fun number(level: Int, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val text = level.coerceIn(0, 100).toString()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = sizePx * 0.95f
        }
        while (paint.measureText(text) > sizePx * 0.92f && paint.textSize > 4f) {
            paint.textSize -= 1f
        }
        val fm = paint.fontMetrics
        canvas.drawText(text, sizePx / 2f, sizePx / 2f - (fm.ascent + fm.descent) / 2f, paint)
        return bmp
    }

    /**
     * Vykreslí emoji a vedle něj číslo.
     * Protože stavový řádek má omezený prostor, bitmapa zůstane čtvercová,
     * ale oba prvky se zmenší a umístí vedle sebe.
     */
    fun emojiWithNumber(glyph: String, level: Int, sizePx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val text = level.coerceIn(0, 100).toString()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.55f
        }

        // 1. Emoji (vlevo nahoře)
        val fmEmoji = paint.fontMetrics
        canvas.drawText(glyph, sizePx * 0.3f, sizePx * 0.45f - (fmEmoji.ascent + fmEmoji.descent) / 2f, paint)

        // 2. Číslo (vpravo dole)
        paint.textSize = sizePx * 0.52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val fmText = paint.fontMetrics
        canvas.drawText(text, sizePx * 0.7f, sizePx * 0.75f - (fmText.ascent + fmText.descent) / 2f, paint)

        return bmp
    }

    /** Vykreslí kreslenou baterii s číslem vedle. */
    fun batteryWithNumber(level: Int, charging: Boolean, sizePx: Int, colored: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val l = level.coerceIn(0, 100)

        // Zmenšená baterie vlevo
        val bSize = (sizePx * 0.6f).toInt()
        val batteryBmp = battery(level, charging, bSize, colored)
        canvas.drawBitmap(batteryBmp, 0f, sizePx * 0.1f, null)

        // Číslo vpravo
        val text = l.toString()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (colored) levelColor(l) else Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = sizePx * 0.5f
        }
        val fm = paint.fontMetrics
        canvas.drawText(text, sizePx * 0.75f, sizePx / 2f - (fm.ascent + fm.descent) / 2f, paint)

        return bmp
    }
}
