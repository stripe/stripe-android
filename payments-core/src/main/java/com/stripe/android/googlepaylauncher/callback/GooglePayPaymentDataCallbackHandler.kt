package com.stripe.android.googlepaylauncher.callback

import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallbackRegistry
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlinx.coroutines.launch

internal object GooglePayPaymentDataCallbackHandler {
    fun onPaymentDataChanged(
        request: IntermediatePaymentData?,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate>,
        googlePayJsonFactory: GooglePayJsonFactory,
        errorReporter: ErrorReporter,
    ) {
        val selection = GooglePayPaymentDataUpdateCallbackRegistry.get()

        val hasNoRequest = request == null

        if (hasNoRequest || selection == null) {
            val event = if (hasNoRequest) {
                ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_MISSING_REQUEST
            } else {
                ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_MISSING_CALLBACK
            }

            errorReporter.report(event)

            onCompleteListener.complete(PaymentDataRequestUpdate.fromJson("{}"))
            return
        }

        selection.workScope.launch {
            val update = GooglePayPaymentDataUpdate.fromIntermediatePaymentData(request)

            runCatching {
                val update = GooglePayPaymentDataUpdate.fromIntermediatePaymentData(request)
                val response = selection.callback.onPaymentDataChanged(update)
                response.toJson(googlePayJsonFactory)
            }.fold(
                onSuccess = { json ->
                    onCompleteListener.complete(PaymentDataRequestUpdate.fromJson(json.toString()))
                },
                onFailure = { error ->
                    onCompleteListener.complete(
                        PaymentDataRequestUpdate.fromJson(
                            GooglePayPaymentDataUpdateResponse(
                                newTransactionInfo = null,
                                error = GooglePayPaymentDataError(
                                    reason = GooglePayPaymentDataError.Reason.OtherError,
                                    message = error.message.orEmpty(),
                                    intent = when (update.callbackTrigger) {
                                        null,
                                        GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
                                        GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress ->
                                            GooglePayPaymentDataError.Intent.ShippingAddress
                                        GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption ->
                                            GooglePayPaymentDataError.Intent.ShippingOption
                                        GooglePayPaymentDataUpdate.CallbackTrigger.Offer ->
                                            GooglePayPaymentDataError.Intent.Offer
                                    },
                                ),
                            ).toJson(
                                googlePayJsonFactory = googlePayJsonFactory,
                            ).toString()
                        )
                    )
                }
            )
        }.invokeOnCompletion { cause ->
        }
    }
}
