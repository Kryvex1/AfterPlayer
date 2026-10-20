package dev.anilbeesetti.nextplayer.core.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.io.File
import java.io.FileOutputStream

data class CustomFontConfig(
    val regularFontUri: String? = null,
    val boldFontUri: String? = null,
) {
    fun resolveFontFamily(context: Context): FontFamily {
        val regularFile = regularFontUri?.let { getCachedFontFile(context, it) }
        val boldFile = boldFontUri?.let { getCachedFontFile(context, it) }

        return when {
            // Case 1: Both Regular and Bold fonts are provided
            regularFile != null && boldFile != null -> {
                FontFamily(
                    Font(file = regularFile, weight = FontWeight.Normal),
                    Font(file = boldFile, weight = FontWeight.Bold),
                )
            }
            // Case 2: Only Regular is provided -> Use for both weights
            regularFile != null -> {
                FontFamily(
                    Font(file = regularFile, weight = FontWeight.Normal),
                    Font(file = regularFile, weight = FontWeight.Bold),
                )
            }
            // Case 3: Only Bold is provided -> Use for both weights
            boldFile != null -> {
                FontFamily(
                    Font(file = boldFile, weight = FontWeight.Normal),
                    Font(file = boldFile, weight = FontWeight.Bold),
                )
            }
            // Case 4: No custom font -> Default to SF Pro
            else -> SFProFontFamily
        }
    }

    companion object {
        fun resolveHindiFontFamily(context: Context, customHindiFontUri: String?): FontFamily {
            val hindiFile = customHindiFontUri?.let { getCachedFontFile(context, it) }
            return if (hindiFile != null) {
                FontFamily(
                    Font(file = hindiFile, weight = FontWeight.Normal),
                    Font(file = hindiFile, weight = FontWeight.Bold),
                )
            } else {
                PoppinsFontFamily
            }
        }

        fun getCachedFontFile(context: Context, uriString: String): File? {
            return try {
                val uri = Uri.parse(uriString)
                val tempFile = File(context.cacheDir, "font_${uriString.hashCode().toUInt()}.ttf")
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0L) {
                    // Validate with Typeface to ensure file is not corrupt
                    Typeface.createFromFile(tempFile)
                    tempFile
                } else {
                    null
                }
            } catch (_: Throwable) {
                null
            }
        }

        fun getFileNameFromUri(context: Context, uriString: String?): String? {
            if (uriString.isNullOrBlank()) return null
            return try {
                val uri = Uri.parse(uriString)
                var name: String? = null
                if (uri.scheme == "content") {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                name = cursor.getString(nameIndex)
                            }
                        }
                    }
                }
                name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Custom Font"
            } catch (_: Throwable) {
                "Custom Font"
            }
        }
    }
}
