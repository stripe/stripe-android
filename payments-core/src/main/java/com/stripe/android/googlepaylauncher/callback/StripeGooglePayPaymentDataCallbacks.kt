package com.stripe.android.googlepaylauncher.callback

import android.content.Context
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacks
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.payments.core.analytics.ErrorReporter

internal class StripeGooglePayPaymentDataCallbacks(
    private val googlePayJsonFactory: GooglePayJsonFactory,
    private val errorReporter: ErrorReporter,
) : BasePaymentDataCallbacks() {
    constructor(context: Context) : this(
        context = context,
        paymentConfiguration = PaymentConfiguration.getInstance(context),
    )

    private constructor(
        context: Context,
        paymentConfiguration: PaymentConfiguration,
    ) : this(
        googlePayJsonFactory = GooglePayJsonFactory(
            googlePayConfig = GooglePayConfig(
                publishableKey = paymentConfiguration.publishableKey,
                connectedAccountId = paymentConfiguration.stripeAccountId,
            ),
        ),
        errorReporter = ErrorReporter.createFallbackInstance(
            context = context,
            productUsage = emptySet(),
        ),
    )

    override fun onPaymentDataChanged(
        request: IntermediatePaymentData?,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate>,
    ) {
        GooglePayPaymentDataCallbackHandler.onPaymentDataChanged(
            request = request,
            onCompleteListener = onCompleteListener,
            googlePayJsonFactory = googlePayJsonFactory,
            errorReporter = errorReporter,
        )
    }
}
