package com.stripe.android.paymentsheet

import android.content.Context
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.stripe.android.GooglePayJsonFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class DefaultGooglePayRepository(
    private val context: Context
) : GooglePayRepository {
    private val googlePayJsonFactory = GooglePayJsonFactory(context)

    private val paymentsClient: PaymentsClient by lazy {
        Wallet.getPaymentsClient(
            context,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()
        )
    }

    override fun isReady(): Flow<Boolean?> {
        val isReadyState = MutableStateFlow<Boolean?>(null)

        val request = IsReadyToPayRequest.fromJson(
            googlePayJsonFactory.createIsReadyToPayRequest().toString()
        )

        paymentsClient.isReadyToPay(request)
            .addOnCompleteListener { task ->
                isReadyState.value = runCatching {
                    task.isSuccessful
                }.getOrDefault(false)
            }

        return isReadyState
    }
}
