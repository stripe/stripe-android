package com.stripe.android.googlepaylauncher.callback

import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.stripe.android.GooglePayJsonFactory
import dev.drewhamilton.poko.Poko
import org.json.JSONObject

/**
 * Response returned from [GooglePayPaymentDataChangedCallback] to update the Google Pay payment
 * sheet with new transaction details and/or shipping options.
 */
@Poko
class GooglePayPaymentDataRequestUpdate @JvmOverloads constructor(
    val newTransactionInfo: GooglePayJsonFactory.TransactionInfo? = null,
    val newShippingOptionParameters: GooglePayJsonFactory.ShippingOptionParameters? = null,
    val error: GooglePayPaymentDataError? = null,
) {
    internal fun toPaymentDataRequestUpdate(
        googlePayJsonFactory: GooglePayJsonFactory,
    ): PaymentDataRequestUpdate {
        return PaymentDataRequestUpdate.fromJson(
            GooglePayPaymentDataRequestUpdateFactory.toJson(
                update = this,
                googlePayJsonFactory = googlePayJsonFactory,
            ).toString()
        )
    }
}

@Poko
class GooglePayPaymentDataError @JvmOverloads constructor(
    val reason: Reason,
    val message: String,
    val intent: Intent = Intent.SHIPPING_ADDRESS,
) {
    enum class Reason(val code: String) {
        SHIPPING_ADDRESS_INVALID("SHIPPING_ADDRESS_INVALID"),
        SHIPPING_ADDRESS_UNSERVICEABLE("SHIPPING_ADDRESS_UNSERVICEABLE"),
        SHIPPING_OPTION_INVALID("SHIPPING_OPTION_INVALID"),
        OTHER_ERROR("OTHER_ERROR"),
    }

    enum class Intent(val code: String) {
        SHIPPING_ADDRESS("SHIPPING_ADDRESS"),
        SHIPPING_OPTION("SHIPPING_OPTION"),
    }
}

internal object GooglePayPaymentDataRequestUpdateFactory {
    fun toJson(
        update: GooglePayPaymentDataRequestUpdate,
        googlePayJsonFactory: GooglePayJsonFactory,
    ): JSONObject {
        return JSONObject().apply {
            update.newTransactionInfo?.let { transactionInfo ->
                put(
                    "newTransactionInfo",
                    googlePayJsonFactory.createTransactionInfoJson(transactionInfo)
                )
            }

            update.newShippingOptionParameters?.let { shippingOptionParameters ->
                put(
                    "newShippingOptionParameters",
                    googlePayJsonFactory.createShippingOptionParametersJson(shippingOptionParameters)
                )
            }

            update.error?.let { error ->
                put(
                    "error",
                    JSONObject()
                        .put("reason", error.reason.code)
                        .put("message", error.message)
                        .put("intent", error.intent.code)
                )
            }
        }
    }
}
