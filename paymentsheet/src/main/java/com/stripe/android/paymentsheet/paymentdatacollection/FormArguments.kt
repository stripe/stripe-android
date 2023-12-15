package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.forms.convertToFormValuesMap
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.ParameterDestination
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class FormArguments(
    val paymentMethodCode: PaymentMethodCode,
    val showCheckbox: Boolean,
    val showCheckboxControlledFields: Boolean,
    val cbcEligibility: CardBrandChoiceEligibility,
    val merchantName: String,
    val amount: Amount? = null,
    val billingDetails: PaymentSheet.BillingDetails? = null,
    val shippingDetails: AddressDetails? = null,
    val initialPaymentMethodCreateParams: PaymentMethodCreateParams? = null,
    val initialPaymentMethodExtraParams: PaymentMethodExtraParams? = null,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
        PaymentSheet.BillingDetailsCollectionConfiguration(),
    val requiresMandate: Boolean = false,
    val requiredFields: List<IdentifierSpec> = emptyList()
) : Parcelable

internal fun FormArguments.getInitialValuesMap(): Map<IdentifierSpec, String?> {
    val initialValues = initialPaymentMethodCreateParams?.let {
        convertToFormValuesMap(it.toParamMap())
    } ?: emptyMap()

    val initialExtras = initialPaymentMethodExtraParams?.let {
        convertToFormValuesMap(it.toParamMap()).mapKeys { entry ->
            entry.key.copy(destination = ParameterDestination.Local.Extras)
        }
    } ?: emptyMap()

    return mapOf(
        IdentifierSpec.Name to this.billingDetails?.name,
        IdentifierSpec.Email to this.billingDetails?.email,
        IdentifierSpec.Phone to this.billingDetails?.phone,
        IdentifierSpec.Line1 to this.billingDetails?.address?.line1,
        IdentifierSpec.Line2 to this.billingDetails?.address?.line2,
        IdentifierSpec.City to this.billingDetails?.address?.city,
        IdentifierSpec.State to this.billingDetails?.address?.state,
        IdentifierSpec.Country to this.billingDetails?.address?.country,
        IdentifierSpec.PostalCode to this.billingDetails?.address?.postalCode
    ).plus(initialValues).plus(initialExtras)
}
