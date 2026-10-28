// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.card

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.annotation.WorkerThread
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

/** Decoding and storing card images (photo picker, crop screen, theme packs). */
object CardImages {
    /** Long side of a stored card image; plenty for a card that is at most half the screen. */
    const val MAX_SIDE = 1080

    /** Decodes [uri] downscaled to at most [MAX_SIDE] on the long side, or null. */
    @WorkerThread
    fun decode(context: Context, uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decode(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            sampled { context.contentResolver.openInputStream(uri)!! }
        }
    }.getOrNull()

    /** Same as [decode] for image bytes (theme packs). */
    @WorkerThread
    fun decode(bytes: ByteArray): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decode(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))
        } else {
            sampled { bytes.inputStream() }
        }
    }.getOrNull()

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun decode(source: ImageDecoder.Source): Bitmap =
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val s = MAX_SIDE.toFloat() / max(info.size.width, info.size.height)
            if (s < 1f) decoder.setTargetSize((info.size.width * s).toInt(), (info.size.height * s).toInt())
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

    private fun sampled(open: () -> java.io.InputStream): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        return open().use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })!! }
    }

    /** Cuts [crop] (bitmap pixels) out of [bitmap]; the whole bitmap if [crop] is null. */
    fun crop(bitmap: Bitmap, crop: Rect?): Bitmap {
        if (crop == null) return bitmap
        val r = Rect(crop).apply { if (!intersect(0, 0, bitmap.width, bitmap.height)) return bitmap }
        if (r.width() < 8 || r.height() < 8) return bitmap
        return Bitmap.createBitmap(bitmap, r.left, r.top, r.width(), r.height())
    }

    /** Stores [bitmap] as WebP (keeps transparency, so cut-out characters stay cut-out). */
    @WorkerThread
    fun write(bitmap: Bitmap, target: File): Boolean = runCatching {
        target.parentFile?.mkdirs()
        val tmp = File(target.path + ".tmp")
        tmp.outputStream().use {
            @Suppress("DEPRECATION")
            val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            check(bitmap.compress(format, 85, it))
        }
        check(tmp.renameTo(target) || (target.delete() && tmp.renameTo(target)))
        true
    }.getOrDefault(false)
}
