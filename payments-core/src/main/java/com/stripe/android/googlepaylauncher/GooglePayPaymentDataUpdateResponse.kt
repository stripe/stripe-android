package com.stripe.android.googlepaylauncher

import androidx.annotation.RestrictTo
import com.stripe.android.GooglePayJsonFactory
import dev.drewhamilton.poko.Poko

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
class GooglePayPaymentDataUpdateResponse(
    val newTransactionInfo: GooglePayJsonFactory.TransactionInfo?,
    val error: GooglePayPaymentDataError?,
)

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
