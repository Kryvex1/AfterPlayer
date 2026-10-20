package dev.anilbeesetti.nextplayer.feature.player.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Stub ContentProvider for the "androidx.car.app.connection" authority.
 *
 * Media3's [androidx.media3.session.AndroidAutoConnectionStateObserver] queries
 * "content://androidx.car.app.connection" to determine whether the device is connected
 * to Android Auto / Automotive OS. On devices or emulators without an external car host,
 * Android's ActivityThread logs "Failed to find provider info for androidx.car.app.connection"
 * when this provider is missing. Providing this local stub cleanly satisfies the query
 * by reporting [CONNECTION_TYPE_NOT_CONNECTED] (0) without generating system errors.
 */
class CarConnectionProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val column = CAR_CONNECTION_STATE_COLUMN
        val columns = if (projection != null && projection.isNotEmpty()) projection else arrayOf(column)
        return MatrixCursor(columns).apply {
            val row = Array<Any?>(columns.size) { null }
            val stateIndex = columns.indexOf(column)
            if (stateIndex != -1) {
                row[stateIndex] = CONNECTION_TYPE_NOT_CONNECTED
            }
            addRow(row)
        }
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "androidx.car.app.connection"
        private const val CAR_CONNECTION_STATE_COLUMN = "CarConnectionState"
        private const val CONNECTION_TYPE_NOT_CONNECTED = 0
    }
}
