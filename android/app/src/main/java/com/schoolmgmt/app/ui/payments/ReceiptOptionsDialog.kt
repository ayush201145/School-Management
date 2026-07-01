package com.schoolmgmt.app.ui.payments

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.schoolmgmt.app.data.repository.ReceiptItem
import com.schoolmgmt.app.utils.ReceiptPdfHelper
import com.schoolmgmt.app.utils.ReceiptPrintHelper
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun ReceiptOptionsDialog(
    receiptNo: String,
    studentName: String,
    admissionNo: String,
    className: String,
    items: List<ReceiptItem>,
    paidAmount: Double,
    mode: String,
    remainingDues: Double,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var printersList by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var printerDropdownExpanded by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }

    val dateStr = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())

    // Permission launcher for Android 12+ Bluetooth permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        if (granted) {
            printersList = ReceiptPrintHelper.getPairedPrinters(context)
            if (printersList.isNotEmpty()) {
                printerDropdownExpanded = true
            } else {
                Toast.makeText(context, "No paired Bluetooth printers found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Bluetooth permissions are required for thermal printing", Toast.LENGTH_LONG).show()
        }
    }

    fun handlePrintAction() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            printersList = ReceiptPrintHelper.getPairedPrinters(context)
            if (printersList.isNotEmpty()) {
                printerDropdownExpanded = true
            } else {
                Toast.makeText(context, "No paired Bluetooth printers found. Please pair device in settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun triggerBluetoothPrint(device: BluetoothDevice) {
        isPrinting = true
        coroutineScope.launch {
            val bitmap = ReceiptPrintHelper.drawReceiptBitmap(
                context = context,
                receiptNo = receiptNo,
                dateStr = dateStr,
                studentName = studentName,
                admissionNo = admissionNo,
                className = className,
                items = items,
                paidAmount = paidAmount,
                mode = mode,
                remainingDues = remainingDues
            )
            val result = ReceiptPrintHelper.printBitmap(device, bitmap)
            isPrinting = false
            if (result.isSuccess) {
                Toast.makeText(context, "Receipt printed successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to print: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50)
                )
                Text("Payment Recorded!")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "The payment of ₹${"%.2f".format(paidAmount)} has been registered. Select a receipt option below:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Share PDF Option Button
                OutlinedButton(
                    onClick = {
                        ReceiptPdfHelper.generateAndShareInvoicePdf(
                            context = context,
                            receiptNo = receiptNo,
                            dateStr = dateStr,
                            studentName = studentName,
                            admissionNo = admissionNo,
                            className = className,
                            items = items,
                            paidAmount = paidAmount,
                            mode = mode,
                            remainingDues = remainingDues
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
                    Text("Share PDF Receipt")
                }

                // Bluetooth Print Option Button
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { handlePrintAction() },
                        enabled = !isPrinting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.Print, contentDescription = "Print")
                        Text(if (isPrinting) "Printing..." else "Print Thermal Receipt")
                    }

                    // Dropdown menu showing paired bluetooth printers
                    DropdownMenu(
                        expanded = printerDropdownExpanded,
                        onDismissRequest = { printerDropdownExpanded = false }
                    ) {
                        printersList.forEach { device ->
                            DropdownMenuItem(
                                text = { Text(device.name ?: device.address) },
                                onClick = {
                                    printerDropdownExpanded = false
                                    triggerBluetoothPrint(device)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
