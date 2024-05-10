package com.stripe.android.paymentsheet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.model.PaymentMethod

/**
 * Handles external payment methods.
 *
 * This will call the merchant's [ExternalPaymentMethodConfirmHandler.confirmExternalPaymentMethod] when initially
 * created. When the merchant's implementation calls [ExternalPaymentMethodResultHandler.onExternalPaymentMethodResult],
 * it will re-start this activity with the result. This class then finishes and the class that launched this activity
 * is responsible for responding to that result.
 */
internal class ExternalPaymentMethodActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type = intent.getStringExtra(EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE)

        @Suppress("DEPRECATION")
        val billingDetails = intent.getParcelableExtra<PaymentMethod.BillingDetails>(EXTRA_BILLING_DETAILS)

        if (type != null) {
            ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler?.confirmExternalPaymentMethod(
                type,
                billingDetails ?: PaymentMethod.BillingDetails()
            )
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        @Suppress("DEPRECATION")
        val externalPaymentMethodResult: ExternalPaymentMethodResult? =
            intent.getParcelableExtra(ExternalPaymentMethodResultHandler.EXTRA_EXTERNAL_PAYMENT_METHOD_RESULT)
        externalPaymentMethodResult?.let {
            when (it) {
                is ExternalPaymentMethodResult.Completed -> setResult(ExternalPaymentMethodResult.Completed.RESULT_CODE)
                is ExternalPaymentMethodResult.Canceled -> setResult(ExternalPaymentMethodResult.Canceled.RESULT_CODE)
                is ExternalPaymentMethodResult.Failed -> {
                    val data =
                        Intent().putExtra(ExternalPaymentMethodResult.Failed.ERROR_MESSAGE_EXTRA, it.displayMessage)
                    setResult(ExternalPaymentMethodResult.Failed.RESULT_CODE, data)
                }
            }
            finish()
        }
    }

   internal companion object {
        const val EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE = "external_payment_method_type"
        const val EXTRA_BILLING_DETAILS = "external_payment_method_billing_details"
    }
}
