package com.example.claims.android.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device validation of the real OCR pipeline: PdfRenderer rasterization + ML Kit text
 * recognition + ClaimParser. Renders its own PNG and multi-page PDF so it needs no fixtures,
 * backend, or login. The deterministic parser is unit-tested separately; here we confirm the
 * Android-only plumbing actually produces text and (for PDFs) a JPEG preview image.
 */
@RunWith(AndroidJUnit4::class)
class OcrPipelineTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun textPaint(size: Float) = Paint().apply {
        color = Color.BLACK
        textSize = size
        isAntiAlias = true
    }

    /** Draw the given lines onto a white bitmap. */
    private fun renderBitmap(lines: List<String>, w: Int = 1000, h: Int = 1400): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val paint = textPaint(44f)
        var y = 90f
        for (line in lines) { c.drawText(line, 60f, y, paint); y += 80f }
        return bmp
    }

    private fun writePng(name: String, lines: List<String>): Uri {
        val f = File(ctx.cacheDir, name)
        f.outputStream().use { renderBitmap(lines).compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(f)
    }

    private fun writePdf(name: String, pages: List<List<String>>): Uri {
        val f = File(ctx.cacheDir, name)
        val doc = PdfDocument()
        pages.forEachIndexed { i, lines ->
            val info = PdfDocument.PageInfo.Builder(595, 842, i + 1).create()
            val page = doc.startPage(info)
            val c = page.canvas
            c.drawColor(Color.WHITE)
            val paint = textPaint(22f)
            var y = 60f
            for (line in lines) { c.drawText(line, 40f, y, paint); y += 40f }
            doc.finishPage(page)
        }
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
        return Uri.fromFile(f)
    }

    @Test
    fun png_synthetic_labelled_form_is_recognized_and_parsed() = runBlocking {
        val uri = writePng(
            "synthetic.png",
            listOf(
                "Claim ID: TEST-0007",
                "Patient: Alpha Tester",
                "Member ID: MBR-TEST-0007",
                "Payer: Synthetic Health Plan",
                "Billed: 250.00",
                "Paid: 200.00",
                "Status: submitted"
            )
        )

        val doc = ocrDocument(ctx, uri)
        assertTrue("OCR returned no text", doc.text.isNotBlank())

        val form = ClaimParser.parse(doc.text)
        // OCR of clean rendered text should fill the labelled fields.
        assertTrue("claimId='${form.claimId}'", form.claimId.replace(" ", "").contains("TEST", ignoreCase = true))
        assertTrue("patientName blank", form.patientName.isNotBlank())
        assertTrue("payer='${form.payer}'", form.payer.contains("Health", ignoreCase = true))
        assertTrue("billed='${form.billedAmount}'", form.billedAmount.toDoubleOrNull()?.let { it in 240.0..260.0 } == true)
        // For an image, the preview/upload URI is the source image itself.
        assertTrue("imageUri=${doc.imageUri}", doc.imageUri == uri)
    }

    @Test
    fun pdf_multipage_is_rasterized_ocrd_and_first_page_saved_as_jpeg() = runBlocking {
        val uri = writePdf(
            "statement.pdf",
            listOf(
                listOf(
                    "HOSPITAL STATEMENT",
                    "PATIENT NAME",
                    "JANE DOE",
                    "TOTAL CHARGES  \$654.80",
                    "BALANCE:  \$419.07"
                ),
                listOf("Page two continuation", "Remit to PO BOX 9125")
            )
        )

        val doc = ocrDocument(ctx, uri)
        assertTrue("OCR returned no text", doc.text.isNotBlank())
        // Text from BOTH pages should be present (multi-page OCR).
        assertTrue("missing page1 text: ${doc.text}", doc.text.contains("BALANCE", ignoreCase = true))
        assertTrue("missing page2 text: ${doc.text}", doc.text.contains("continuation", ignoreCase = true))

        val form = ClaimParser.parse(doc.text)
        assertTrue("patientName='${form.patientName}'", form.patientName.isNotBlank())
        assertTrue("balance='${form.balance}'", form.balance.toDoubleOrNull()?.let { it in 418.0..420.0 } == true)

        // The preview/upload URI must be a rendered JPEG (not the PDF), and the file must exist.
        val path = doc.imageUri.path ?: ""
        assertTrue("imageUri not a jpg: ${doc.imageUri}", path.endsWith(".jpg"))
        assertTrue("rendered jpeg missing", File(path).exists() && File(path).length() > 0)
    }
}
