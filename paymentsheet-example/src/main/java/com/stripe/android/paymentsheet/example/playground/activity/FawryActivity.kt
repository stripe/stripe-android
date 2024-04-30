package com.stripe.android.paymentsheet.example.playground.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodResult
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

class FawryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getStringExtra(EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE) != "external_fawry") {
            finishWithResult(
                ExternalPaymentMethodResult.Failed,
                errorMessage = "Received invalid external payment method type. Expected external_fawry."
            )
        }

        setContent {
            PaymentSheetExampleTheme {
                Column {
                    ResultButton(result = ExternalPaymentMethodResult.Completed)
                    ResultButton(result = ExternalPaymentMethodResult.Canceled)
                    ResultButton(result = ExternalPaymentMethodResult.Failed)
                }
            }
        }
    }

    private fun finishWithResult(result: ExternalPaymentMethodResult, errorMessage: String?) {
        val resultCode = result.resultCode
        var data: Intent? = null
        if (result is ExternalPaymentMethodResult.Failed) {
            data = Intent().putExtra(ExternalPaymentMethodResult.Failed.ERROR_MESSAGE_EXTRA, errorMessage)
        }
        setResult(resultCode, data)
        finish()
    }

    @Composable
    fun ResultButton(result: ExternalPaymentMethodResult) {
        Button(onClick = { finishWithResult(result, errorMessage = "Payment Failed!") }) {
            Text(text = result.toString())
        }
    }

    companion object {
        private const val EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE = "external_payment_method_type"
    }

    object FawryConfirmHandler : ExternalPaymentMethodConfirmHandler {
        override fun createIntent(
            context: Context,
            externalPaymentMethodType: String,
        ): Intent {
            return Intent().setClass(
                context,
                FawryActivity::class.java
            ).putExtra(EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE, externalPaymentMethodType)
        }
    }
}
