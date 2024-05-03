package com.stripe.android.paymentsheet.example.playground.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.stripe.android.paymentsheet.example.playground.PaymentSheetPlaygroundUrlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class QrCodeActivity : AppCompatActivity() {
    companion object {
        fun create(context: Context, settingsJson: String): Intent {
            return Intent(context, QrCodeActivity::class.java).apply {
                putExtra("settingsJson", settingsJson)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsJson = intent.getStringExtra("settingsJson")
        if (settingsJson == null) {
            finish()
            return
        }

        val uri = PaymentSheetPlaygroundUrlHelper.createUri(settingsJson)

        setContent {
            var bitmap: Bitmap? by remember { mutableStateOf(null) }

            LaunchedEffect(uri) {
                launch(Dispatchers.IO) {
                    bitmap = getQrCodeBitmap(uri)
                }
            }

            val localBitmap = bitmap
            if (localBitmap != null) {
                Image(
                    bitmap = localBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun getQrCodeBitmap(uri: Uri): Bitmap {
        val size = QR_CODE_SIZE
        val bits = QRCodeWriter().encode(uri.toString(), BarcodeFormat.QR_CODE, size, size)
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
