package com.example.claims.android.scan

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import java.io.File
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** On-device document scanner: edge detection, deskew, crop — returns a cropped JPEG page. */
fun buildDocumentScanner(): GmsDocumentScanner =
    GmsDocumentScanning.getClient(
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    )

/** Formats the import picker accepts: every image type, plus PDF. */
val IMPORT_MIME_TYPES = arrayOf("image/*", "application/pdf")

/**
 * OCR result plus the URI to preview/upload for this document. For images that's the source URI;
 * for a PDF it's a rendered JPEG of the first page (so preview and upload are always an image).
 */
data class ScannedDoc(val text: String, val imageUri: Uri)

/**
 * On-device OCR of a scanned or imported document — nothing leaves the phone. Handles both:
 *  - **any image format** the platform can decode (JPEG/PNG/WEBP/HEIC/BMP/GIF…), via [InputImage]; and
 *  - **PDF**, which ML Kit cannot read directly, by rasterizing each page with [PdfRenderer] and OCR-ing it.
 *
 * The document scanner's own gallery import is images-only, so PDFs arrive here from the file picker.
 * Runs off the main thread (PdfRenderer is synchronous).
 */
suspend fun ocrDocument(context: Context, uri: Uri): ScannedDoc = withContext(Dispatchers.IO) {
    val mime = context.contentResolver.getType(uri)
    val isPdf = mime == "application/pdf" || uri.toString().substringBefore('?').endsWith(".pdf", ignoreCase = true)
    if (isPdf) ocrPdf(context, uri) else ScannedDoc(recognize(InputImage.fromFilePath(context, uri)), uri)
}

/**
 * Rasterize every PDF page (2× for legibility, on a white background) and OCR each; join the text.
 * The first page is also saved as a JPEG so it can back the on-screen preview and the upload.
 */
private suspend fun ocrPdf(context: Context, uri: Uri): ScannedDoc {
    val out = StringBuilder()
    var imageUri = uri // fallback if rendering yields no pages
    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val scale = 2
                    val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                    // PDF pages can be transparent; paint white first so dark text has contrast.
                    Canvas(bmp).drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    try {
                        if (i == 0) imageUri = cacheJpeg(context, bmp)
                        val text = recognize(InputImage.fromBitmap(bmp, 0))
                        if (text.isNotBlank()) out.append(text).append('\n')
                    } finally {
                        bmp.recycle()
                    }
                }
            }
        }
    }
    return ScannedDoc(out.toString(), imageUri)
}

/** Write a bitmap to a private cache JPEG and return its file URI (readable by Coil and the uploader). */
private fun cacheJpeg(context: Context, bmp: Bitmap): Uri {
    val file = File(context.cacheDir, "import_${System.nanoTime()}.jpg")
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    return Uri.fromFile(file)
}

/** Suspend wrapper over ML Kit's Task-based Latin text recognizer. */
private suspend fun recognize(image: InputImage): String = suspendCancellableCoroutine { cont ->
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(image)
        .addOnSuccessListener { cont.resume(it.text) }
        .addOnFailureListener { cont.resumeWithException(it) }
}

/** Unwrap the Activity from a Compose LocalContext (needed to start the scanner intent). */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
