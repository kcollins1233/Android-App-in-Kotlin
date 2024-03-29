package com.example.project

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.loader.content.CursorLoader
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class FileUtils<IOException : Any> @Inject constructor(private val context: Context) {

    private var selection: String? = null
    private var selectionArgs: Array<String>? = null

    fun getFile(uri: Uri): File? {
        val path = getPath(uri)
        return if (path != null) {
            File(path)
        } else {
            null
        }
    }

    fun getPath(uri: Uri): String? {
        // check here to KITKAT or new version
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getPathForKitKatAndAbove(uri)
        } else {
            getPathBelowKitKat(uri)
        }
    }

    @SuppressLint("NewApi")
    private fun handleExternalStorage(uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":".toRegex()).toTypedArray()
        val fullPath = getPathFromExtSD(split)
        return if (fullPath !== "") {
            fullPath
        } else {
            null
        }
    }

    @SuppressLint("NewApi")
    private fun handleDownloads(uri: Uri): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            handleDownloads23ApiAndAbove(uri)
        } else {
            handleDownloadsBelow23Api(uri)
        }
        return null
    }

    @SuppressLint("NewApi")
    private fun handleDownloadsBelow23Api(uri: Uri): String? {
        val id = DocumentsContract.getDocumentId(uri)
        if (id.startsWith("raw:")) {
            return id.replaceFirst("raw:".toRegex(), "")
        }
        var contentUri: Uri? = null
        try {
            contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), id.toLong()
            )
        } catch (e: NumberFormatException) {
            Log.e("FileUtils", "Downloads below 23 API - NumberFormatException: $e")
        }
        if (contentUri != null) {
            return getDataColumn(contentUri)
        }
        return null
    }

    @SuppressLint("NewApi")
    private fun handleDownloads23ApiAndAbove(uri: Uri): String? {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver
                .query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val fileName = cursor.getString(0)
                val path: String =
                    Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                if (path.isNotEmpty()) {
                    return path
                }
            }
        } finally {
            cursor?.close()
        }
        val id: String = DocumentsContract.getDocumentId(uri)
        if (id.isNotEmpty()) {
            if (id.startsWith("raw:")) {
                return id.replaceFirst("raw:".toRegex(), "")
            }
            val contentUriPrefixesToTry = arrayOf(
                "content://downloads/public_downloads",
                "content://downloads/my_downloads"
            )
            for (contentUriPrefix in contentUriPrefixesToTry) {
                return try {
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse(contentUriPrefix),
                        id.toLong()
                    )
                    getDataColumn(contentUri)
                } catch (e: NumberFormatException) {
                    //In Android 8 and Android P the id is not a number
                    uri.path.orEmpty().replaceFirst("^/document/raw:".toRegex(), "")
                        .replaceFirst("^raw:".toRegex(), "")
                }
            }
        }
        return null
    }

    @SuppressLint("NewApi")
    private fun handleMedia(uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":".toRegex()).toTypedArray()
        val contentUri: Uri? = when (split[0]) {
            "image" -> {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            "video" -> {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            "audio" -> {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            else -> null
        }
        selection = "_id=?"
        selectionArgs = arrayOf(split[1])
        return if (contentUri != null) {
            getDataColumn(contentUri)
        } else {
            null
        }
    }

    private fun handleContentScheme(uri: Uri): String? {
        if (isGooglePhotosUri(uri)) {
            return uri.lastPathSegment
        }
        if (isGoogleDriveUri(uri)) {
            return getDriveFilePath(uri)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyFileToInternalStorage(uri, "userfiles")
        } else {
            getDataColumn(uri)
        }
    }

    @SuppressLint("NewApi")
    private fun getPathForKitKatAndAbove(uri: Uri): String? {
        return when {
            // ExternalStorageProvider
            isExternalStorageDocument(uri) -> handleExternalStorage(uri)
            // DownloadsProvider
            isDownloadsDocument(uri) -> handleDownloads(uri)
            // MediaProvider
            isMediaDocument(uri) -> handleMedia(uri)
            //GoogleDriveProvider
            isGoogleDriveUri(uri) -> getDriveFilePath(uri)
            //WhatsAppProvider
            isWhatsAppFile(uri) -> getFilePathForWhatsApp(uri)
            //ContentScheme
            "content".equals(uri.scheme, ignoreCase = true) -> handleContentScheme(uri)
            //FileScheme
            "file".equals(uri.scheme, ignoreCase = true) -> uri.path
            else -> null
        }
    }

    private fun getPathBelowKitKat(uri: Uri): String? {
        if (isWhatsAppFile(uri)) {
            return getFilePathForWhatsApp(uri)
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val projection = arrayOf(
                MediaStore.Images.Media.DATA
            )
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver
                    .query(uri, projection, selection, selectionArgs, null)
                val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        return columnIndex?.let { cursor.getString(it) }
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        return null
    }

    private fun fileExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    private fun getPathFromExtSD(pathData: Array<String>): String {
        val type = pathData[0]
        val relativePath = "/" + pathData[1]
        var fullPath: String

        // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
        // something like "71F8-2C0A", some kind of unique id per storage
        // don't know any API that can get the root path of that storage based on its id.
        //
        // so no "primary" type, but let the check here for other devices
        if ("primary".equals(type, ignoreCase = true)) {
            fullPath = Environment.getExternalStorageDirectory().toString() + relativePath
            if (fileExists(fullPath)) {
                return fullPath
            }
        }

        // Environment.isExternalStorageRemovable() is `true` for external and internal storage
        // so we cannot relay on it.
        //
        // instead, for each possible path, check if file exists
        // we'll start with secondary storage as this could be our (physically) removable sd card
        fullPath = System.getenv("SECONDARY_STORAGE").orEmpty() + relativePath
        if (fileExists(fullPath)) {
            return fullPath
        }
        fullPath = System.getenv("EXTERNAL_STORAGE").orEmpty() + relativePath
        return if (fileExists(fullPath)) {
            fullPath
        } else fullPath
    }

    private fun getDriveFilePath(uri: Uri): String? {
        context.contentResolver.query(
            uri, null, null, null, null
        )?.use { cursor ->
            /*
             * Get the column indexes of the data in the Cursor,
             *     * move to the first row in the Cursor, get the data,
             *     * and display it.
             * */
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name = cursor.getString(nameIndex)
            val file = File(context.cacheDir, name)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)!!
                val outputStream = FileOutputStream(file)
                val bytesAvailable = inputStream.available()

                val bufferSize = min(bytesAvailable, MAX_BUFFER_SIZE)
                val buffers = ByteArray(bufferSize)
                var read: Int
                while (inputStream.read(buffers).also { read = it } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                inputStream.close()
                outputStream.close()
            } finally {
                cursor.close()
            }
            return file.path
        }
        return null
    }

    /***
     * Used for Android Q+
     * @param uri
     * @param newDirName if you want to create a directory, you can set this variable
     * @return
     */
    private fun copyFileToInternalStorage(uri: Uri, newDirName: String): String? {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null
        )?.use { cursor ->
            /*
             * Get the column indexes of the data in the Cursor,
             *     * move to the first row in the Cursor, get the data,
             *     * and display it.
             * */
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name = cursor.getString(nameIndex)
            val output: File = if (newDirName != "") {
                val dir = File(context.filesDir.toString() + "/" + newDirName)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                File(context.filesDir.toString() + "/" + newDirName + "/" + name)
            } else {
                File(context.filesDir.toString() + "/" + name)
            }
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val outputStream = FileOutputStream(output)
                var read: Int
                val buffers = ByteArray(BUFFER_SIZE)
                while (inputStream.read(buffers).also { read = it } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                inputStream.close()
                outputStream.close()
            } finally {
                cursor.close()
            }
            return output.path
        }
        return null
    }

    private fun getFilePathForWhatsApp(uri: Uri): String? {
        return copyFileToInternalStorage(uri, "whatsapp")
    }

    private fun getDataColumn(uri: Uri): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(
                uri, projection,
                selection, selectionArgs, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return EXTERNAL_STORAGE_CONTENT == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return DOWNLOAD_DOCUMENT_CONTENT == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return MEDIA_DOCUMENT_CONTENT == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return GOOGLE_PHOTOS_CONTENT == uri.authority
    }

    private fun isWhatsAppFile(uri: Uri): Boolean {
        return WHATS_APP_CONTENT == uri.authority
    }

    private fun isGoogleDriveUri(uri: Uri): Boolean {
        return GOOGLE_DRIVE_CONTENT == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
    }

    companion object {
        private const val BUFFER_SIZE = 1024
        private const val MAX_BUFFER_SIZE = 1024 * 1024
        private const val GOOGLE_DRIVE_CONTENT = "com.google.android.apps.docs.storage"
        private const val WHATS_APP_CONTENT = "com.whatsapp.provider.media"
        private const val GOOGLE_PHOTOS_CONTENT = "com.google.android.apps.photos.content"
        private const val MEDIA_DOCUMENT_CONTENT = "com.android.providers.media.documents"
        private const val DOWNLOAD_DOCUMENT_CONTENT = "com.android.providers.downloads.documents"
        private const val EXTERNAL_STORAGE_CONTENT = "com.android.externalstorage.documents"
    }
}

annotation class Inject
