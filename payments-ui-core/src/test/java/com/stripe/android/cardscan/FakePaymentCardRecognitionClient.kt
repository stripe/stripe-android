package com.stripe.android.cardscan

import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.stripe.android.ui.core.cardscan.PaymentCardRecognitionClient

class FakePaymentCardRecognitionClient(private val shouldSuccess: Boolean) : PaymentCardRecognitionClient {
    override fun fetchIntent(
        context: Context,
        onFailure: (Exception) -> Unit,
        onSuccess: (IntentSenderRequest) -> Unit
    ) {
        if (shouldSuccess) {
            val mockPendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                Intent(),
                android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val intentSenderRequest = IntentSenderRequest.Builder(mockPendingIntent.intentSender).build()
            onSuccess(intentSenderRequest)
        } else {
            onFailure(Exception("Failed to fetch intent"))
        }
    }
}
