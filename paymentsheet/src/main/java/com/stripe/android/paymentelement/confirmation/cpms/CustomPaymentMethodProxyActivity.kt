package com.stripe.android.paymentelement.confirmation.cpms

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.CustomPaymentMethodResult
import com.stripe.android.paymentelement.CustomPaymentMethodResultHandler
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Handles custom payment methods.
 *
 * This will call the merchant's [ConfirmCustomPaymentMethodCallback.onConfirmCustomPaymentMethod] when first created.
 * When the merchant's implementation calls [CustomPaymentMethodResultHandler.handleCustomPaymentMethodResult], it will
 * re-start this activity with the result. This class then finishes and the class that launched this activity is
 * responsible for responding to that result.
 */
@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal class CustomPaymentMethodProxyActivity : AppCompatActivity() {

    private var hasConfirmStarted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getBoolean(HAS_CONFIRM_STARTED_KEY)?.let { hasConfirmStarted = it }

        val extras = intent.extras

        val type = extras?.let {
            BundleCompat.getParcelable(
                it,
                EXTRA_CUSTOM_PAYMENT_METHOD_TYPE,
                PaymentSheet.CustomPaymentMethod::class.java,
            )
        }
        val paymentElementCallbackIdentifier = intent.getStringExtra(EXTRA_PAYMENT_ELEMENT_IDENTIFIER)

        val billingDetails = extras?.let {
            BundleCompat.getParcelable(
                it,
                EXTRA_BILLING_DETAILS,
                PaymentMethod.BillingDetails::class.java,
            )
        }

        if (type != null && !hasConfirmStarted && paymentElementCallbackIdentifier != null) {
            hasConfirmStarted = true
            PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.confirmCustomPaymentMethodCallback
                ?.onConfirmCustomPaymentMethod(type, billingDetails ?: PaymentMethod.BillingDetails())
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        val extras = intent.extras ?: run {
            // We expect to start this activity with extras. If that's not true, it is in an  unexpected state and
            // should finish.
            finish()
            return
        }

        val type = BundleCompat.getParcelable(
            extras,
            EXTRA_CUSTOM_PAYMENT_METHOD_TYPE,
            PaymentSheet.CustomPaymentMethod::class.java,
        )

        val customPaymentMethodResult = BundleCompat.getParcelable(
            extras,
            CustomPaymentMethodResultHandler.EXTRA_CUSTOM_PAYMENT_METHOD_RESULT,
            CustomPaymentMethodResult::class.java,
        )

        if (type == null && customPaymentMethodResult == null) {
            // We expect to start this activity with either a type or a result. If that's not true, it is in an
            // unexpected state and should finish.
            finish()
            return
        }

        customPaymentMethodResult?.let {
            val resultExtras = InternalCustomPaymentMethodResult.fromCustomPaymentMethodResult(it).toBundle()

            setResult(
                Activity.RESULT_OK,
                Intent().putExtras(resultExtras)
            )

            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(HAS_CONFIRM_STARTED_KEY, hasConfirmStarted)

        super.onSaveInstanceState(outState)
    }

    internal companion object {
        const val EXTRA_PAYMENT_ELEMENT_IDENTIFIER = "payment_element_identifier"
        const val EXTRA_CUSTOM_PAYMENT_METHOD_TYPE = "extra_custom_method_type"
        const val EXTRA_BILLING_DETAILS = "extra_payment_method_billing_details"

        const val HAS_CONFIRM_STARTED_KEY = "has_confirm_started"
    }
}
