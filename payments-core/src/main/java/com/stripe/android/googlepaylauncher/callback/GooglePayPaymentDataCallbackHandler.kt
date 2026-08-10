package com.stripe.android.googlepaylauncher.callback

import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter

internal object GooglePayPaymentDataCallbackHandler {
    fun onPaymentDataChanged(
        request: IntermediatePaymentData?,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate>,
        googlePayJsonFactory: GooglePayJsonFactory,
        errorReporter: ErrorReporter,
    ) {
        if (request == null) {
            onCompleteListener.complete(PaymentDataRequestUpdate.fromJson("{}"))
            return
        }

        runCatching {
            val intermediatePaymentData = GooglePayIntermediatePaymentData.fromIntermediatePaymentData(
                request
            )

            GooglePayPaymentDataCallbackRegistry.handle(intermediatePaymentData) { update ->
                if (update.error?.message == GooglePayPaymentDataCallbackRegistry.NOT_REGISTERED_ERROR_MESSAGE) {
                    errorReporter.report(
                        ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_NOT_REGISTERED,
                    )
                }

                runCatching {
                    update.toPaymentDataRequestUpdate(googlePayJsonFactory)
                }.fold(
                    onSuccess = { paymentDataRequestUpdate ->
                        onCompleteListener.complete(paymentDataRequestUpdate)
                    },
                    onFailure = { error ->
                        errorReporter.report(
                            ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_FAILURE,
                            StripeException.create(error),
                        )

                        onCompleteListener.complete(
                            PaymentDataRequestUpdate.fromJson(
                                GooglePayPaymentDataRequestUpdateFactory.toJson(
                                    update = GooglePayPaymentDataRequestUpdate(
                                        newTransactionInfo = null,
                                        newShippingOptionParameters = null,
                                        error = GooglePayPaymentDataError(
                                            reason = GooglePayPaymentDataError.Reason.OTHER_ERROR,
                                            message = error.message.orEmpty(),
                                            intent = GooglePayPaymentDataError.Intent.SHIPPING_ADDRESS,
                                        ),
                                    ),
                                    googlePayJsonFactory = googlePayJsonFactory,
                                ).toString()
                            )
                        )
                    }
                )
            }
        }
    }
}
