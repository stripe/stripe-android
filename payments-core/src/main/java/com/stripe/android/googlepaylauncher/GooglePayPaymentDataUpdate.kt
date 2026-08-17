package com.stripe.android.googlepaylauncher

import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
class GooglePayPaymentDataUpdate(
    val callbackTrigger: CallbackTrigger,
    val shippingAddress: ShippingAddress?,
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class CallbackTrigger {
        Initialize,
        ShippingAddress,
        ShippingOption,
        Offer,
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Poko
    class ShippingAddress(
        val administrativeArea: String,
        val countryCode: String,
        val locality: String,
        val postalCode: String,
        val iso3166AdministrativeArea: String?,
    )
}
