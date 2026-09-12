package com.stripe.android.googlepaylauncher.callback

import android.content.Context
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacks
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallbackRegistry
import com.stripe.android.payments.core.analytics.ErrorReporter

internal class StripeGooglePayPaymentDataCallbacks(
    private val context: Context,
) : BasePaymentDataCallbacks() {
    public override fun onPaymentDataChanged(
        request: IntermediatePaymentData?,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate>,
    ) {
        val apiConfiguration = GooglePayPaymentDataUpdateCallbackRegistry.get()?.apiConfiguration
            ?: PaymentConfiguration.getInstance(context).let {
                ApiConfiguration.State(it.publishableKey, it.stripeAccountId)
            }
        GooglePayPaymentDataCallbackHandler.onPaymentDataChanged(
            request = request,
            onCompleteListener = onCompleteListener,
            googlePayJsonFactory = GooglePayJsonFactory(
                googlePayConfig = GooglePayConfig(
                    publishableKey = apiConfiguration.publishableKey,
                    connectedAccountId = apiConfiguration.stripeAccountId,
                ),
            ),
            errorReporter = ErrorReporter.createFallbackInstance(
                context = context,
                apiConfigurationProvider = { apiConfiguration },
                productUsage = emptySet(),
            ),
            stringResolver = { it.resolve(context) },
        )
    }
}
