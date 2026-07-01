package com.schoolmgmt.app.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.repository.ReceiptItem
import kotlinx.coroutines.runBlocking
import java.io.OutputStream
import java.util.UUID

object ReceiptPrintHelper {

    // Standard SPP UUID for Bluetooth serial board communication
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return adapter.bondedDevices.filter { device ->
            val deviceClass = device.bluetoothClass?.deviceClass ?: 0
            // Include common thermal printer major classes
            device.name.contains("printer", ignoreCase = true) || 
            deviceClass == 1664 || deviceClass == 7936
        }
    }

    fun drawReceiptBitmap(
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
    ): Bitmap {
        // Fetch settings from local DB
        val settings = runBlocking {
            try {
                AppDatabase.getInstance(context).invoiceSettingsDao().getSettings()
            } catch (e: Exception) {
                null
            }
        }

        val schoolName = settings?.schoolName ?: "ABC Public School"
        val footerNote = settings?.footerNote ?: "Thank you for your payment!"
        val width = settings?.thermalWidth ?: 576
        
        // Calculate dynamic height based on the number of paid categories
        val height = 480 + (items.size * 45) + 120
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(AndroidColor.WHITE)
        
        val paint = Paint().apply {
            color = AndroidColor.BLACK
            isAntiAlias = true
        }

        var y = 40f

        // Draw School Header (Bold, Centered)
        paint.apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(schoolName.uppercase(), (width / 2).toFloat(), y, paint)
        
        y += 35f
        paint.apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("OFFICIAL PAYMENT RECEIPT", (width / 2).toFloat(), y, paint)
        
        y += 25f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("------------------------------------------------", 10f, y, paint)
        
        y += 25f
        paint.textSize = 16f
        canvas.drawText("Receipt No: $receiptNo", 20f, y, paint)
        canvas.drawText("Date: $dateStr", (width - 236).toFloat(), y, paint)
        
        y += 25f
        canvas.drawText("Student: $studentName", 20f, y, paint)
        canvas.drawText("Adm No: $admissionNo", (width - 236).toFloat(), y, paint)
        
        y += 25f
        canvas.drawText("Class: $className", 20f, y, paint)
        
        y += 25f
        canvas.drawText("------------------------------------------------", 10f, y, paint)
        
        // Columns header
        y += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Particulars", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount", (width - 26).toFloat(), y, paint)
        
        y += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("------------------------------------------------", 10f, y, paint)
        
        // Particulars list
        items.forEach { item ->
            y += 30f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(item.description, 20f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("₹${"%.2f".format(item.amount)}", (width - 26).toFloat(), y, paint)
        }
        
        y += 30f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("------------------------------------------------", 10f, y, paint)
        
        // Payment Summary
        y += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Paid (${mode}):", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${"%.2f".format(paidAmount)}", (width - 26).toFloat(), y, paint)
        
        y += 30f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Outstanding Balance:", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${"%.2f".format(remainingDues)}", (width - 26).toFloat(), y, paint)
        
        y += 30f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("------------------------------------------------", 10f, y, paint)
        
        // Footer message
        y += 45f
        paint.apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This is a computer-generated transaction receipt.", (width / 2).toFloat(), y, paint)
        y += 25f
        canvas.drawText(footerNote, (width / 2).toFloat(), y, paint)

        return bitmap
    }

    @SuppressLint("MissingPermission")
    suspend fun printBitmap(device: BluetoothDevice, bitmap: Bitmap): Result<Unit> {
        return kotlin.runCatching {
            var socket: BluetoothSocket? = null
            var out: OutputStream? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                out = socket.outputStream

                // Initialize printer (ESC @)
                out.write(byteArrayOf(0x1B, 0x40))

                // ESC/POS print raster bitmap command pattern
                val widthBytes = (bitmap.width + 7) / 8
                val height = bitmap.height
                val pixels = IntArray(bitmap.width * height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, height)

                // Header for raster print (GS v 0 m xL xH yL yH)
                val header = byteArrayOf(
                    0x1D, 0x76, 0x30, 0x00,
                    (widthBytes and 0xFF).toByte(),
                    ((widthBytes shr 8) and 0xFF).toByte(),
                    (height and 0xFF).toByte(),
                    ((height shr 8) and 0xFF).toByte()
                )
                out.write(header)

                // Convert pixels to ESC/POS binary black-and-white stream
                val buffer = ByteArray(widthBytes * height)
                for (y in 0 until height) {
                    for (x in 0 until bitmap.width) {
                        val pixel = pixels[y * bitmap.width + x]
                        val r = (pixel and 0x00FF0000) shr 16
                        val g = (pixel and 0x0000FF00) shr 8
                        val b = pixel and 0x000000FF
                        val isBlack = (r + g + b) / 3 < 128 // binary thresholding
                        if (isBlack) {
                            val byteIndex = y * widthBytes + (x / 8)
                            val bitIndex = 7 - (x % 8)
                            buffer[byteIndex] = (buffer[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                        }
                    }
                }
                out.write(buffer)

                // Feed paper and cut (ESC d 3, GS V 66 0)
                out.write(byteArrayOf(0x1B, 0x64, 0x03))
                out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))

                out.flush()
            } finally {
                out?.close()
                socket?.close()
            }
        }
    }
}
