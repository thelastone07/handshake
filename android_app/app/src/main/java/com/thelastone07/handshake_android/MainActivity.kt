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

import java.nio.ByteBuffer
import java.nio.ByteOrder

import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import kotlin.concurrent.thread

import java.io.InputStream



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

var lastClipboardText = ""
var lastClipboardTextRec = ""

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
                val result = if (isValid) "Successful" else "Unsuccessful"

                onResult("Handshake $result ✅")
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

fun startListening(context: Context, ipAddress: String, port: Int) {
    try {
        val socket = Socket().apply {
            soTimeout = 0 // keep socket open indefinitely
            connect(InetSocketAddress(ipAddress, port))
        }

        val input = socket.getInputStream()

        for (packet in readPackets(input)) {
            try {
                val decoded = decodeData(packet)
                if (decoded.size < 8) continue // Invalid header size

                val header = decoded.copyOfRange(0, 8)
                val payload = decoded.copyOfRange(8, decoded.size)

                val (msgType, fmt, size) = unpackMessageHeader(header)

                if (msgType == 'T'.code.toByte() && fmt.contentEquals("txt".toByteArray())) {
                    val message = payload.toString(Charsets.UTF_8)
                    lastClipboardTextRec = message

                    // Update clipboard
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Received Text", message)
                    clipboard.setPrimaryClip(clip)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}


fun startSending(context: Context,ipAddress: String, port: Int) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    try {
        val socket = Socket().apply {
            soTimeout = 0
            connect(InetSocketAddress(ipAddress, port))
        }
        val output = socket.getOutputStream()

        while (true) {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""

                if (text.isNotEmpty() &&
                    text != lastClipboardText &&
                    text != lastClipboardTextRec
                ) {
                    val dataBytes = text.toByteArray(Charsets.UTF_8)
                    val header = packMessageHeader('T'.code.toByte(), "txt".toByteArray(), dataBytes.size)
                    val fullMessage = header + dataBytes
                    val encoded = encodeData(fullMessage)

                    output.write(encoded)
                    output.flush()

                    lastClipboardText = text
                }
            }

            Thread.sleep(500) // Sleep before next check
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun readPackets(input : InputStream, stopByte: Byte = 0x00, bufferSize: Int = 1024): Sequence<ByteArray> = sequence {
    val buffer = ByteArray(bufferSize)
    var data = ByteArray(0)

    while (true) {
        val bytesRead = input.read(buffer)
        if (bytesRead == -1) break

        data += buffer.copyOfRange(0, bytesRead)

        var stopIndex = data.indexOf(stopByte)
        while (stopIndex != -1) {
            val packet = data.copyOfRange(0, stopIndex)
            data = data.copyOfRange(stopIndex + 1, data.size)

            if (packet.isNotEmpty()) {
                yield(packet)
            }

            stopIndex = data.indexOf(stopByte)
        }
    }
}


fun encodeData(data: ByteArray): ByteArray {
    val encoded = mutableListOf<Byte>()
    var j = 0

    while (j < data.size) {
        val codeIndex = encoded.size
        var codeLen: Int = 1
        encoded.add(0) // Placeholder for code length

        while (j < data.size && data[j] != 0.toByte() && codeLen < 255) {
            encoded.add(data[j])
            j++
            codeLen++
        }

        encoded[codeIndex] = codeLen.toByte()

        if (j < data.size && data[j] == 0.toByte()) {
            j++ // Skip the zero
        }
    }

    encoded.add(0) // Final zero
    return encoded.toByteArray()
}


fun decodeData(data: ByteArray): ByteArray {
    val decoded = mutableListOf<Byte>()
    var j = 0

    // Exclude the last trailing zero
    val trimmedData = data.copyOf(data.size - 1)

    while (j < trimmedData.size) {
        val codeLen = trimmedData[j].toInt() and 0xFF // Unsigned byte
        j++

        for (i in 1 until codeLen) {
            if (j < trimmedData.size) {
                decoded.add(trimmedData[j])
                j++
            }
        }

        if (codeLen < 255 && j < trimmedData.size) {
            decoded.add(0)
        }
    }

    return decoded.toByteArray()
}

fun packMessageHeader(msgType: Byte, fmt: ByteArray, dataLength: Int): ByteArray {
    require(fmt.size == 3) { "Format must be 3 bytes" }

    val buffer = ByteBuffer.allocate(8) // 1 + 3 + 4 = 8 bytes
    buffer.order(ByteOrder.BIG_ENDIAN)
    buffer.put(msgType)       // c
    buffer.put(fmt)           // 3s
    buffer.putInt(dataLength) // I
    return buffer.array()
}

fun unpackMessageHeader(header: ByteArray): Triple<Byte, ByteArray, Int> {
    require(header.size == 8) { "Header must be exactly 8 bytes" }

    val buffer = ByteBuffer.wrap(header)
    buffer.order(ByteOrder.BIG_ENDIAN)

    val msgType = buffer.get()             // c
    val fmt = ByteArray(3) { buffer.get() } // 3s
    val dataLength = buffer.int             // I

    return Triple(msgType, fmt, dataLength)
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
        LaunchedEffect(handshakeResult) {
            if (handshakeResult == "Handshake Successful ✅") {
                thread(start = true) {
                    // Thread 1: Listen for incoming data
                    startListening(context,ipAddress, port.toInt())
                }

                thread(start = true) {
                    // Thread 2: Send outgoing data
                    startSending(context ,ipAddress, port.toInt())
                }
            }
        }
    }
}
