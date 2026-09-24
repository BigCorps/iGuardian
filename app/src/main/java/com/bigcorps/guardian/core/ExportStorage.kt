package com.bigcorps.guardian.core

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException

class ExportStorage(private val context: Context) {

    data class SavedFile(
        val uri: Uri,
        val bytes: Long,
        val locationLabel: String
    )

    fun supportsDirectDownloads(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun saveToDownloads(filename: String, contents: String): SavedFile {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw UnsupportedOperationException("MediaStore.Downloads requires Android 10+")
        }

        val expected = contents.toByteArray(Charsets.UTF_8)
        if (expected.isEmpty()) throw IOException("generated_payload_empty")

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/iGuardian"
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IOException("downloads_insert_failed")

        try {
            val output = resolver.openOutputStream(uri, "w")
                ?: throw IOException("downloads_output_stream_unavailable")

            output.use { stream ->
                stream.write(expected)
                stream.flush()
            }

            // Do not trust provider metadata alone. Read the destination back and
            // compare byte-for-byte before claiming that export succeeded.
            val actual = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("downloads_readback_unavailable")

            if (!actual.contentEquals(expected)) {
                throw IOException(
                    "downloads_readback_mismatch_expected_${expected.size}_actual_${actual.size}"
                )
            }

            val publish = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, publish, null, null)

            return SavedFile(
                uri = uri,
                bytes = actual.size.toLong(),
                locationLabel = "Downloads/iGuardian/$filename"
            )
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
    }

    fun writeAndVerifyDocument(uri: Uri, contents: String): Long {
        val expected = contents.toByteArray(Charsets.UTF_8)
        if (expected.isEmpty()) throw IOException("generated_payload_empty")

        val resolver = context.contentResolver
        val output = resolver.openOutputStream(uri, "w")
            ?: throw IOException("document_output_stream_unavailable")

        output.use { stream ->
            stream.write(expected)
            stream.flush()
        }

        val actual = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("document_readback_unavailable")

        if (!actual.contentEquals(expected)) {
            throw IOException(
                "document_readback_mismatch_expected_${expected.size}_actual_${actual.size}"
            )
        }

        return actual.size.toLong()
    }
}
