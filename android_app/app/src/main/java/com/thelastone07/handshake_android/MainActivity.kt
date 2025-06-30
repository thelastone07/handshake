package com.thelastone07.handshake_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.thelastone07.handshake_android.ui.theme.Handshake_androidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Ask for permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                123
            )
        }

        setContent {
            Handshake_androidTheme {
                QRScannerUI()
            }
        }
    }
}

@Composable
fun QRScannerUI() {
    val context = LocalContext.current
    var showScanner by remember { mutableStateOf(false) }
    var scannedData by remember { mutableStateOf("") }

    var publicKey by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }

    if (showScanner) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val scannerView = CodeScannerView(ctx)
                val codeScanner = CodeScanner(ctx, scannerView)

                codeScanner.decodeCallback = DecodeCallback { result ->
                    val parts = result.text.split("|")
                    if (parts.size == 3) {
                        publicKey = parts[0].trim()
                        ipAddress = parts[1].trim()
                        port = parts[2].trim()
                        scannedData = result.text
                    } else {
                        Toast.makeText(ctx, "Invalid QR format", Toast.LENGTH_SHORT).show()
                    }

                    (ctx as ComponentActivity).runOnUiThread {
                        showScanner = false // Close camera
                    }
                }

                scannerView.setOnClickListener { codeScanner.startPreview() }
                codeScanner.startPreview()
                scannerView
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { showScanner = true }) {
                Text("Connect")
            }

            if (scannedData.isNotEmpty()) {
                Text("✅ Scanned Successfully:")
                Text("Public Key: $publicKey")
                Text("IP: $ipAddress")
                Text("Port: $port")
            }
        }
    }
}
