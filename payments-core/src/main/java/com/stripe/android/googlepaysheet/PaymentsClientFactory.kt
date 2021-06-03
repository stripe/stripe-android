package com.stripe.android.googlepaysheet

import android.content.Context
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet

internal class PaymentsClientFactory(
    private val context: Context
) {
    fun create(
        environment: GooglePaySheetEnvironment
    ): PaymentsClient {
        val options = Wallet.WalletOptions.Builder()
            .setEnvironment(environment.value)
            .build()

        return Wallet.getPaymentsClient(context, options)
    }
}
