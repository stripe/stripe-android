@file:Suppress("ktlint:filename")

package com.stripe.android.ui.core.address

import androidx.annotation.RestrictTo
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.uicore.elements.IdentifierSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Map<IdentifierSpec, String?>.toConfirmPaymentIntentShipping():
    ConfirmPaymentIntentParams.Shipping {
    return ConfirmPaymentIntentParams.Shipping(
        name = this[IdentifierSpec.Name] ?: "",
        address = Address.Builder()
            .setLine1(this[IdentifierSpec.Line1])
            .setLine2(this[IdentifierSpec.Line2])
            .setCity(this[IdentifierSpec.City])
            .setState(this[IdentifierSpec.State])
            .setCountry(this[IdentifierSpec.Country])
            .setPostalCode(this[IdentifierSpec.PostalCode])
            .build(),
        phone = this[IdentifierSpec.Phone]
    )
}
