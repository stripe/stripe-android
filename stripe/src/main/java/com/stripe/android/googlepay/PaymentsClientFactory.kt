package com.stripe.android.googlepay

import android.app.Activity
import android.content.Context
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants

internal class PaymentsClientFactory(
    private val context: Context
) {
    fun create(
        environment: StripeGooglePayEnvironment
    ): PaymentsClient {
        val options = Wallet.WalletOptions.Builder()
            .setEnvironment(
                when (environment) {
                    StripeGooglePayEnvironment.Production ->
                        WalletConstants.ENVIRONMENT_PRODUCTION
                    StripeGooglePayEnvironment.Test ->
                        WalletConstants.ENVIRONMENT_TEST
                }
            )
            .build()

        return if (context is Activity) {
            Wallet.getPaymentsClient(context, options)
        } else {
            Wallet.getPaymentsClient(context, options)
        }
    }
}
