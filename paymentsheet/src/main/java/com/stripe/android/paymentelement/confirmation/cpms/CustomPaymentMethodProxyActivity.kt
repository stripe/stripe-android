package com.stripe.android.paymentelement.confirmation.cpms

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CustomPaymentMethodConfirmHandler
import com.stripe.android.paymentelement.CustomPaymentMethodResult
import com.stripe.android.paymentelement.CustomPaymentMethodResultHandler
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Handles external payment methods.
 *
 * This will call the merchant's [CustomPaymentMethodConfirmHandler.confirmCustomPaymentMethod] when initially
 * created. When the merchant's implementation calls [CustomPaymentMethodResultHandler.onCustomPaymentMethodResult],
 * it will re-start this activity with the result. This class then finishes and the class that launched this activity
 * is responsible for responding to that result.
 */
@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal class CustomPaymentMethodProxyActivity : AppCompatActivity() {

    private var hasConfirmStarted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getBoolean(HAS_CONFIRM_STARTED_KEY)?.let { hasConfirmStarted = it }

        @Suppress("DEPRECATION")
        val type = intent.getParcelableExtra<PaymentSheet.CustomPaymentMethodConfiguration.CustomPaymentMethodType>(
            EXTRA_CUSTOM_PAYMENT_METHOD_TYPE
        )

        @Suppress("DEPRECATION")
        val billingDetails = intent.getParcelableExtra<PaymentMethod.BillingDetails>(EXTRA_BILLING_DETAILS)

        if (type != null && !hasConfirmStarted) {
            hasConfirmStarted = true
            customPaymentMethodConfirmHandler?.confirmCustomPaymentMethod(
                type,
                billingDetails ?: PaymentMethod.BillingDetails()
            )
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        @Suppress("DEPRECATION")
        val type = intent.getParcelableExtra<PaymentSheet.CustomPaymentMethodConfiguration.CustomPaymentMethodType>(
            EXTRA_CUSTOM_PAYMENT_METHOD_TYPE
        )

        @Suppress("DEPRECATION")
        val customPaymentMethodResult: CustomPaymentMethodResult? =
            intent.getParcelableExtra(CustomPaymentMethodResultHandler.EXTRA_CUSTOM_PAYMENT_METHOD_RESULT)

        if (type == null && customPaymentMethodResult == null) {
            // We expect to start this activity with either a type or a result. If that's not true, it is in an
            // unexpected state and should finish.
            finish()
            return
        }

        customPaymentMethodResult?.let {
            when (it) {
                is CustomPaymentMethodResult.Completed -> setResult(CustomPaymentMethodResult.Completed.RESULT_CODE)
                is CustomPaymentMethodResult.Canceled -> setResult(CustomPaymentMethodResult.Canceled.RESULT_CODE)
                is CustomPaymentMethodResult.Failed -> {
                    val data =
                        Intent().putExtra(CustomPaymentMethodResult.Failed.DISPLAY_MESSAGE_EXTRA, it.displayMessage)
                    setResult(CustomPaymentMethodResult.Failed.RESULT_CODE, data)
                }
            }
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(HAS_CONFIRM_STARTED_KEY, hasConfirmStarted)

        super.onSaveInstanceState(outState)
    }

    internal companion object {
        var customPaymentMethodConfirmHandler: CustomPaymentMethodConfirmHandler? = null

        const val EXTRA_CUSTOM_PAYMENT_METHOD_TYPE = "external_custom_method_type"
        const val EXTRA_BILLING_DETAILS = "external_payment_method_billing_details"

        const val HAS_CONFIRM_STARTED_KEY = "has_confirm_started"
    }
}
