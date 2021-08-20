package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.parcelize.Parcelize

@Parcelize
data class ComposeFragmentArguments(
    val supportedPaymentMethodName: String,
    val saveForFutureUseInitialVisibility: Boolean,
    val saveForFutureUseInitialValue: Boolean,
    val merchantName: String,
    val billingDetails: BillingDetails?,
) : Parcelable

@Parcelize
data class Address(
    val city: String? = null,
    val country: String? = null,
    val line1: String? = null,
    val line2: String? = null,
    val postalCode: String? = null,
    val state: String? = null
) : Parcelable

@Parcelize
data class BillingDetails(
    val address: Address?,
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null
) : Parcelable

fun ComposeFragmentArguments.getValue(id: IdentifierSpec) =
    when (id.value) {
        "name" -> this.billingDetails?.name
        "email" -> this.billingDetails?.email
        "phone" -> this.billingDetails?.phone
        "line1" -> this.billingDetails?.address?.line1
        "line2" -> this.billingDetails?.address?.line2
        "city" -> this.billingDetails?.address?.city
        "state" -> this.billingDetails?.address?.state
        "country" -> this.billingDetails?.address?.country
        "postal_code" -> this.billingDetails?.address?.postalCode
        else -> null
    }

fun ComposeFragmentArguments.toFormFieldValues(): FormFieldValues {
    // TODO: Make sure these identifier specs are standardized
    return FormFieldValues(
        listOf(
            IdentifierSpec("name"),
            IdentifierSpec("email"),
            IdentifierSpec("phone"),
            IdentifierSpec("line1"),
            IdentifierSpec("line2"),
            IdentifierSpec("city"),
            IdentifierSpec("state"),
            IdentifierSpec("country"),
            IdentifierSpec("postal_code"),
        ).associateWith { FormFieldEntry(this.getValue(it)) }
            .filterValues { it.value != null },
        saveForFutureUse = this.saveForFutureUseInitialValue,
        showsMandate = false
    )
}
