package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.stripe.android.Logger
import com.stripe.android.PaymentAuthUpiAppViewStarter
import com.stripe.android.R
import com.stripe.android.payments.PaymentFlowResult
import java.nio.charset.Charset

class PaymentAuthUpiAppViewActivity : AppCompatActivity() {

    private val _args: PaymentAuthUpiAppViewStarter.Args? by lazy {
        intent.getParcelableExtra(PaymentAuthUpiAppViewStarter.EXTRA_ARGS)
    }

    private val logger: Logger by lazy {
        Logger.getInstance(_args?.enableLogging == true)
    }

    private val viewModel: UpiAuthActivityViewModel by viewModels()

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

        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret
                ).toBundle()
            )
        )

        logger.debug("PaymentAuthUpiAppViewActivity#onCreate() - PaymentAuthUpiAppView init and loadUrl")

        val upiPayIntent = Intent(Intent.ACTION_VIEW)
        upiPayIntent.data = Uri.parse(viewModel.decode(args.nativeData))

        val chooser = Intent.createChooser(upiPayIntent, getString(R.string.upi_app_pay_with))
        runCatching {
            startActivityForResult(chooser, REQUEST_CODE)
        }.onFailure {
            // handle failure
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
    }

    private fun cancelIntentSource() {
        finish()
    }

    companion object {
        const val REQUEST_CODE: Int = 3442
    }

    internal class UpiAuthActivityViewModel : ViewModel() {
        fun decode(nativeData: String): String {
            val decodedNativeData = Base64.decode(nativeData, 0)
            return decodedNativeData.toString(Charset.forName("utf-8"))
        }
    }
}
