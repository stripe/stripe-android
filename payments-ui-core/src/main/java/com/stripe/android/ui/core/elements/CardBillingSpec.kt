package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
data class CardBillingSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("card_billing"),
    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = CountryUtils.supportedBillingCountries,
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        shippingValues: Map<IdentifierSpec, String?>?,
    ): SectionElement? {
        val addressElement = CardBillingAddressElement(
            IdentifierSpec.Generic("credit_billing"),
            countryCodes = allowedCountryCodes,
            rawValuesMap = initialValues,
            sameAsShippingElement = null,
            shippingValuesMap = shippingValues,
            collectionMode = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
        )

        return createSectionElement(
            sectionFieldElements = listOfNotNull(
                addressElement,
            ),
            label = R.string.stripe_billing_details
        )
    }
}
