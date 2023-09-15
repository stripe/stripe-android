package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class PlaceholderSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("placeholder"),
    @SerialName("for")
    val field: PlaceholderField = PlaceholderField.Unknown,
) : FormItemSpec() {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    enum class PlaceholderField {
        @SerialName("name")
        Name,

        @SerialName("email")
        Email,

        @SerialName("phone")
        Phone,

        @SerialName("billing_address")
        BillingAddress,

        @SerialName("billing_address_without_country")
        BillingAddressWithoutCountry,

        @SerialName("sepa_mandate")
        SepaMandate,

        @SerialName("unknown")
        Unknown,
    }
}
