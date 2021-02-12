package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.Logger
import com.stripe.android.PaymentAuthUpiAppViewStarter
import java.nio.charset.Charset

class PaymentAuthUpiAppViewActivity : AppCompatActivity() {

    private val _args: PaymentAuthUpiAppViewStarter.Args? by lazy {
        intent.getParcelableExtra(PaymentAuthUpiAppViewStarter.EXTRA_ARGS)
    }

    private val logger: Logger by lazy {
        Logger.getInstance(_args?.enableLogging == true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = _args
        if (args == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        logger.debug("PaymentAuthUpiAppViewActivity#onCreate()")

        val clientSecret = args.clientSecret

        if (clientSecret.isBlank()) {
            logger.debug("PaymentAuthAppViewActivity#onCreate() - clientSecret is blank")
            finish()
            return
        }

        logger.debug("PaymentAuthUpiAppViewActivity#onCreate() - PaymentAuthUpiAppView init and loadUrl")

        val nativeData = args.nativeData
        val decodedNativeData = Base64.decode(nativeData,0)
        val nativeDataString = decodedNativeData.toString(Charset.forName("utf-8"))

        val upiPayIntent = Intent(Intent.ACTION_VIEW)
        upiPayIntent.data = Uri.parse(nativeDataString)

        val chooser = Intent.createChooser(upiPayIntent, "Pay with")

        runCatching {
            startActivityForResult(chooser, REQUEST_CODE)
        }.onFailure {
            // handle failure
        }
    }

    private fun cancelIntentSource() {
        finish()
    }

    companion object {
        const val REQUEST_CODE: Int = 3442
    }
}

