package com.stripe.android.ui.core.cardscan

import android.content.Context
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.wallet.PaymentCardRecognitionIntentRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants

internal interface PaymentCardRecognitionClient {
    fun fetchIntent(
        context: Context,
        onFailure: (Exception) -> Unit = {},
        onSuccess: (IntentSenderRequest) -> Unit
    )
}

internal class DefaultPaymentCardRecognitionClient : PaymentCardRecognitionClient {
    override fun fetchIntent(
        context: Context,
        onFailure: (Exception) -> Unit,
        onSuccess: (IntentSenderRequest) -> Unit
    ) {
        try {
            val paymentsClient = createPaymentsClient(context)
            val request = PaymentCardRecognitionIntentRequest.getDefaultInstance()

            paymentsClient.getPaymentCardRecognitionIntent(request)
                .addOnSuccessListener { intentResponse ->
                    val pendingIntent = intentResponse.paymentCardRecognitionPendingIntent
                    val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    onSuccess(intentSenderRequest)
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            onFailure(e)
        }
    }

    private fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)
            .build()

        return Wallet.getPaymentsClient(context, walletOptions)
    }
}