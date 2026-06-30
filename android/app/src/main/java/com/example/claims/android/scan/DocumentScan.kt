package com.example.claims.android.scan

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

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

/** On-device Latin text recognition over a scanned page. Nothing leaves the phone. */
fun runOcr(context: Context, uri: Uri, onText: (String) -> Unit, onError: (Exception) -> Unit) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { onText(it.text) }
            .addOnFailureListener { onError(it) }
    } catch (e: Exception) {
        onError(e)
    }
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
