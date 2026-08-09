package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.AutomaticTaxBillingAddressSpec
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.ui.core.elements.additionalAutomaticTaxFieldsByCountry
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement

internal class AutomaticTaxBillingAddressFactory(
    private val arguments: UiDefinitionFactory.Arguments,
) {
    fun create(
        spec: AutomaticTaxBillingAddressSpec,
    ): List<FormElement> {
        val sameAsShippingElement = arguments.shippingValues
            ?.get(IdentifierSpec.SameAsShipping)
            ?.toBooleanStrictOrNull()
            ?.let { sameAsShipping ->
                SameAsShippingElement(
                    identifier = IdentifierSpec.SameAsShipping,
                    controller = SameAsShippingController(sameAsShipping),
                )
            }
        val billingAddressElement = BillingAddressElement(
            identifier = IdentifierSpec.BillingAddress,
            rawValuesMap = arguments.initialValues,
            countryCodes = spec.allowedCountryCodes,
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = arguments.shippingValues,
            addressCollectionMode = BillingAddressCollectionMode.Country(
                additionalFieldsByCountry = additionalAutomaticTaxFieldsByCountry,
            ),
        )

        return listOfNotNull(
            SectionElement.wrap(billingAddressElement),
            sameAsShippingElement,
        )
    }
}
