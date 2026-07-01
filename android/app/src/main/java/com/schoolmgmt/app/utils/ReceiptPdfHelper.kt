package com.schoolmgmt.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.repository.ReceiptItem
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

object ReceiptPdfHelper {

    fun generateAndShareInvoicePdf(
        context: Context,
        receiptNo: String,
        dateStr: String,
        studentName: String,
        admissionNo: String,
        className: String,
        items: List<ReceiptItem>,
        paidAmount: Double,
        mode: String,
        remainingDues: Double,
    ) {
        // Fetch invoice settings from local Room database
        val settings = runBlocking {
            try {
                AppDatabase.getInstance(context).invoiceSettingsDao().getSettings()
            } catch (e: Exception) {
                null
            }
        }

        val schoolName = settings?.schoolName ?: "ABC Public School"
        val address = settings?.address
        val phone = settings?.phone
        val email = settings?.email
        val footerNote = settings?.footerNote ?: "Thank you for your payment!"
        val margin = settings?.marginSize ?: 20
        val headerSize = settings?.headerFontSize ?: 28
        val bodySize = settings?.bodyFontSize ?: 14

        val pdfDocument = PdfDocument()
        
        // Dynamic page height based on items list length to prevent overflow
        val dynamicHeight = 700 + (items.size * 30)
        val pageInfo = PdfDocument.PageInfo.Builder(595, maxOf(842, dynamicHeight), 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = AndroidColor.BLACK
            isAntiAlias = true
        }

        var y = margin.toFloat() + 40f
        val width = 595

        // School Title
        paint.apply {
            textSize = headerSize.toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(schoolName.uppercase(), (width / 2).toFloat(), y, paint)

        // Address & Contacts (if available)
        val subHeaderLines = mutableListOf<String>()
        if (!address.isNullOrBlank()) subHeaderLines.add(address)
        val contactLine = listOfNotNull(
            phone?.let { "Phone: $it" },
            email?.let { "Email: $it" }
        ).joinToString(" | ")
        if (contactLine.isNotBlank()) subHeaderLines.add(contactLine)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = (bodySize - 2).toFloat().coerceAtLeast(10f)
        subHeaderLines.forEach { line ->
            y += 20f
            canvas.drawText(line, (width / 2).toFloat(), y, paint)
        }

        y += 25f
        paint.textSize = (bodySize + 4).toFloat()
        canvas.drawText("OFFICIAL PAYMENT RECEIPT", (width / 2).toFloat(), y, paint)

        y += 20f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = bodySize.toFloat()
        canvas.drawText("-".repeat(78), margin.toFloat() * 1.5f, y, paint)

        // Metadata block
        y += 25f
        canvas.drawText("Receipt No: $receiptNo", margin.toFloat() * 2f, y, paint)
        canvas.drawText("Date: $dateStr", width - margin.toFloat() * 10f, y, paint)

        y += 22f
        canvas.drawText("Student Name: $studentName", margin.toFloat() * 2f, y, paint)
        canvas.drawText("Admission No: $admissionNo", width - margin.toFloat() * 10f, y, paint)

        y += 22f
        canvas.drawText("Class: $className", margin.toFloat() * 2f, y, paint)

        y += 20f
        canvas.drawText("-".repeat(78), margin.toFloat() * 1.5f, y, paint)

        // Columns header
        y += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Particulars", margin.toFloat() * 2f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount", width - margin.toFloat() * 2f, y, paint)

        y += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("-".repeat(78), margin.toFloat() * 1.5f, y, paint)

        // Items list breakdown
        items.forEach { item ->
            y += 25f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(item.description, margin.toFloat() * 2f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("₹${"%.2f".format(item.amount)}", width - margin.toFloat() * 2f, y, paint)
        }

        y += 20f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("-".repeat(78), margin.toFloat() * 1.5f, y, paint)

        // Totals summary
        y += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Paid (${mode}):", margin.toFloat() * 2f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${"%.2f".format(paidAmount)}", width - margin.toFloat() * 2f, y, paint)

        y += 25f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Outstanding Dues Balance:", margin.toFloat() * 2f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${"%.2f".format(remainingDues)}", width - margin.toFloat() * 2f, y, paint)

        y += 20f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("-".repeat(78), margin.toFloat() * 1.5f, y, paint)

        // Footer note
        y += 40f
        paint.apply {
            textSize = (bodySize - 1).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This is an official cloud-synced receipt generated by the school app.", (width / 2).toFloat(), y, paint)
        y += 20f
        canvas.drawText(footerNote, (width / 2).toFloat(), y, paint)

        pdfDocument.finishPage(page)

        // Save PDF to cache
        val pdfFile = File(context.cacheDir, "receipt_${receiptNo}.pdf")
        try {
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        // Trigger Android Share Intent
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing receipt PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
