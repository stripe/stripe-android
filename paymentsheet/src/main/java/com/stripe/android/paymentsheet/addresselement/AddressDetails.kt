package com.stripe.android.paymentsheet.addresselement

import android.os.Parcelable
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddressDetails(
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
): Map<IdentifierSpec, String?> {
    return if (billingDetails == null) {
        mapOf(
            IdentifierSpec.Name to name,
            IdentifierSpec.Line1 to address?.line1,
            IdentifierSpec.Line2 to address?.line2,
            IdentifierSpec.City to address?.city,
            IdentifierSpec.State to address?.state,
            IdentifierSpec.PostalCode to address?.postalCode,
            IdentifierSpec.Country to address?.country,
            IdentifierSpec.Phone to phoneNumber
        ).plus(
            mapOf(
                IdentifierSpec.SameAsShipping to isCheckboxSelected?.toString()
            ).takeIf { isCheckboxSelected != null } ?: emptyMap()
        )
    } else {
        emptyMap()
    }
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
