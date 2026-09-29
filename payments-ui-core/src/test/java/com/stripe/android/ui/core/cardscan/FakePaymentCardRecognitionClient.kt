package com.stripe.android.ui.core.cardscan

import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest

internal class FakePaymentCardRecognitionClient(
    private val loadingState: CardScanLoadingState,
) : PaymentCardRecognitionClient {
    constructor(shouldSucceed: Boolean) : this(
        loadingState = if (shouldSucceed) {
            CardScanLoadingState.Available
        } else {
            CardScanLoadingState.Unavailable
        }
    )

    override fun fetchIntent(
        context: Context,
        onFailure: (Throwable) -> Unit,
        onSuccess: (IntentSenderRequest) -> Unit
    ) {
        when (loadingState) {
            CardScanLoadingState.Loading -> Unit
            CardScanLoadingState.Available -> {
                val mockPendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    0,
                    Intent(),
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val intentSenderRequest = IntentSenderRequest.Builder(mockPendingIntent.intentSender).build()
                onSuccess(intentSenderRequest)
            }
            CardScanLoadingState.Unavailable -> {
                onFailure(Exception("Failed to fetch intent"))
            }
        }
    }
}
