package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.transactions.ErrorData

class ProtocolErrorEventFactory {

    /**
     * @param errorData the [ErrorData] instance which populates the created [ProtocolErrorEvent]
     * @return a [ProtocolErrorEvent]
     */
    fun create(errorData: ErrorData): ProtocolErrorEvent {
        val errorMessage = ErrorMessage(
            transactionId = errorData.acsTransId.orEmpty(),
            errorCode = errorData.errorCode,
            errorDescription = errorData.errorDescription,
            errorDetails = errorData.errorDetail
        )

        return ProtocolErrorEvent(
            sdkTransactionId = errorData.sdkTransId,
            errorMessage = errorMessage
        )
    }
}
