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
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

object ReceiptPrintHelper {

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return adapter.bondedDevices.toList()
    }

    fun drawReceiptBitmap(
        schoolName: String,
        receiptNo: String,
        dateStr: String,
        studentName: String,
        admissionNo: String,
        className: String,
        particulars: String,
        paidAmount: Double,
        mode: String,
        remainingDues: Double,
    ): Bitmap {
        val width = 576
        val height = 620
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
        canvas.drawText("Date: $dateStr", 340f, y, paint)
        
        y += 25f
        canvas.drawText("Student: $studentName", 20f, y, paint)
        canvas.drawText("Adm No: $admissionNo", 340f, y, paint)
        
        y += 25f
        canvas.drawText("Class: $className", 20f, y, paint)
        
        y += 25f
        canvas.drawText("------------------------------------------------", 10f, y, paint)
        
        // Columns header
        y += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Particulars", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount", 550f, y, paint)
        
        y += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("------------------------------------------------", 10f, y, paint)
        
        // Particulars details
        y += 30f
        canvas.drawText(particulars, 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${"%.2f".format(paidAmount)}", 550f, y, paint)
        
        y += 30f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("------------------------------------------------", 10f, y, paint)
        
        // Payment Summary
        y += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Paid (${mode}):", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${"%.2f".format(paidAmount)}", 550f, y, paint)
        
        y += 30f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Outstanding Balance:", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${"%.2f".format(remainingDues)}", 550f, y, paint)
        
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
        canvas.drawText("Thank you for your payment!", (width / 2).toFloat(), y, paint)

        return bitmap
    }

    fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerLine = width / 8
        val output = ByteArrayOutputStream()

        // ESC @ (Initialize printer)
        output.write(byteArrayOf(0x1B, 0x40))

        // GS v 0 0 xL xH yL yH d1...dk
        val xL = (bytesPerLine and 0xFF).toByte()
        val xH = ((bytesPerLine shr 8) and 0xFF).toByte()
        val yL = (height and 0xFF).toByte()
        val yH = ((height shr 8) and 0xFF).toByte()

        output.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (xByte in 0 until bytesPerLine) {
                var value = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    val pixel = pixels[y * width + x]
                    val alpha = (pixel shr 24) and 0xFF
                    val red = (pixel shr 16) and 0xFF
                    val green = (pixel shr 8) and 0xFF
                    val blue = pixel and 0xFF
                    
                    val isDark = if (alpha < 128) {
                        false
                    } else {
                        val gray = (0.299 * red + 0.587 * green + 0.114 * blue)
                        gray < 160
                    }

                    if (isDark) {
                        value = value or (1 shl (7 - bit))
                    }
                }
                output.write(value)
            }
        }

        // Feed paper and cut (GS V 66 0x00)
        output.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))

        return output.toByteArray()
    }

    @SuppressLint("MissingPermission")
    suspend fun printBitmap(device: BluetoothDevice, bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            val outputStream = socket.outputStream
            val printBytes = convertBitmapToEscPos(bitmap)
            outputStream.write(printBytes)
            outputStream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                socket?.close()
            } catch (ignored: Exception) {}
        }
    }
}
