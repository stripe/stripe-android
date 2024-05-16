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
internal class ExternalPaymentMethodProxyActivity : AppCompatActivity() {

    private var hasStarted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getBoolean(HAS_STARTED_CONFIRM_STATE_KEY)?.let { hasStarted = it }

        val type = intent.getStringExtra(EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE)

        @Suppress("DEPRECATION")
        val billingDetails = intent.getParcelableExtra<PaymentMethod.BillingDetails>(EXTRA_BILLING_DETAILS)

        if (type != null && !hasStarted) {
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

        val type = intent.getStringExtra(EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE)

        @Suppress("DEPRECATION")
        val externalPaymentMethodResult: ExternalPaymentMethodResult? =
            intent.getParcelableExtra(ExternalPaymentMethodResultHandler.EXTRA_EXTERNAL_PAYMENT_METHOD_RESULT)

        if (type == null && externalPaymentMethodResult == null) {
            // We expect to start this activity with either a type or a result. If that's not true, it is in an
            // unexpected state and should finish.
            finish()
            return
        }

        // This calls finish immediately, so whatever is listening for the result (PS or FC) has already received an
        // event when we get here.
        if (hasStarted && externalPaymentMethodResult == null) {
            // When this activity is resumed but the external payment method result is not set, we should return to
            // the PaymentSheet or FlowController activity to avoid hanging in a "processing" state.
            finish()
            return
        }

        externalPaymentMethodResult?.let {
            when (it) {
                is ExternalPaymentMethodResult.Completed -> setResult(ExternalPaymentMethodResult.Completed.RESULT_CODE)
                is ExternalPaymentMethodResult.Canceled -> setResult(ExternalPaymentMethodResult.Canceled.RESULT_CODE)
                is ExternalPaymentMethodResult.Failed -> {
                    val data =
                        Intent().putExtra(ExternalPaymentMethodResult.Failed.DISPLAY_MESSAGE_EXTRA, it.displayMessage)
                    setResult(ExternalPaymentMethodResult.Failed.RESULT_CODE, data)
                }
            }
            finish()
        }
    }

    override fun onPause() {
        super.onPause()

        hasStarted = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(HAS_STARTED_CONFIRM_STATE_KEY, true)

        super.onSaveInstanceState(outState)
    }

    internal companion object {
        const val HAS_STARTED_CONFIRM_STATE_KEY = "has_started_confirm"

        const val EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE = "external_payment_method_type"
        const val EXTRA_BILLING_DETAILS = "external_payment_method_billing_details"
    }
}
