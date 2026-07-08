package com.vishwajitrajput.musetraceai.core.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.vishwajitrajput.musetraceai.core.errors.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val imagesDir: File
        get() = File(context.filesDir, "images").also { it.mkdirs() }

    private val cacheImagesDir: File
        get() = File(context.cacheDir, "images").also { it.mkdirs() }

    private val projectExportsDir: File
        get() = File(context.filesDir, "project_exports").also { it.mkdirs() }

    fun createCameraImageUri(): Uri {
        val file = File(cacheImagesDir, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun saveBitmap(prefix: String, bitmap: Bitmap): String {
        val file = File(imagesDir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return Uri.fromFile(file).toString()
    }

    fun copyIntoImageStorage(prefix: String, uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val source = Uri.parse(uriString)
        val extension = source.lastPathSegment
            ?.substringAfterLast('.', "")
            ?.takeIf { it.length in 1..5 }
            ?: "png"
        val file = File(imagesDir, "${prefix}_${System.currentTimeMillis()}.$extension")
        return try {
            openInput(uriString)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            } ?: return uriString
            Uri.fromFile(file).toString()
        } catch (_: Throwable) {
            uriString
        }
    }

    fun saveProjectJson(projectId: Long, title: String, json: String): String {
        val file = createProjectExportFile(projectId, title, "json")
        file.writeText(json)
        return fileUri(file)
    }

    fun createProjectExportFile(projectId: Long, title: String, extension: String): File {
        val safeTitle = title
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "musetrace-project" }
        return File(projectExportsDir, "${safeTitle}_${projectId}_${System.currentTimeMillis()}.$extension")
    }

    fun fileUri(file: File): String = Uri.fromFile(file).toString()

    fun openInputStream(uriString: String): InputStream? = openInput(uriString)

    fun writeImageFile(prefix: String, extension: String, input: InputStream): String {
        val safeExtension = extension.lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,5}")) } ?: "bin"
        val file = File(imagesDir, "${prefix}_${System.currentTimeMillis()}.$safeExtension")
        FileOutputStream(file).use { output -> input.copyTo(output) }
        return Uri.fromFile(file).toString()
    }

    fun readText(uriString: String): String {
        val uri = Uri.parse(uriString)
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE -> {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            null -> File(uriString).takeIf { it.exists() }?.readText()
            else -> uri.path?.let { File(it).takeIf { file -> file.exists() }?.readText() }
        } ?: throw AppError.ImageLoad("MuseTrace could not read the selected project file.")
    }

    fun deleteStoredFile(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val uri = Uri.parse(uriString)
        if (uri.scheme != ContentResolver.SCHEME_FILE && uri.scheme != null) return
        val file = File(uri.path ?: uriString)
        val imagesRoot = imagesDir.canonicalFile
        val exportsRoot = projectExportsDir.canonicalFile
        val target = runCatching { file.canonicalFile }.getOrNull() ?: return
        if (target.path.startsWith(imagesRoot.path) || target.path.startsWith(exportsRoot.path)) {
            target.delete()
        }
    }

    fun loadBitmapSampled(uriString: String, maxSide: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decodeUri(uriString, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return loadBitmap(uriString).scaleToMax(maxSide)
        }
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
        }
        val decoded = decodeUri(uriString, options)
            ?: throw AppError.ImageLoad("MuseTrace could not decode the selected image.")
        return decoded.scaleToMax(maxSide).copy(Bitmap.Config.ARGB_8888, false)
    }

    fun loadBitmap(uriString: String): Bitmap {
        val uri = Uri.parse(uriString)
        return try {
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE -> {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                null -> BitmapFactory.decodeFile(uriString)
                else -> BitmapFactory.decodeFile(uri.path)
            } ?: throw AppError.ImageLoad("MuseTrace could not decode the selected image.")
        } catch (throwable: Throwable) {
            if (throwable is AppError.ImageLoad) throw throwable
            throw AppError.ImageLoad("MuseTrace could not open the selected image.", throwable)
        }
    }

    private fun decodeUri(uriString: String, options: BitmapFactory.Options): Bitmap? {
        return when (Uri.parse(uriString).scheme) {
            ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE -> {
                openInput(uriString)?.use { BitmapFactory.decodeStream(it, null, options) }
            }
            null -> BitmapFactory.decodeFile(uriString, options)
            else -> BitmapFactory.decodeFile(Uri.parse(uriString).path, options)
        }
    }

    private fun openInput(uriString: String): InputStream? {
        val uri = Uri.parse(uriString)
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE -> context.contentResolver.openInputStream(uri)
            null -> File(uriString).takeIf { it.exists() }?.inputStream()
            else -> uri.path?.let { File(it).takeIf { file -> file.exists() }?.inputStream() }
        }
    }

    private fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
        var sampleSize = 1
        var sampledWidth = width
        var sampledHeight = height
        while (sampledWidth / 2 >= maxSide || sampledHeight / 2 >= maxSide) {
            sampleSize *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private fun Bitmap.scaleToMax(maxSide: Int): Bitmap {
        val largest = width.coerceAtLeast(height)
        if (largest <= maxSide) return this
        val ratio = maxSide.toFloat() / largest.toFloat()
        val scaledWidth = (width * ratio).toInt().coerceAtLeast(1)
        val scaledHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    }
}
