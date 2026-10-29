// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.theme

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings.Favorites
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Manages atomic snapshots of workspace favorites rows (desktop screen 0 and hotseat)
 * before any theme change. Prevents user data loss and enables 100% clean reverts.
 */
object ThemeSnapshot {
    private const val TAG = "ThemeSnapshot"
    private const val BACKUP_DIR_NAME = "theme_backups"
    private const val MAX_SNAPSHOTS = 3

    /**
     * Creates a JSON snapshot of all rows on screen 0 of the desktop and the entire hotseat.
     * Saved in internal filesDir/theme_backups/<timestamp>.json.
     */
    fun createSnapshot(context: Context): String? = runCatching {
        val app = context.applicationContext
        val db = LauncherAppState.getInstance(app).model.modelDbController
        val backupDir = File(app.filesDir, BACKUP_DIR_NAME).apply { if (!exists()) mkdirs() }

        val itemsArray = JSONArray()
        val querySelection = "(${Favorites.CONTAINER} = ${Favorites.CONTAINER_DESKTOP} AND ${Favorites.SCREEN} = 0) OR (${Favorites.CONTAINER} = ${Favorites.CONTAINER_HOTSEAT})"

        db.query(Favorites.TABLE_NAME, null, querySelection, null, null).use { cursor ->
            val colCount = cursor.columnCount
            while (cursor.moveToNext()) {
                val rowObj = JSONObject()
                for (i in 0 until colCount) {
                    val colName = cursor.getColumnName(i)
                    when (cursor.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> rowObj.put(colName, cursor.getLong(i))
                        android.database.Cursor.FIELD_TYPE_FLOAT -> rowObj.put(colName, cursor.getDouble(i))
                        android.database.Cursor.FIELD_TYPE_STRING -> rowObj.put(colName, cursor.getString(i))
                        android.database.Cursor.FIELD_TYPE_BLOB -> {
                            // Convert blob to base64 if present
                            val blob = cursor.getBlob(i)
                            if (blob != null) {
                                rowObj.put(colName, android.util.Base64.encodeToString(blob, android.util.Base64.NO_WRAP))
                                rowObj.put("__is_blob_${colName}", true)
                            }
                        }
                    }
                }
                itemsArray.put(rowObj)
            }
        }

        val timestamp = System.currentTimeMillis()
        val snapshotFile = File(backupDir, "snapshot_$timestamp.json")
        val root = JSONObject().apply {
            put("timestamp", timestamp)
            put("items_count", itemsArray.length())
            put("items", itemsArray)
        }

        snapshotFile.writeText(root.toString())
        pruneSnapshots(backupDir)
        Log.i(TAG, "Snapshot created with ${itemsArray.length()} items: ${snapshotFile.name}")
        snapshotFile.absolutePath
    }.onFailure { Log.e(TAG, "Failed to create theme snapshot", it) }.getOrNull()

    /**
     * Restores the most recent snapshot into the favorites table.
     * Restores screen 0 and hotseat items safely inside a transaction.
     */
    fun restoreLatestSnapshot(context: Context): Boolean = runCatching {
        val app = context.applicationContext
        val backupDir = File(app.filesDir, BACKUP_DIR_NAME)
        if (!backupDir.exists()) return false

        val snapshotFiles = backupDir.listFiles { _, name -> name.startsWith("snapshot_") && name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: return false

        if (snapshotFiles.isEmpty()) return false
        val latestFile = snapshotFiles.first()

        val root = JSONObject(latestFile.readText())
        val itemsArray = root.getJSONArray("items")
        val db = LauncherAppState.getInstance(app).model.modelDbController

        // Clear existing screen 0 and hotseat items first
        val deleteSelection = "(${Favorites.CONTAINER} = ${Favorites.CONTAINER_DESKTOP} AND ${Favorites.SCREEN} = 0) OR (${Favorites.CONTAINER} = ${Favorites.CONTAINER_HOTSEAT})"
        db.delete(Favorites.TABLE_NAME, deleteSelection, null)

        var restoredCount = 0
        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.getJSONObject(i)
            val cv = ContentValues()
            val keys = itemObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.startsWith("__is_blob_")) continue

                if (itemObj.optBoolean("__is_blob_$key", false)) {
                    val base64Str = itemObj.getString(key)
                    cv.put(key, android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP))
                } else {
                    val value = itemObj.get(key)
                    when (value) {
                        is Int -> cv.put(key, value)
                        is Long -> cv.put(key, value)
                        is Double -> cv.put(key, value)
                        is String -> cv.put(key, value)
                        is Boolean -> cv.put(key, if (value) 1 else 0)
                    }
                }
            }
            if (db.insert(Favorites.TABLE_NAME, cv) >= 0) {
                restoredCount++
            }
        }

        Log.i(TAG, "Restored $restoredCount items from ${latestFile.name}")
        true
    }.onFailure { Log.e(TAG, "Failed to restore theme snapshot", it) }.getOrDefault(false)

    /** Keeps only the latest [MAX_SNAPSHOTS] files to preserve disk space */
    private fun pruneSnapshots(backupDir: File) {
        runCatching {
            val snapshotFiles = backupDir.listFiles { _, name -> name.startsWith("snapshot_") && name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?: return

            if (snapshotFiles.size > MAX_SNAPSHOTS) {
                for (i in MAX_SNAPSHOTS until snapshotFiles.size) {
                    snapshotFiles[i].delete()
                }
            }
        }
    }
}
