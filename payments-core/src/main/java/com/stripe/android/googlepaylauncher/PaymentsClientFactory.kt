package com.stripe.android.googlepaylauncher

import android.content.Context
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet

internal class PaymentsClientFactory(
    private val context: Context
) {
    fun create(
        environment: GooglePayEnvironment
    ): PaymentsClient {
        val options = Wallet.WalletOptions.Builder()
            .setEnvironment(environment.value)
            .build()

        return Wallet.getPaymentsClient(context, options)
    }
}
