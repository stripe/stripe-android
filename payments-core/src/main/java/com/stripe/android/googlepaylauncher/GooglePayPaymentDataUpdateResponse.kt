package com.stripe.android.googlepaylauncher

import androidx.annotation.RestrictTo
import com.stripe.android.GooglePayJsonFactory
import dev.drewhamilton.poko.Poko
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
class GooglePayPaymentDataUpdateResponse(
    val newTransactionInfo: GooglePayJsonFactory.TransactionInfo?,
    val error: GooglePayPaymentDataError?,
) {
    internal fun toJson(googlePayJsonFactory: GooglePayJsonFactory): JSONObject {
        return JSONObject().apply {
            newTransactionInfo?.let { transactionInfo ->
                put(
                    FIELD_NEW_TRANSACTION_INFO,
                    googlePayJsonFactory.createTransactionInfo(transactionInfo)
                )
            }

            error?.let { error ->
                put(
                    FIELD_ERROR,
                    JSONObject()
                        .put(FIELD_REASON, error.reason.code)
                        .put(FIELD_MESSAGE, error.message)
                        .put(FIELD_INTENT, error.intent.code)
                )
            }
        }
    }

    private companion object {
        private const val FIELD_NEW_TRANSACTION_INFO = "newTransactionInfo"
        private const val FIELD_ERROR = "error"
        private const val FIELD_REASON = "reason"
        private const val FIELD_MESSAGE = "message"
        private const val FIELD_INTENT = "intent"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
class GooglePayPaymentDataError(
    val reason: Reason,
    val message: String,
    val intent: Intent = Intent.ShippingAddress,
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Reason(val code: String) {
        ShippingAddressInvalid("SHIPPING_ADDRESS_INVALID"),
        OtherError("OTHER_ERROR"),
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Intent(val code: String) {
        ShippingAddress("SHIPPING_ADDRESS"),
        Offer("OFFER"),
        ShippingOption("SHIPPING_OPTION"),
    }
}
