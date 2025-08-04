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
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat.startForegroundService
import java.io.ByteArrayOutputStream
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

fun connectToPC(ip: String, port: Int, publicKey: String ,onResult: (String) -> Unit, onConnected : (Socket)->Unit) {
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
                soTimeout = 0  // 5-second read timeout
                connect(InetSocketAddress(ip, port))  // 5-second connection timeout
            }
            val message = "HELLO|$challengeHex\n"


                val out = socket.getOutputStream()
                val input = socket.getInputStream()

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
            if (isValid) {
                onConnected(socket)
            } else {
                socket.close()
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

fun startListening(context: Context, socket: Socket, onMessage: (String) -> Unit) {
    try {
        onMessage("hello world")

        val input = socket.getInputStream()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        while (true) {
            val buffer = ByteArrayOutputStream()
            val temp = ByteArray(1024)


            while (true) {
                val bytesRead = input.read(temp)
                if (bytesRead == -1) break // connection closed by server
                buffer.write(temp, 0, bytesRead)
                if (temp[bytesRead - 1] == 0.toByte()) break
            }
            var message = buffer.toByteArray()

            message = decodeData(message)
            onMessage(message.toString(Charsets.UTF_8))
            lastClipboardTextRec = message.toString(Charsets.UTF_8)
            val clip = ClipData.newPlainText("", message.toString(Charsets.UTF_8))
            clipboard.setPrimaryClip(clip)
            Thread.sleep(500)

        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun startSending(context: Context, socket: Socket, onSent: (String) -> Unit) {
    startForegroundService(Intent(context, ClipboardService::class.java))
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val output = socket.getOutputStream()
    val clip1 = clipboard.primaryClip
    if (clip1 != null && clip1.itemCount > 0)
        lastClipboardText = clip1.getItemAt(0).text?.toString() ?: ""
    try {

        while (true) {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val curr = clip.getItemAt(0).text?.toString() ?: ""

                val shouldSend = curr.isNotEmpty() &&
                        curr != lastClipboardText &&
                        curr != lastClipboardTextRec

                if (shouldSend) {
                    println(curr)

                    val message = curr.toByteArray(Charsets.UTF_8)
                    val formatted = formatMessage(message, 'T'.code.toByte(), "txt")
                    val encoded = encodeData(formatted)


                    output.write(encoded)
                    output.flush()

                    onSent(curr)

                    lastClipboardText = curr
                }
            }

            Thread.sleep(500)
        }

    } catch (e: Exception) {
        println("Error or client disconnected: $e")
    } finally {
        println("Closing connection...")
        socket.close()
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

    val trimmedData = data.copyOfRange(0,data.size-1)



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
    val payload = decoded.toByteArray().copyOfRange(8,decoded.size)

    return payload
}


fun formatMessage(message: ByteArray, msgType: Byte, format: String): ByteArray {
    val header = packMessageHeader(msgType, format.toByteArray(), message.size)
    return header + message
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

fun unpackMessageHeader(header: ByteArray): Triple<Byte, String, Int> {
    require(header.size == 8) { "Header must be exactly 8 bytes" }

    val buffer = ByteBuffer.wrap(header)
    buffer.order(ByteOrder.BIG_ENDIAN)

    val msgType = buffer.get()       // c
    val fmt = ByteArray(3) { buffer.get() }.toString(Charsets.UTF_8) // 3s
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

    var receivedMessage by remember {mutableStateOf("")}
    var sentMessage by remember {mutableStateOf("")}

    var connectedSocket by remember {mutableStateOf<Socket?>(null)}

    val mainHandler = Handler(Looper.getMainLooper())


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

                        connectToPC(ipAddress, port.toInt(), publicKey,
                            onResult = { handshakeResult = it},
                            onConnected = {socket -> connectedSocket = socket}
                        )
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

                if (receivedMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("📨 Latest Message:")
                    Text(receivedMessage)
                }
                if (sentMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("📨 Latest Message:")
                    Text(sentMessage)
                }



            }
        }
        LaunchedEffect(connectedSocket) {
            connectedSocket?.let { socket ->
                thread(start = true) {
                    // Thread 1: Listen for incoming data
                    startListening(context, socket) { msg ->
                        mainHandler.post {
                            receivedMessage = msg
                        }
                    }
                }
                thread(start = true) {
                    startSending(context, socket) { msg ->
                        mainHandler.post {
                            sentMessage = msg
                        }

                    }
                }
            }
        }

    }
}
