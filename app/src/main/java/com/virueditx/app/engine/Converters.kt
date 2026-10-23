package com.virueditx.app.engine

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** A finished, verified output file. Success UI is only shown with one of these in hand. */
data class OutputResult(
    val file: File,
    val pageCount: Int? = null,
    val extraCount: Int? = null // e.g. number of images written by PDF -> Images
) {
    val name: String get() = file.name
    val sizeBytes: Long get() = file.length()
}

enum class PdfPageSize(val widthPt: Int, val heightPt: Int, val label: String) {
    A4(595, 842, "A4"),
    LETTER(612, 792, "Letter"),
    FIT(0, 0, "Fit to Image")
}

enum class PdfOrientation(val label: String) { AUTO("Auto"), PORTRAIT("Portrait"), LANDSCAPE("Landscape") }

data class ImagesToPdfOptions(
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val orientation: PdfOrientation = PdfOrientation.AUTO,
    val marginPt: Int = 36,
    val quality: Int = 90, // 40..100
    val outputName: String = "VirueditX"
)

enum class ImageFormat(val ext: String, val label: String) {
    JPG("jpg", "JPG"), PNG("png", "PNG"), WEBP("webp", "WEBP"), BMP("bmp", "BMP")
}

object Converters {

    // ------------------------------------------------------------------ helpers

    fun outputsDir(context: Context): File =
        File(context.filesDir, "outputs").apply { mkdirs() }

    fun uniqueFile(dir: File, baseName: String, ext: String): File {
        val clean = baseName.trim().ifBlank { "VirueditX" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        var candidate = File(dir, "$clean.$ext")
        var n = 1
        while (candidate.exists()) {
            candidate = File(dir, "$clean ($n).$ext")
            n++
        }
        return candidate
    }

    fun displayName(context: Context, uri: Uri): String {
        var name: String? = null
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        } catch (e: Exception) {
            // fall through to path segment
        } finally {
            cursor?.close()
        }
        return name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    }

    suspend fun copyUriToCache(context: Context, uri: Uri, prefix: String): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "inputs").apply { mkdirs() }
            val safe = displayName(context, uri).replace(Regex("[^A-Za-z0-9._-]"), "_")
            val out = File(dir, "${prefix}_$safe")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            } ?: throw IOException("Could not read the selected file.")
            if (out.length() == 0L) throw IOException("The selected file is empty or unreadable.")
            out
        }

    /** Decode a bitmap from a Uri, downsampled to [maxDim] px on the long edge (OOM safety), EXIF-rotated. */
    suspend fun decodeBitmap(context: Context, uri: Uri, maxDim: Int = 2048, applyRotation: Int = 0): Bitmap =
        withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("Could not read the image.")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("This image format could not be decoded on this device (HEIC needs Android 9+).")
            }
            var sample = 1
            while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                ?: throw IOException("This image could not be decoded on this device.")

            // Honour EXIF orientation where present.
            var rotate = applyRotation
            try {
                val exif = ExifInterface(ByteArrayInputStream(bytes))
                val o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                rotate += when (o) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } catch (e: Exception) {
                // EXIF is optional; ignore failures.
            }
            if (rotate % 360 != 0) {
                val m = Matrix().apply { postRotate((rotate % 360).toFloat()) }
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                if (rotated !== bmp) {
                    bmp.recycle()
                    bmp = rotated
                }
            }
            bmp
        }

    private fun requireRealFile(file: File): File {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("Conversion produced an empty file. Nothing was saved.")
        }
        return file
    }

    private fun friendlyPdfError(e: Exception): Exception = when {
        e is SecurityException || (e.message ?: "").contains("password", ignoreCase = true) ->
            IOException("This PDF is password-protected. VirueditX cannot open encrypted PDFs.")
        else -> IOException("This PDF could not be read. It may be corrupted or in an unsupported variant.")
    }

    // ------------------------------------------------------------- Images -> PDF

    suspend fun imagesToPdf(
        context: Context,
        images: List<Uri>,
        rotations: List<Int>,
        options: ImagesToPdfOptions,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): OutputResult = withContext(Dispatchers.Default) {
        if (images.isEmpty()) throw IOException("Select at least one image first.")
        val outFile = uniqueFile(outputsDir(context), options.outputName, "pdf")
        val doc = PdfDocument()
        try {
            images.forEachIndexed { index, uri ->
                currentCoroutineContext().ensureActive()
                val bmp = decodeBitmap(context, uri, maxDim = 2048, applyRotation = rotations.getOrElse(index) { 0 })
                val working = if (options.quality < 100) {
                    // Apply the chosen quality honestly: JPEG round-trip at the requested quality.
                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, options.quality.coerceIn(40, 100), baos)
                    val re = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size()) ?: bmp
                    if (re !== bmp) bmp.recycle()
                    re
                } else bmp

                val pageW: Int
                val pageH: Int
                when (options.pageSize) {
                    PdfPageSize.FIT -> {
                        pageW = working.width.coerceAtMost(2480)
                        pageH = working.height.coerceAtMost(3508)
                    }
                    else -> {
                        var w = options.pageSize.widthPt
                        var h = options.pageSize.heightPt
                        val landscape = when (options.orientation) {
                            PdfOrientation.LANDSCAPE -> true
                            PdfOrientation.PORTRAIT -> false
                            PdfOrientation.AUTO -> working.width > working.height
                        }
                        if (landscape && h > w) {
                            val t = w; w = h; h = t
                        }
                        pageW = w; pageH = h
                    }
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                val margin = if (options.pageSize == PdfPageSize.FIT) 0 else options.marginPt
                val availW = (pageW - 2 * margin).toFloat()
                val availH = (pageH - 2 * margin).toFloat()
                val scale = minOf(availW / working.width, availH / working.height)
                val drawW = working.width * scale
                val drawH = working.height * scale
                val left = (pageW - drawW) / 2f
                val top = (pageH - drawH) / 2f
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(working, null, RectF(left, top, left + drawW, top + drawH), paint)
                doc.finishPage(page)
                working.recycle()
                onProgress(index + 1, images.size)
            }
            FileOutputStream(outFile).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        OutputResult(requireRealFile(outFile), pageCount = images.size)
    }

    // ------------------------------------------------------------- PDF -> Images

    suspend fun pdfPageCount(context: Context, pdfUri: Uri): Int = withContext(Dispatchers.IO) {
        val tmp = copyUriToCache(context, pdfUri, "count")
        var renderer: PdfRenderer? = null
        var fd: ParcelFileDescriptor? = null
        try {
            fd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)
            renderer.pageCount
        } catch (e: Exception) {
            throw friendlyPdfError(e)
        } finally {
            try { renderer?.close() } catch (e: Exception) {}
            try { fd?.close() } catch (e: Exception) {}
            tmp.delete()
        }
    }

    /** Render one PDF page to a bitmap (used by the PDF preview screen too). */
    suspend fun renderPdfPage(context: Context, pdfFile: File, pageIndex: Int, maxDim: Int = 1400): Bitmap =
        withContext(Dispatchers.IO) {
            var renderer: PdfRenderer? = null
            var fd: ParcelFileDescriptor? = null
            try {
                fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(fd)
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    throw IOException("That page does not exist in this PDF.")
                }
                val page = renderer.openPage(pageIndex)
                val scale = maxDim.toFloat() / maxOf(page.width, page.height)
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Canvas(bmp).drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bmp
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                throw friendlyPdfError(e)
            } finally {
                try { renderer?.close() } catch (e: Exception) {}
                try { fd?.close() } catch (e: Exception) {}
            }
        }

    suspend fun pdfToImages(
        context: Context,
        pdfUri: Uri,
        format: ImageFormat,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<OutputResult> = withContext(Dispatchers.Default) {
        val tmp = copyUriToCache(context, pdfUri, "pdf2img")
        val base = displayName(context, pdfUri).substringBeforeLast('.').ifBlank { "pdf" }
        val results = mutableListOf<OutputResult>()
        var renderer: PdfRenderer? = null
        var fd: ParcelFileDescriptor? = null
        try {
            fd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            if (count == 0) throw IOException("This PDF has no pages.")
            for (i in 0 until count) {
                currentCoroutineContext().ensureActive()
                val page = renderer.openPage(i)
                val scale = 2000f / maxOf(page.width, page.height)
                val bmp = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                Canvas(bmp).drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                val out = uniqueFile(outputsDir(context), "${base}_page${i + 1}", format.ext)
                saveBitmap(bmp, format, out)
                bmp.recycle()
                results.add(OutputResult(requireRealFile(out)))
                onProgress(i + 1, count)
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw friendlyPdfError(e)
        } finally {
            try { renderer?.close() } catch (e: Exception) {}
            try { fd?.close() } catch (e: Exception) {}
            tmp.delete()
        }
        results
    }

    // ------------------------------------------------------------- Merge / Split / Compress (PDFBox)

    suspend fun mergePdfs(context: Context, pdfUris: List<Uri>, outputName: String): OutputResult =
        withContext(Dispatchers.Default) {
            if (pdfUris.size < 2) throw IOException("Select at least two PDFs to merge.")
            val files = pdfUris.mapIndexed { i, uri -> copyUriToCache(context, uri, "merge$i") }
            val outFile = uniqueFile(outputsDir(context), outputName.ifBlank { "Merged" }, "pdf")
            try {
                val merger = PDFMergerUtility()
                merger.destinationFileName = outFile.absolutePath
                files.forEach { merger.addSource(it) }
                merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
            } catch (e: Exception) {
                outFile.delete()
                throw friendlyPdfError(e)
            } finally {
                files.forEach { it.delete() }
            }
            val pages = try {
                PDDocument.load(outFile).use { it.numberOfPages }
            } catch (e: Exception) {
                null
            }
            OutputResult(requireRealFile(outFile), pageCount = pages)
        }

    /** Parse "1,3-5,8" (1-based, inclusive) into sorted 0-based page indices. */
    fun parsePageRanges(spec: String, totalPages: Int): List<Int> {
        val result = sortedSetOf<Int>()
        spec.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
            if (part.contains('-')) {
                val bits = part.split('-')
                if (bits.size != 2) throw IOException("Page range \"$part\" is not valid.")
                val from = bits[0].trim().toIntOrNull() ?: throw IOException("Page range \"$part\" is not valid.")
                val to = bits[1].trim().toIntOrNull() ?: throw IOException("Page range \"$part\" is not valid.")
                if (from > to) throw IOException("Page range \"$part\" goes backwards.")
                for (p in from..to) result.add(p - 1)
            } else {
                val p = part.toIntOrNull() ?: throw IOException("Page \"$part\" is not a number.")
                result.add(p - 1)
            }
        }
        if (result.isEmpty()) throw IOException("Enter at least one page, e.g. 1,3-5.")
        val bad = result.firstOrNull { it < 0 || it >= totalPages }
        if (bad != null) throw IOException("Page ${bad + 1} does not exist — this PDF has $totalPages pages.")
        return result.toList()
    }

    suspend fun extractPages(context: Context, pdfUri: Uri, rangeSpec: String, outputName: String): OutputResult =
        withContext(Dispatchers.Default) {
            val src = copyUriToCache(context, pdfUri, "split")
            val outFile = uniqueFile(outputsDir(context), outputName.ifBlank { "Extracted" }, "pdf")
            try {
                PDDocument.load(src).use { doc ->
                    val indices = parsePageRanges(rangeSpec, doc.numberOfPages)
                    PDDocument().use { out ->
                        indices.forEach { i -> out.addPage(doc.getPage(i)) }
                        out.save(outFile.absolutePath)
                    }
                }
            } catch (e: IOException) {
                outFile.delete()
                throw e
            } catch (e: Exception) {
                outFile.delete()
                throw friendlyPdfError(e)
            } finally {
                src.delete()
            }
            OutputResult(requireRealFile(outFile))
        }

    /**
     * Honest best-effort compression: re-saves the document stripped of metadata and
     * document-info baggage with PDFBox's optimised writer. The result screen shows
     * both sizes so the user sees the real effect — it is never faked.
     */
    suspend fun compressPdf(context: Context, pdfUri: Uri, outputName: String): OutputResult =
        withContext(Dispatchers.Default) {
            val src = copyUriToCache(context, pdfUri, "compress")
            val outFile = uniqueFile(outputsDir(context), outputName.ifBlank { "Compressed" }, "pdf")
            try {
                PDDocument.load(src).use { doc ->
                    doc.isAllSecurityToBeRemoved = true
                    doc.documentInformation = PDDocumentInformation()
                    try {
                        doc.documentCatalog.metadata = null
                    } catch (e: Exception) {
                        // Some PDFs carry no metadata stream; fine.
                    }
                    doc.save(outFile.absolutePath)
                }
            } catch (e: Exception) {
                outFile.delete()
                throw friendlyPdfError(e)
            } finally {
                src.delete()
            }
            OutputResult(requireRealFile(outFile))
        }

    // ------------------------------------------------------------- TXT -> PDF

    suspend fun txtToPdf(context: Context, txtUri: Uri, outputName: String): OutputResult =
        withContext(Dispatchers.Default) {
            val raw = context.contentResolver.openInputStream(txtUri)?.bufferedReader()?.readText()
                ?: throw IOException("Could not read the text file.")
            val text = raw.take(200_000)
            val outFile = uniqueFile(outputsDir(context), outputName.ifBlank { "Text" }, "pdf")
            val pageW = 595
            val pageH = 842
            val margin = 48f
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            // Simple word-wrap by measured width.
            val maxWidth = pageW - 2 * margin
            val lines = mutableListOf<String>()
            text.split('\n').forEach { paragraph ->
                if (paragraph.isEmpty()) {
                    lines.add("")
                    return@forEach
                }
                var current = ""
                for (word in paragraph.split(' ')) {
                    val trial = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(trial) > maxWidth && current.isNotEmpty()) {
                        lines.add(current)
                        current = word
                    } else {
                        current = trial
                    }
                }
                lines.add(current)
            }
            val lineHeight = 17f
            val linesPerPage = ((pageH - 2 * margin) / lineHeight).toInt().coerceAtLeast(1)
            val doc = PdfDocument()
            try {
                var pageNo = 0
                var i = 0
                while (i < lines.size) {
                    val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNo + 1).create()
                    val page = doc.startPage(info)
                    var y = margin + 12f
                    var used = 0
                    while (i < lines.size && used < linesPerPage) {
                        page.canvas.drawText(lines[i], margin, y, paint)
                        y += lineHeight
                        i++
                        used++
                    }
                    doc.finishPage(page)
                    pageNo++
                }
                if (pageNo == 0) {
                    val info = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
                    val page = doc.startPage(info)
                    doc.finishPage(page)
                }
                FileOutputStream(outFile).use { doc.writeTo(it) }
            } finally {
                doc.close()
            }
            OutputResult(requireRealFile(outFile))
        }

    // ------------------------------------------------------------- Image Converter

    suspend fun convertImage(context: Context, imageUri: Uri, format: ImageFormat): OutputResult =
        withContext(Dispatchers.Default) {
            val bmp = decodeBitmap(context, imageUri, maxDim = 4096)
            val base = displayName(context, imageUri).substringBeforeLast('.').ifBlank { "image" }
            val out = uniqueFile(outputsDir(context), base + "_converted", format.ext)
            try {
                saveBitmap(bmp, format, out)
            } finally {
                bmp.recycle()
            }
            OutputResult(requireRealFile(out))
        }

    fun saveBitmap(bmp: Bitmap, format: ImageFormat, out: File) {
        FileOutputStream(out).use { stream ->
            when (format) {
                ImageFormat.JPG -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                ImageFormat.PNG -> bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                ImageFormat.WEBP -> bmp.compress(Bitmap.CompressFormat.WEBP, 90, stream)
                ImageFormat.BMP -> BmpEncoder.write(bmp, stream)
            }
        }
    }
}

/** Minimal 24-bit BMP writer (Android has no built-in BMP encoder). */
object BmpEncoder {
    fun write(bmp: Bitmap, out: java.io.OutputStream) {
        val width = bmp.width
        val height = bmp.height
        val rowBytes = ((width * 3 + 3) / 4) * 4
        val pixelDataSize = rowBytes * height
        val fileSize = 54 + pixelDataSize
        val header = ByteArray(54)
        fun putShort(offset: Int, v: Int) {
            header[offset] = (v and 0xFF).toByte()
            header[offset + 1] = ((v shr 8) and 0xFF).toByte()
        }
        fun putInt(offset: Int, v: Int) {
            for (i in 0 until 4) header[offset + i] = ((v shr (8 * i)) and 0xFF).toByte()
        }
        header[0] = 'B'.code.toByte()
        header[1] = 'M'.code.toByte()
        putInt(2, fileSize)
        putInt(10, 54)          // pixel data offset
        putInt(14, 40)          // BITMAPINFOHEADER size
        putInt(18, width)
        putInt(22, -height)     // negative = top-down rows
        putShort(26, 1)         // planes
        putShort(28, 24)        // bits per pixel
        putInt(34, pixelDataSize)
        out.write(header)
        val pixels = IntArray(width)
        val row = ByteArray(rowBytes)
        for (y in 0 until height) {
            bmp.getPixels(pixels, 0, width, 0, y, width, 1)
            for (x in 0 until width) {
                val c = pixels[x]
                row[x * 3] = (c and 0xFF).toByte()               // B
                row[x * 3 + 1] = ((c shr 8) and 0xFF).toByte()   // G
                row[x * 3 + 2] = ((c shr 16) and 0xFF).toByte()  // R
            }
            out.write(row)
        }
        out.flush()
    }
}

// ------------------------------------------------- "Under 1 MB" target compression

/** Result of a target-size compression. [reachedTarget] is always the honest truth. */
data class TargetCompressResult(
    val result: OutputResult,
    val reachedTarget: Boolean,
    val targetBytes: Long
)

object SizeTargets {
    /** Strict portals count 1 MB as 1,000,000 bytes — VirueditX targets that, to be safe. */
    const val ONE_MB_BYTES: Long = 1_000_000L
}


private fun requireRealFileTop(file: File): File {
    if (!file.exists() || file.length() == 0L) throw IOException("Conversion produced an empty file. Nothing was saved.")
    return file
}

private fun friendlyPdfErrorTop(e: Exception): Exception = when {
    e is SecurityException || (e.message ?: "").contains("password", ignoreCase = true) ->
        IOException("This PDF is password-protected. VirueditX cannot open encrypted PDFs.")
    else -> IOException("This PDF could not be read. It may be corrupted or in an unsupported variant.")
}

suspend fun compressPdfToTarget(
    context: Context,
    pdfUri: Uri,
    outputName: String,
    targetBytes: Long = SizeTargets.ONE_MB_BYTES
): TargetCompressResult = withContext(Dispatchers.Default) {
    val src = Converters.copyUriToCache(context, pdfUri, "target")
    val outDir = Converters.outputsDir(context)
    // Already small enough? Just save a copy and say so honestly.
    if (src.length() in 1..targetBytes) {
        val done = Converters.uniqueFile(outDir, outputName.ifBlank { "Under1MB" }, "pdf")
        src.copyTo(done, overwrite = true)
        src.delete()
        return@withContext TargetCompressResult(OutputResult(requireRealFileTop(done)), true, targetBytes)
    }
    var best: File? = null

    fun consider(candidate: File): File {
        val current = best
        if (current == null || candidate.length() < current.length()) {
            if (current != null && current != candidate) current.delete()
            best = candidate
            return candidate
        }
        if (candidate != best) candidate.delete()
        return best!!
    }

    try {
        // Pass 0: clean re-save (metadata stripped) — enough for many PDFs.
        val pass0 = Converters.uniqueFile(outDir, outputName.ifBlank { "Under1MB" }, "pdf")
        try {
            PDDocument.load(src).use { doc ->
                doc.isAllSecurityToBeRemoved = true
                doc.documentInformation = PDDocumentInformation()
                try { doc.documentCatalog.metadata = null } catch (e: Exception) {}
                doc.save(pass0.absolutePath)
            }
            consider(pass0)
        } catch (e: Exception) {
            pass0.delete()
            throw friendlyPdfErrorTop(e)
        }
        if (best!!.length() <= targetBytes) {
            return@withContext TargetCompressResult(OutputResult(requireRealFileTop(best!!)), true, targetBytes)
        }

        // Passes 1..n: shrink every embedded photo further each round, then re-save.
        val scales = listOf(0.85f, 0.7f, 0.55f, 0.42f, 0.32f, 0.24f)
        var working = src
        for (scale in scales) {
            val passFile = Converters.uniqueFile(outDir, outputName.ifBlank { "Under1MB" }, "pdf")
            val changed = try {
                shrinkPdfImages(working, passFile, scale)
            } catch (e: Exception) {
                passFile.delete()
                false
            }
            if (!changed) break // no embedded images to shrink — cannot do more on-device
            consider(passFile)
            if (best!!.length() <= targetBytes) break
            working = best!!
        }
    } finally {
        src.delete()
    }
    val finalFile = requireRealFileTop(best ?: throw IOException("Compression failed."))
    TargetCompressResult(OutputResult(finalFile), finalFile.length() <= targetBytes, targetBytes)
}

/** Re-encode every embedded image in [input] at [scale] of its size as JPEG, saving to [output]. Returns true if any image was shrunk. */
private fun shrinkPdfImages(input: File, output: File, scale: Float): Boolean {
    var changed = false
    PDDocument.load(input).use { doc ->
        for (page in doc.pages) {
            val resources = page.resources ?: continue
            for (name in resources.xObjectNames) {
                val xobj = try { resources.getXObject(name) } catch (e: Exception) { null } ?: continue
                if (xobj is com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                    try {
                        val original: Bitmap = xobj.image ?: continue
                        val newW = (original.width * scale).toInt().coerceAtLeast(16)
                        val newH = (original.height * scale).toInt().coerceAtLeast(16)
                        if (newW >= original.width && bestAlreadySmall(original)) continue
                        val scaled = Bitmap.createScaledBitmap(original, newW, newH, true)
                        val replacement = com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromImage(doc, scaled, 0.72f)
                        resources.put(name, replacement)
                        if (scaled !== original) scaled.recycle()
                        original.recycle()
                        changed = true
                    } catch (e: Exception) {
                        // One bad image must not sink the whole pass.
                    }
                }
            }
        }
        doc.save(output.absolutePath)
    }
    return changed
}

private fun bestAlreadySmall(bmp: Bitmap): Boolean = bmp.width <= 64 || bmp.height <= 64

suspend fun compressImageToTarget(
    context: Context,
    imageUri: Uri,
    targetBytes: Long = SizeTargets.ONE_MB_BYTES
): TargetCompressResult = withContext(Dispatchers.Default) {
    var bmp = Converters.decodeBitmap(context, imageUri, maxDim = 4096)
    val base = Converters.displayName(context, imageUri).substringBeforeLast('.').ifBlank { "photo" }
    val outDir = Converters.outputsDir(context)
    var bestFile: File? = null

    fun trySave(candidate: Bitmap, quality: Int): File {
        val f = Converters.uniqueFile(outDir, base + "_under1mb", "jpg")
        FileOutputStream(f).use { candidate.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        val current = bestFile
        if (current == null || f.length() < current.length()) {
            current?.delete()
            bestFile = f
        } else {
            f.delete()
        }
        return bestFile!!
    }

    try {
        // Step 1: lower the JPEG quality step by step at full size.
        for (q in listOf(90, 80, 70, 60, 50, 40)) {
            val best = trySave(bmp, q)
            if (best.length() <= targetBytes) break
        }
        // Step 2: if still too big, make the photo smaller and try again.
        var guard = 0
        while ((bestFile?.length() ?: Long.MAX_VALUE) > targetBytes && guard < 8) {
            guard++
            val newW = (bmp.width * 0.8f).toInt()
            val newH = (bmp.height * 0.8f).toInt()
            if (newW < 320 || newH < 320) break
            val scaled = Bitmap.createScaledBitmap(bmp, newW, newH, true)
            if (scaled !== bmp) {
                bmp.recycle()
                bmp = scaled
            }
            for (q in listOf(80, 65, 50)) {
                val best = trySave(bmp, q)
                if (best.length() <= targetBytes) break
            }
        }
    } finally {
        bmp.recycle()
    }
    val finalFile = requireRealFileTop(bestFile ?: throw IOException("Compression failed."))
    TargetCompressResult(OutputResult(finalFile), finalFile.length() <= targetBytes, targetBytes)
}
