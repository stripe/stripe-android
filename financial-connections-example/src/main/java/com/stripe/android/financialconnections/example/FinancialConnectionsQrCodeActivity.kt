package com.stripe.android.financialconnections.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class FinancialConnectionsQrCodeActivity : AppCompatActivity() {
    companion object {
        fun create(context: Context, settingsUri: String): Intent {
            return Intent(context, FinancialConnectionsQrCodeActivity::class.java).apply {
                putExtra("settingsUri", settingsUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsUri = intent.getStringExtra("settingsUri")
        if (settingsUri == null) {
            finish()
            return
        }

        setContent {
            FinancialConnectionsExampleTheme {
                QrCodeScreen(settingsUri)
            }
        }
    }

    @Composable
    private fun QrCodeScreen(settingsUri: String) {
        var bitmap: Bitmap? by remember { mutableStateOf(null) }

        LaunchedEffect(settingsUri) {
            launch(Dispatchers.IO) {
                bitmap = getQrCodeBitmap(settingsUri)
            }
        }

        val localBitmap = bitmap
        if (localBitmap != null) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = localBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = settingsUri,
                    color = MaterialTheme.colors.secondaryVariant
                )
            }
        }
    }

    private fun getQrCodeBitmap(uri: String): Bitmap {
        val size = QR_CODE_SIZE
        val bits = QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    it.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }
}

private const val QR_CODE_SIZE = 512 // Pixels.
