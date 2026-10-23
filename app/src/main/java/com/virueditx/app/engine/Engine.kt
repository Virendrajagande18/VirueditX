package com.virueditx.app.engine

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * VirueditX conversion ENGINE — the strong backend under the clean UI.
 *
 * Rules the engine lives by:
 *  1. Every operation returns a typed [EngineResult] — never a raw exception to the UI.
 *  2. An output is VERIFIED before success: the file is re-opened, its size must be > 0,
 *     PDFs must report a page count (and match the expected count when known), images
 *     must actually decode. If verification fails, the partial file is deleted and the
 *     operation is a Failure — the success screen can never be shown falsely.
 *  3. Corrupt / encrypted / unsupported inputs become plain-language failures, not crashes.
 *  4. Work runs off the main thread, page-by-page, with bitmaps recycled — big photos
 *     and 500-page PDFs do not freeze or OOM the app.
 *  5. Operations are cancellable (coroutine cancellation is honoured and rethrown).
 *  6. Everything is on-device. Nothing is uploaded anywhere.
 */

sealed interface EngineResult<out T> {
    data class Success<T>(val value: T) : EngineResult<T>
    data class Failure(val reason: String) : EngineResult<Nothing>
}

/** An output file that has been re-opened and verified by the engine. */
data class VerifiedOutput(
    val file: File,
    val sizeBytes: Long,
    val pageCount: Int? = null,
    /** Machine-readable note, e.g. "UNDER_1MB" or "SMALLEST_POSSIBLE" for target compression. */
    val note: String = ""
) {
    val name: String get() = file.name
}

object ViruEngine {

    // ------------------------------------------------------------------ verify

    private fun verifyPdf(file: File, expectedPages: Int? = null): VerifiedOutput {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("The result file was empty, so nothing was saved.")
        }
        var fd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            if (count <= 0) throw IOException("The result PDF had no pages, so nothing was saved.")
            if (expectedPages != null && count != expectedPages) {
                throw IOException("The result PDF did not come out right, so nothing was saved. Please try again.")
            }
            return VerifiedOutput(file, file.length(), pageCount = count)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("The result PDF could not be checked, so nothing was saved.")
        } finally {
            try { renderer?.close() } catch (e: Exception) {}
            try { fd?.close() } catch (e: Exception) {}
        }
    }

    private fun verifyImage(file: File): VerifiedOutput {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("The result image was empty, so nothing was saved.")
        }
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) {
            throw IOException("The result image could not be opened, so nothing was saved.")
        }
        return VerifiedOutput(file, file.length())
    }

    private suspend fun <T> guard(block: suspend () -> T): EngineResult<T> =
        try {
            EngineResult.Success(block())
        } catch (e: CancellationException) {
            throw e // cancellation is not a failure — let it propagate
        } catch (e: Exception) {
            EngineResult.Failure(plainReason(e))
        }

    private fun plainReason(e: Exception): String {
        val msg = e.message ?: return "Something went wrong. Please try again."
        return when {
            msg.contains("password", ignoreCase = true) ->
                "This file is locked with a password, so it can't be opened. Try another file."
            msg.contains("empty", ignoreCase = true) || msg.contains("no pages", ignoreCase = true) -> msg
            msg.contains("could not be read", ignoreCase = true) ->
                "This file seems damaged or isn't really the type it says. Try another file."
            msg.contains("decoded", ignoreCase = true) ->
                "This photo type can't be opened on this phone. Try a JPG or PNG photo."
            msg.contains("does not exist", ignoreCase = true) || msg.contains("not a number", ignoreCase = true) ||
                msg.contains("not valid", ignoreCase = true) || msg.contains("Enter at least", ignoreCase = true) ||
                msg.contains("Select at least", ignoreCase = true) -> msg
            else -> msg.ifBlank { "Something went wrong. Please try again." }
        }
    }

    private fun failCleanup(vararg files: File?) {
        files.forEach { f ->
            try {
                if (f != null && f.exists() && f.length() == 0L) f.delete()
            } catch (e: Exception) {
                // cleanup is best-effort
            }
        }
    }

    // ------------------------------------------------------------------ operations

    suspend fun imagesToPdf(
        context: Context,
        images: List<Uri>,
        rotations: List<Int>,
        options: ImagesToPdfOptions,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): EngineResult<VerifiedOutput> = withContext(Dispatchers.Default) {
        guard {
            val r = Converters.imagesToPdf(context, images, rotations, options, onProgress)
            try {
                verifyPdf(r.file, expectedPages = images.size)
            } catch (e: Exception) {
                r.file.delete()
                throw e
            }
        }
    }

    suspend fun pdfToImages(
        context: Context,
        pdfUri: Uri,
        format: ImageFormat,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): EngineResult<List<VerifiedOutput>> = withContext(Dispatchers.Default) {
        guard {
            val results = Converters.pdfToImages(context, pdfUri, format, onProgress)
            try {
                results.map { verifyImage(it.file) }
            } catch (e: Exception) {
                results.forEach { it.file.delete() }
                throw e
            }
        }
    }

    suspend fun mergePdfs(context: Context, pdfUris: List<Uri>, outputName: String): EngineResult<VerifiedOutput> =
        withContext(Dispatchers.Default) {
            guard {
                val r = Converters.mergePdfs(context, pdfUris, outputName)
                try {
                    verifyPdf(r.file)
                } catch (e: Exception) {
                    r.file.delete(); throw e
                }
            }
        }

    suspend fun extractPages(
        context: Context,
        pdfUri: Uri,
        rangeSpec: String,
        outputName: String
    ): EngineResult<VerifiedOutput> = withContext(Dispatchers.Default) {
        guard {
            val r = Converters.extractPages(context, pdfUri, rangeSpec, outputName)
            try {
                verifyPdf(r.file)
            } catch (e: Exception) {
                r.file.delete(); throw e
            }
        }
    }

    suspend fun compressPdf(context: Context, pdfUri: Uri, outputName: String): EngineResult<VerifiedOutput> =
        withContext(Dispatchers.Default) {
            guard {
                val r = Converters.compressPdf(context, pdfUri, outputName)
                try {
                    verifyPdf(r.file)
                } catch (e: Exception) {
                    r.file.delete(); throw e
                }
            }
        }

    /** "Make it under 1 MB" for PDFs. Note = UNDER_1MB or SMALLEST_POSSIBLE — always honest. */
    suspend fun compressPdfUnder1Mb(context: Context, pdfUri: Uri, outputName: String): EngineResult<VerifiedOutput> =
        withContext(Dispatchers.Default) {
            guard {
                val r = compressPdfToTarget(context, pdfUri, outputName, SizeTargets.ONE_MB_BYTES)
                val v = try {
                    verifyPdf(r.result.file)
                } catch (e: Exception) {
                    r.result.file.delete(); throw e
                }
                v.copy(note = if (r.reachedTarget) "UNDER_1MB" else "SMALLEST_POSSIBLE")
            }
        }

    /** "Make it under 1 MB" for a photo (saved as JPG). Note = UNDER_1MB or SMALLEST_POSSIBLE. */
    suspend fun compressImageUnder1Mb(context: Context, imageUri: Uri): EngineResult<VerifiedOutput> =
        withContext(Dispatchers.Default) {
            guard {
                val r = compressImageToTarget(context, imageUri, SizeTargets.ONE_MB_BYTES)
                val v = try {
                    verifyImage(r.result.file)
                } catch (e: Exception) {
                    r.result.file.delete(); throw e
                }
                v.copy(note = if (r.reachedTarget) "UNDER_1MB" else "SMALLEST_POSSIBLE")
            }
        }

    suspend fun convertImage(context: Context, imageUri: Uri, format: ImageFormat): EngineResult<VerifiedOutput> =
        withContext(Dispatchers.Default) {
            guard {
                val r = Converters.convertImage(context, imageUri, format)
                try {
                    verifyImage(r.file)
                } catch (e: Exception) {
                    r.file.delete(); throw e
                }
            }
        }

    suspend fun txtToPdf(context: Context, txtUri: Uri, outputName: String): EngineResult<VerifiedOutput> =
        withContext(Dispatchers.Default) {
            guard {
                val r = Converters.txtToPdf(context, txtUri, outputName)
                try {
                    verifyPdf(r.file)
                } catch (e: Exception) {
                    r.file.delete(); throw e
                }
            }
        }

    suspend fun pdfPageCount(context: Context, pdfUri: Uri): EngineResult<Int> =
        guard { Converters.pdfPageCount(context, pdfUri) }

    suspend fun renderPdfPage(context: Context, file: File, pageIndex: Int): EngineResult<android.graphics.Bitmap> =
        guard { Converters.renderPdfPage(context, file, pageIndex) }
}
