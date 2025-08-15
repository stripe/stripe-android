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
    }

    private fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(
                if (isStripeExampleApp(context))
                    WalletConstants.ENVIRONMENT_TEST
                else
                    WalletConstants.ENVIRONMENT_PRODUCTION
            )
            .build()

        return Wallet.getPaymentsClient(context, walletOptions)
    }

    private fun isStripeExampleApp(context: Context): Boolean {
        val packageName = context.packageName

        // Only Stripe's official example apps
        return packageName.startsWith("com.stripe.android.") &&
            packageName.contains("example")
    }
}