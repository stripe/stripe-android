package com.stripe.android.paymentelement.confirmation.cpms

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class CustomPaymentMethodContract :
    ActivityResultContract<CustomPaymentMethodInput, InternalCustomPaymentMethodResult>() {
    override fun createIntent(context: Context, input: CustomPaymentMethodInput): Intent {
        return Intent().setClass(
            context,
            CustomPaymentMethodProxyActivity::class.java
        )
            .putExtra(
                CustomPaymentMethodProxyActivity.EXTRA_PAYMENT_ELEMENT_IDENTIFIER,
                input.paymentElementCallbackIdentifier
            )
            .putExtra(CustomPaymentMethodProxyActivity.EXTRA_CUSTOM_PAYMENT_METHOD_TYPE, input.type)
            .putExtra(CustomPaymentMethodProxyActivity.EXTRA_BILLING_DETAILS, input.billingDetails)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): InternalCustomPaymentMethodResult {
        return InternalCustomPaymentMethodResult.fromIntent(intent)
    }
}
