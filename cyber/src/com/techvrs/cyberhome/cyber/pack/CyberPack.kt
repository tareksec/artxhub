// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.pack

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import com.techvrs.cyberhome.cyber.board.BoardPart
import com.techvrs.cyberhome.cyber.board.BuiltInCharacter
import com.techvrs.cyberhome.cyber.board.BuiltInWallpaper
import com.techvrs.cyberhome.cyber.board.CozyBoard
import com.techvrs.cyberhome.cyber.board.CyberWallpaper
import com.techvrs.cyberhome.cyber.widgets.card.CardImages
import com.techvrs.cyberhome.cyber.widgets.card.CardStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * CyberHome pack: one .zip that is both a **theme pack** (art + labels made by someone, applied in
 * one tap) and the **backup** of our own settings (Lawnchair's backup only covers the launcher's
 * layout and its own preferences). Format: docs/THEME_PACKS.md.
 *
 * Import is defensive: unknown keys are ignored, only our own preference files can be written,
 * entry names with ".." are skipped and sizes are capped (no zip bombs).
 */
object CyberPack {
    private const val TAG = "CyberPack"
    const val MANIFEST = "cyberhome-pack.json"
    const val FORMAT = 1
    const val MIME = "application/zip"

    /** Our preference files that a backup carries (not onboarding / layout-reset state). */
    private val PREF_FILES = listOf("cyber_board", "cyber_cards", "cyber_counter", "cyber_water", "cyber_weather", "cyber_look")

    private val SLOT_KEYS = mapOf("blue" to CozyBoard.SLOT_BLUE, "pink" to CozyBoard.SLOT_PINK, "teal" to CozyBoard.SLOT_TEAL)

    private const val MAX_ENTRIES = 64
    private const val MAX_ENTRY_BYTES = 12L shl 20
    private const val MAX_TOTAL_BYTES = 48L shl 20

    enum class Result { OK, NOT_A_PACK, TOO_NEW, FAILED }

    /** Writes a full backup (settings + own card images + own wallpaper) to [out]. */
    @WorkerThread
    fun export(context: Context, out: OutputStream): Boolean = runCatching {
        ZipOutputStream(out).use { zip ->
            val json = JSONObject().put("format", FORMAT).put("name", "CyberHome backup").put("backup", true)
            val cards = JSONObject()
            val store = CardStore(context)
            SLOT_KEYS.forEach { (key, slot) ->
                val file = store.imageFile(slot)
                if (file.exists()) {
                    val path = "cards/$key.webp"
                    zip.putNextEntry(ZipEntry(path))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    cards.put(key, JSONObject().put("image", path))
                }
            }
            json.put("cards", cards)
            val wallpaper = CyberWallpaper.customFile(context)
            if (CyberWallpaper.choice(context) == CyberWallpaper.CUSTOM && wallpaper.exists()) {
                zip.putNextEntry(ZipEntry("wallpaper.jpg"))
                wallpaper.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                json.put("wallpaper", JSONObject().put("image", "wallpaper.jpg"))
            }
            val settings = JSONObject()
            PREF_FILES.forEach { name -> settings.put(name, dumpPrefs(context, name)) }
            json.put("settings", settings)
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(json.toString(2).toByteArray())
            zip.closeEntry()
        }
        true
    }.onFailure { Log.w(TAG, "export failed", it) }.getOrDefault(false)

    /** Applies a theme pack or restores a backup from [input]. */
    @WorkerThread
    fun import(context: Context, input: InputStream): Result {
        val entries = runCatching { readZip(input) }.getOrElse {
            Log.w(TAG, "not a readable zip", it)
            return Result.NOT_A_PACK
        }
        val json = entries[MANIFEST]?.let { runCatching { JSONObject(String(it)) }.getOrNull() } ?: return Result.NOT_A_PACK
        if (json.optInt("format", 0) > FORMAT) return Result.TOO_NEW
        return runCatching {
            // Settings first, so the explicit pack parts below win.
            json.optJSONObject("settings")?.let { settings ->
                PREF_FILES.forEach { name -> settings.optJSONObject(name)?.let { restorePrefs(context, name, it) } }
            }
            val backup = json.optBoolean("backup", false)
            val cards = json.optJSONObject("cards") ?: JSONObject()
            if (backup) {
                // A backup is the whole state: cards without an image there use their character.
                SLOT_KEYS.forEach { (key, slot) -> if (!cards.has(key)) CardStore(context).imageFile(slot).delete() }
            }
            applyCards(context, cards, entries)
            json.optJSONObject("board")?.let { applyBoard(context, it) }
            val wallpaper = json.optJSONObject("wallpaper") ?: if (backup) JSONObject() else null
            wallpaper?.let { applyWallpaper(context, it, entries) }
            CozyBoard.notifyChanged()
            Result.OK
        }.onFailure { Log.w(TAG, "import failed", it) }.getOrDefault(Result.FAILED)
    }

    private fun applyCards(context: Context, cards: JSONObject, entries: Map<String, ByteArray>) {
        val store = CardStore(context)
        SLOT_KEYS.forEach { (key, slot) ->
            val card = cards.optJSONObject(key) ?: return@forEach
            card.optString("character").takeIf { it.isNotEmpty() }?.let { name ->
                runCatching { BuiltInCharacter.valueOf(name.uppercase()) }.getOrNull()?.let {
                    CozyBoard.setCharacter(context, slot, it)
                    store.imageFile(slot).delete()
                }
            }
            card.optString("image").takeIf { it.isNotEmpty() }?.let { path ->
                val bitmap = entries[path]?.let { CardImages.decode(it) }
                if (bitmap != null) CardImages.write(bitmap, store.imageFile(slot)) else Log.w(TAG, "card image missing: $path")
            }
            store.setTexts(slot, card.optStringOrNull("title"), card.optStringOrNull("number"), card.optStringOrNull("caption"))
        }
    }

    private fun applyBoard(context: Context, board: JSONObject) {
        if (board.has("decoration")) CozyBoard.setDecoration(context, board.getBoolean("decoration"))
        if (board.has("mirrored")) CozyBoard.setMirrored(context, board.getBoolean("mirrored"))
        board.optJSONArray("hidden")?.let { hidden ->
            val names = (0 until hidden.length()).map { hidden.optString(it).uppercase() }.toSet()
            BoardPart.entries.forEach { CozyBoard.setHidden(context, it, it.name in names) }
        }
    }

    private fun applyWallpaper(context: Context, wallpaper: JSONObject, entries: Map<String, ByteArray>) {
        val image = wallpaper.optStringOrNull("image")
        val builtIn = wallpaper.optStringOrNull("builtin")
        when {
            image != null -> entries[image]?.let { bytes -> CyberWallpaper.upload(context) { bytes.inputStream() } }
            builtIn.equals(CyberWallpaper.AUTO, ignoreCase = true) -> CyberWallpaper.choose(context, CyberWallpaper.AUTO)
            builtIn != null -> runCatching { BuiltInWallpaper.valueOf(builtIn.uppercase()) }.getOrNull()
                ?.let { CyberWallpaper.choose(context, it.name) }
            // A restored backup brings its own choice in cyber_look; apply it.
            else -> CyberWallpaper.apply(context)
        }
    }

    private fun readZip(input: InputStream): Map<String, ByteArray> {
        val out = HashMap<String, ByteArray>()
        var total = 0L
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (entry.isDirectory || name.contains("..") || name.startsWith("/")) continue
                check(out.size < MAX_ENTRIES) { "too many entries" }
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(16 * 1024)
                var size = 0L
                while (true) {
                    val n = zip.read(chunk)
                    if (n < 0) break
                    size += n
                    total += n
                    check(size <= MAX_ENTRY_BYTES && total <= MAX_TOTAL_BYTES) { "pack too large" }
                    buffer.write(chunk, 0, n)
                }
                val isImage = name.endsWith(".png", true) || name.endsWith(".jpg", true) ||
                        name.endsWith(".jpeg", true) || name.endsWith(".webp", true)
                val bytes = buffer.toByteArray()
                if (isImage && !isSafeImage(bytes)) {
                    Log.w(TAG, "Rejected unsafe or corrupted image entry: $name")
                    continue
                }
                out[name] = bytes
            }
        }
        return out
    }

    private fun isSafeImage(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val isPng = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        val isJpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        val isWebp = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()

        if (!isPng && !isJpeg && !isWebp) return false

        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        val w = opts.outWidth
        val h = opts.outHeight
        if (w <= 0 || h <= 0 || w > 4096 || h > 4096 || (w.toLong() * h.toLong() > 40_000_000L)) {
            return false
        }
        return true
    }

    /** Typed dump: {"key": {"t": "s|b|i|l|f|set", "v": value}}. */
    private fun dumpPrefs(context: Context, name: String): JSONObject {
        val json = JSONObject()
        context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
            val typed = when (value) {
                is String -> JSONObject().put("t", "s").put("v", value)
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                is Set<*> -> JSONObject().put("t", "set").put("v", JSONArray(value.map { it.toString() }))
                else -> null
            }
            typed?.let { json.put(key, it) }
        }
        return json
    }

    private fun restorePrefs(context: Context, name: String, json: JSONObject) {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) {
            clear()
            json.keys().forEach { key ->
                val typed = json.optJSONObject(key) ?: return@forEach
                when (typed.optString("t")) {
                    "s" -> putString(key, typed.optString("v"))
                    "b" -> putBoolean(key, typed.optBoolean("v"))
                    "i" -> putInt(key, typed.optInt("v"))
                    "l" -> putLong(key, typed.optLong("v"))
                    "f" -> putFloat(key, typed.optDouble("v").toFloat())
                    "set" -> typed.optJSONArray("v")?.let { a -> putStringSet(key, (0 until a.length()).map { a.optString(it) }.toSet()) }
                }
            }
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) optString(key) else null
}
