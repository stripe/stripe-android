package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.forms.convertToFormValuesMap
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.ParameterDestination

internal object InitialValuesFactory {
    fun create(
        defaultBillingDetails: PaymentSheet.BillingDetails?,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?
    ): Map<IdentifierSpec, String?> {
        val initialValues = paymentMethodCreateParams?.let {
            convertToFormValuesMap(it.toParamMap())
        } ?: emptyMap()

        val initialExtras = paymentMethodExtraParams?.let {
            convertToFormValuesMap(it.toParamMap()).mapKeys { entry ->
                entry.key.copy(destination = ParameterDestination.Local.Extras)
            }
        } ?: emptyMap()

        return mapOf(
            IdentifierSpec.Name to defaultBillingDetails?.name,
            IdentifierSpec.Email to defaultBillingDetails?.email,
            IdentifierSpec.Phone to defaultBillingDetails?.phone,
            IdentifierSpec.Line1 to defaultBillingDetails?.address?.line1,
            IdentifierSpec.Line2 to defaultBillingDetails?.address?.line2,
            IdentifierSpec.City to defaultBillingDetails?.address?.city,
            IdentifierSpec.State to defaultBillingDetails?.address?.state,
            IdentifierSpec.Country to defaultBillingDetails?.address?.country,
            IdentifierSpec.PostalCode to defaultBillingDetails?.address?.postalCode
        ).plus(initialValues).plus(initialExtras)
    }
}
