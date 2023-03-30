package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.uicore.address.AddressRepository
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

        @SerialName("unknown")
        Unknown,
    }

    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        addressRepository: AddressRepository,
        shippingValues: Map<IdentifierSpec, String?>?,
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
    ) = when (field) {
        PlaceholderField.Name -> NameSpec().takeIf {
            billingDetailsCollectionConfiguration.name == CollectionMode.Always
        }?.transform(initialValues)
        PlaceholderField.Email -> EmailSpec().takeIf {
            billingDetailsCollectionConfiguration.email == CollectionMode.Always
        }?.transform(initialValues)
        PlaceholderField.Phone -> PhoneSpec().takeIf {
            billingDetailsCollectionConfiguration.phone == CollectionMode.Always
        }?.transform(initialValues)
        PlaceholderField.BillingAddress -> AddressSpec().takeIf {
            billingDetailsCollectionConfiguration.address == AddressCollectionMode.Full
        }?.transform(
            initialValues = initialValues,
            addressRepository = addressRepository,
            shippingValues = shippingValues,
        )
        PlaceholderField.BillingAddressWithoutCountry -> AddressSpec().takeIf {
            billingDetailsCollectionConfiguration.address == AddressCollectionMode.Full
        }?.transform(
            initialValues = initialValues,
            addressRepository = addressRepository,
            shippingValues = shippingValues,
            hideCountry = true,
        )
        PlaceholderField.Unknown -> null
    }
}
