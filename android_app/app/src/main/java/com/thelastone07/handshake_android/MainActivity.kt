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

import java.net.Socket
import java.security.SecureRandom
import kotlin.concurrent.thread
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.security.PublicKey

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Ask for camera permission
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


fun connectToPC(ip: String, port: Int, publicKey: String ,onResult: (String) -> Unit) {
    //run in background
    thread {
        try {
            // Generate 32-byte random challenge
            val challenge = ByteArray(32).apply {
                SecureRandom().nextBytes(this)
            }
            val challengeHex = challenge.joinToString("") { "%02x".format(it) }

            // Create socket with timeout
            val socket = Socket().apply {
                soTimeout = 10000  // 5-second read timeout
                connect(InetSocketAddress(ip, port), 5000)  // 5-second connection timeout
            }
            val message = "HELLO|$challengeHex\n"

            socket.use {  // Auto-close resource
                val out = it.getOutputStream()
                val input = it.getInputStream()

                // Send challenge with explicit encoding

                out.write(message.toByteArray(Charsets.UTF_8))
                out.flush()



                // Read 64-byte signature with guaranteed length
                val signatureBytes = ByteArray(64).apply {
                    var bytesRead = 0
                    while (bytesRead < 64) {
                        val read = input.read(this, bytesRead, 64 - bytesRead)
                        if (read == -1) throw IOException("Unexpected end of stream")
                        bytesRead += read
                    }
                }
                val hexSignature = signatureBytes.joinToString("") { "%02x".format(it) }
                val publicKeyBytes = hexToBytes(publicKey)
//                val publicKeyBytes = publicKey.toByteArray(Charsets.UTF_8)
                val isValid = verifySignature(publicKeyBytes, challenge, signatureBytes)
//                 Return success only after full verification
                onResult("isValid $isValid")
            }
        } catch (e: SocketTimeoutException) {
            onResult("Timeout error: ${e.message}")
        } catch (e: IOException) {
            onResult("Network error: ${e.message}")
        } catch (e: Exception) {
            onResult("Unexpected error: ${e.message}")
        }
    }
}

fun hexToBytes(hex: String): ByteArray {
    return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun verifySignature(publicKeyBytes : ByteArray, message : ByteArray, signatureBytes: ByteArray): Boolean {
    val publicKey = Ed25519PublicKeyParameters(publicKeyBytes, 0)
    val verifier = Ed25519Signer()

    verifier.init(false,publicKey)
    verifier.update(message, 0, message.size)

    return verifier.verifySignature(signatureBytes)
}

@Composable
fun QRScannerUI() {
    val context = LocalContext.current
    var showScanner by remember { mutableStateOf(false) }
    var scannedData by remember { mutableStateOf("") }

    var publicKey by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }

    var handshakeResult by remember { mutableStateOf("") }

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

                        connectToPC(ipAddress, port.toInt(), publicKey) {
                            handshakeResult = it
                        }
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

                if (handshakeResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("🔐 Handshake Result:")
                    Text(handshakeResult)
                }

            }
        }
    }
}
