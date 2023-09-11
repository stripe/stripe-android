package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class CardBillingSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("card_billing"),
    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = CountryUtils.supportedBillingCountries,
    @SerialName("collection_mode")
    val collectionMode: BillingDetailsCollectionConfiguration.AddressCollectionMode =
        BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        addressRepository: AddressRepository,
        shippingValues: Map<IdentifierSpec, String?>?,
    ): SectionElement? {
        if (collectionMode == BillingDetailsCollectionConfiguration.AddressCollectionMode.Never) {
            return null
        }

        val sameAsShippingElement =
            shippingValues?.get(IdentifierSpec.SameAsShipping)
                ?.toBooleanStrictOrNull()
                ?.let {
                    SameAsShippingElement(
                        identifier = IdentifierSpec.SameAsShipping,
                        controller = SameAsShippingController(it)
                    )
                }
        val addressElement = CardBillingAddressElement(
            IdentifierSpec.Generic("credit_billing"),
            addressRepository = addressRepository,
            countryCodes = allowedCountryCodes,
            rawValuesMap = initialValues,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValues,
            collectionMode = collectionMode,
        )

        return createSectionElement(
            listOfNotNull(
                addressElement,
                sameAsShippingElement
            ),
            R.string.stripe_billing_details
        )
    }
}
