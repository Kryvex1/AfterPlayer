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
        return SFProMediumFontFamily
    }

    companion object {
        fun resolveHindiFontFamily(context: Context, customHindiFontUri: String? = null): FontFamily {
            return KohinoorDevanagariFontFamily
        }

        fun resolveEnglishFontFamily(isBold: Boolean = false): FontFamily {
            return if (isBold) SFProBoldFontFamily else SFProMediumFontFamily
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
