package com.stripe.android.lpmfoundations

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.forms.convertToFormValuesMap
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.ParameterDestination

internal object InitialValuesFactory {
    fun create(
        defaultBillingDetails: PaymentSheet.BillingDetails?,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?
    ): Map<FormFieldId, String?> {
        val initialValues = paymentMethodCreateParams?.let {
            convertToFormValuesMap(it.toParamMap())
        } ?: emptyMap()

        val initialExtras = paymentMethodExtraParams?.let {
            convertToFormValuesMap(it.toParamMap()).mapKeys { entry ->
                entry.key.copy(destination = ParameterDestination.Local.Extras)
            }
        } ?: emptyMap()

        return mapOf(
            FormFieldId.Name to defaultBillingDetails?.name,
            FormFieldId.Email to defaultBillingDetails?.email,
            FormFieldId.Phone to defaultBillingDetails?.phone,
            FormFieldId.Line1 to defaultBillingDetails?.address?.line1,
            FormFieldId.Line2 to defaultBillingDetails?.address?.line2,
            FormFieldId.City to defaultBillingDetails?.address?.city,
            FormFieldId.State to defaultBillingDetails?.address?.state,
            FormFieldId.Country to defaultBillingDetails?.address?.country,
            FormFieldId.PostalCode to defaultBillingDetails?.address?.postalCode
        ).plus(initialValues).plus(initialExtras)
    }
}
