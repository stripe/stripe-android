package com.stripe.android.paymentsheet.addresselement

import android.os.Parcelable
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class AddressDetails(
    /**
     * The customer's full name
     */
    val name: String? = null,
    /**
     * The customer's address
     */
    val address: PaymentSheet.Address? = null,
    /**
     * The customer's phone number, without formatting e.g. "5551234567"
     */
    val phoneNumber: String? = null,
    /**
     * Whether or not your custom checkbox is selected.
     * Note: The checkbox is displayed below the other fields when AdditionalFieldsConfiguration.checkboxLabel is set.
     */
    val isCheckboxSelected: Boolean? = null
) : Parcelable {
    companion object {
        const val KEY = "AddressDetails"
    }
}

internal fun AddressDetails.toIdentifierMap(
    billingDetails: PaymentSheet.BillingDetails? = null
): Map<FormFieldId, String?> {
    return if (billingDetails == null || !billingDetails.isFilledOut()) {
        mapOf(
            FormFieldId.Name to name,
            FormFieldId.Line1 to address?.line1,
            FormFieldId.Line2 to address?.line2,
            FormFieldId.City to address?.city,
            FormFieldId.State to address?.state,
            FormFieldId.PostalCode to address?.postalCode,
            FormFieldId.Country to address?.country,
            FormFieldId.Phone to phoneNumber
        )
            .plus(billingDetails?.address?.toIdentifierMap() ?: emptyMap())
            .plus(
                mapOf(
                    FormFieldId.SameAsShipping to isCheckboxSelected?.toString()
                ).takeIf { isCheckboxSelected != null } ?: emptyMap()
            )
    } else {
        emptyMap()
    }
}

internal fun PaymentSheet.Address.toIdentifierMap(): Map<FormFieldId, String?> {
    return mapOf(
        FormFieldId.Line1 to line1,
        FormFieldId.Line2 to line2,
        FormFieldId.City to city,
        FormFieldId.State to state,
        FormFieldId.PostalCode to postalCode,
        FormFieldId.Country to country,
    )
}

internal fun AddressDetails.toConfirmPaymentIntentShipping(): ConfirmPaymentIntentParams.Shipping {
    return ConfirmPaymentIntentParams.Shipping(
        name = this.name ?: "",
        address = Address.Builder()
            .setLine1(this.address?.line1)
            .setLine2(this.address?.line2)
            .setCity(this.address?.city)
            .setState(this.address?.state)
            .setCountry(this.address?.country)
            .setPostalCode(this.address?.postalCode)
            .build(),
        phone = this.phoneNumber
    )
}
