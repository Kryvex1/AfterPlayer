package dev.anilbeesetti.nextplayer.core.media.services

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dev.anilbeesetti.nextplayer.core.common.di.DiQualifiers
import dev.anilbeesetti.nextplayer.core.common.extensions.VIDEO_COLLECTION_URI
import dev.anilbeesetti.nextplayer.core.common.extensions.prettyName
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * [MediaService] implementation that queries the Android MediaStore for video files.
 *
 * This implementation provides a simple, flat view of media:
 * - Videos are fetched recursively from the specified folder path
 * - Folders are derived from video parent directories
 */
@Single
class MediaStoreMediaService(
    private val context: Context,
    @Named(DiQualifiers.APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
) : MediaService {

    companion object {
        private const val OBSERVER_DEBOUNCE_MS = 250L

        private val VIDEO_PROJECTION = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATE_ADDED,
        )
    }

    /**
     * A single, shared signal of MediaStore changes.
     *
     * One [ContentObserver] is registered for all observers (regardless of folder path),
     * and bursts of change notifications (common during media scans) are coalesced via
     * [debounce] so downstream collectors re-query at most once per quiet window.
     */
    @OptIn(FlowPreview::class)
    private val mediaChanges: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(VIDEO_COLLECTION_URI, true, observer)
        trySend(Unit)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
        .debounce(OBSERVER_DEBOUNCE_MS.milliseconds)
        .shareIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            replay = 1,
        )

    override fun observeFolders(folderPath: String?): Flow<List<MediaFolder>> = observeMedia {
        fetchFolders(folderPath)
    }

    override fun observeVideos(folderPath: String?): Flow<List<MediaVideo>> = observeMedia {
        fetchVideos(folderPath)
    }

    override fun observeTrashVideos(): Flow<List<MediaVideo>> = observeMedia {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) queryTrashVideos() else emptyList()
    }

    private fun <T> observeMedia(query: suspend () -> List<T>): Flow<List<T>> {
        return mediaChanges
            .map {
                try {
                    query()
                } catch (e: IllegalArgumentException) {
                    // A volume can disappear before MediaProvider opens it. Handle each
                    // notification separately so observers can recover when storage returns.
                    if (e.message?.startsWith("Volume ") == true && e.message?.endsWith(" not found") == true) {
                        emptyList()
                    } else {
                        throw e
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }

    override suspend fun fetchFolders(folderPath: String?): List<MediaFolder> = withContext(Dispatchers.IO) {
        val videos = fetchVideos(folderPath)

        val videosByFolder = videos.groupBy { it.parentPath }

        return@withContext videosByFolder.map { (parentPath, folderVideos) ->
            val folderName = parentPath.substringAfterLast('/').ifEmpty { parentPath }
            MediaFolder(
                path = parentPath,
                name = folderName,
                dateModified = folderVideos.maxOfOrNull { video -> video.dateModified } ?: 0L,
                totalSize = folderVideos.sumOf { video -> video.size },
                totalDuration = folderVideos.sumOf { video -> video.duration },
                videosCount = folderVideos.size,
                foldersCount = 0,
            )
        }
    }

    override suspend fun fetchVideos(folderPath: String?): List<MediaVideo> = withContext(Dispatchers.IO) {
        queryVideos(folderPath)
    }

    override suspend fun findVideo(uri: Uri): MediaVideo? = withContext(Dispatchers.IO) {
        return@withContext try {
            context.contentResolver.query(
                uri,
                VIDEO_PROJECTION,
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@withContext null
                cursor.toMediaVideo()
            }
        } catch (e: Exception) {
            // uri isn't a MediaStore video uri (e.g. a vault FileProvider uri) and the
            // provider rejected the MediaStore-specific projection outright. Not finding a
            // MediaStore row for it is the correct outcome, not a crash.
            null
        }
    }

    override suspend fun findFolder(path: String): MediaFolder? = withContext(Dispatchers.IO) {
        val folderFile = File(path)
        if (!folderFile.exists()) return@withContext null

        val allVideos = fetchVideos(path)
        if (allVideos.isEmpty()) return@withContext null

        val directVideos = allVideos.filter { it.parentPath == path }
        val directSubfolders = allVideos
            .mapNotNull { video ->
                File(video.path).parentFile?.walkUp()?.firstOrNull { it.parent == path }
            }
            .distinctBy { it.path }

        return@withContext MediaFolder(
            path = folderFile.path,
            name = folderFile.prettyName,
            dateModified = allVideos.maxOfOrNull { it.dateModified } ?: folderFile.lastModified(),
            totalSize = allVideos.sumOf { it.size },
            totalDuration = allVideos.sumOf { it.duration },
            videosCount = directVideos.size,
            foldersCount = directSubfolders.size,
        )
    }

    /**
     * Walks up the directory tree from this file's parent to the root.
     */
    private fun File.walkUp() = generateSequence(parentFile) { it.parentFile }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun queryTrashVideos(): List<MediaVideo> {
        val queryArgs = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putStringArray(
                android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Video.Media.DATE_MODIFIED),
            )
            putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
        }
        return queryVideoCursor {
            context.contentResolver.query(VIDEO_COLLECTION_URI, VIDEO_PROJECTION, queryArgs, null)
        }
    }

    private fun queryVideos(folderPath: String?): List<MediaVideo> {
        // A null folderPath scans every storage volume (e.g. SD cards / USB OTG). For a specific
        // folder, match it and its descendants, escaping LIKE metacharacters ('%', '_') in the path.
        val selection = if (folderPath == null) null else "${MediaStore.Video.Media.DATA} LIKE ? ESCAPE '\\'"
        val selectionArgs = if (folderPath == null) null else arrayOf("${folderPath.escapeLike()}/%")
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
        return queryVideoCursor {
            context.contentResolver.query(
                VIDEO_COLLECTION_URI,
                VIDEO_PROJECTION,
                selection,
                selectionArgs,
                sortOrder,
            )
        }
    }

    private fun queryVideoCursor(query: () -> Cursor?): List<MediaVideo> {
        val mediaVideos = mutableListOf<MediaVideo>()
        val cursor = checkNotNull(query()) {
            "MediaStore returned no cursor for the video query"
        }
        cursor.use {
            val dataIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA).takeIf { it >= 0 } ?: return emptyList()
            val idIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID).takeIf { it >= 0 } ?: return emptyList()
            val displayNameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val durationIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val widthIndex = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightIndex = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
            val sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
            val dateAddedIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataIndex) ?: continue
                val id = cursor.getLong(idIndex)
                val displayName = if (displayNameIndex >= 0) cursor.getString(displayNameIndex) ?: path.substringAfterLast('/') else path.substringAfterLast('/')
                val parentPath = path.substringBeforeLast('/', "/")
                mediaVideos.add(
                    MediaVideo(
                        id = id,
                        path = path,
                        title = displayName,
                        parentPath = parentPath,
                        uri = ContentUris.withAppendedId(VIDEO_COLLECTION_URI, id),
                        displayName = displayName,
                        duration = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L,
                        width = if (widthIndex >= 0) cursor.getInt(widthIndex) else 0,
                        height = if (heightIndex >= 0) cursor.getInt(heightIndex) else 0,
                        size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L,
                        dateModified = if (dateModifiedIndex >= 0) cursor.getLong(dateModifiedIndex) else 0L,
                        dateAdded = if (dateAddedIndex >= 0) cursor.getLong(dateAddedIndex) else 0L,
                    ),
                )
            }
        }
        return mediaVideos
    }

    /**
     * Escapes SQL LIKE metacharacters so a path is matched literally (used with `ESCAPE '\'`).
     */
    private fun String.escapeLike(): String =
        replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun Cursor.toMediaVideo(): MediaVideo? {
        val dataIndex = getColumnIndex(MediaStore.Video.Media.DATA).takeIf { it >= 0 } ?: return null
        val idIndex = getColumnIndex(MediaStore.Video.Media._ID).takeIf { it >= 0 } ?: return null
        val displayNameIndex = getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME).takeIf { it >= 0 } ?: return null
        val durationIndex = getColumnIndex(MediaStore.Video.Media.DURATION).takeIf { it >= 0 } ?: return null
        val widthIndex = getColumnIndex(MediaStore.Video.Media.WIDTH).takeIf { it >= 0 } ?: return null
        val heightIndex = getColumnIndex(MediaStore.Video.Media.HEIGHT).takeIf { it >= 0 } ?: return null
        val sizeIndex = getColumnIndex(MediaStore.Video.Media.SIZE).takeIf { it >= 0 } ?: return null
        val dateModifiedIndex = getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED).takeIf { it >= 0 } ?: return null
        val dateAddedIndex = getColumnIndex(MediaStore.Video.Media.DATE_ADDED).takeIf { it >= 0 } ?: return null

        val path = getString(dataIndex) ?: return null
        val id = getLong(idIndex)
        val displayName = getString(displayNameIndex) ?: path.substringAfterLast('/')
        val parentPath = path.substringBeforeLast('/', "/")
        return MediaVideo(
            id = id,
            path = path,
            title = displayName,
            parentPath = parentPath,
            uri = ContentUris.withAppendedId(VIDEO_COLLECTION_URI, id),
            displayName = displayName,
            duration = getLong(durationIndex),
            width = getInt(widthIndex),
            height = getInt(heightIndex),
            size = getLong(sizeIndex),
            dateModified = getLong(dateModifiedIndex),
            dateAdded = getLong(dateAddedIndex),
        )
    }
}
