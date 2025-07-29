package com.stripe.android.elements

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
data class BillingDetails(
    /**
     * The customer's billing address.
     */
    val address: PaymentSheet.Address? = null,
    /**
     * The customer's email.
     * The value set is displayed in the payment sheet as-is.
     * Depending on the payment method, the customer may be required to edit this value.
     */
    val email: String? = null,
    /**
     * The customer's full name.
     * The value set is displayed in the payment sheet as-is.
     * Depending on the payment method, the customer may be required to edit this value.
     */
    val name: String? = null,
    /**
     * The customer's phone number without formatting e.g. 5551234567
     */
    val phone: String? = null
) : Parcelable {
    internal fun isFilledOut(): Boolean {
        return address != null ||
            email != null ||
            name != null ||
            phone != null
    }

    /**
     * [BillingDetails] builder for cleaner object creation from Java.
     */
    class Builder {
        private var address: PaymentSheet.Address? = null
        private var email: String? = null
        private var name: String? = null
        private var phone: String? = null

        fun address(address: PaymentSheet.Address?) = apply { this.address = address }
        fun address(addressBuilder: PaymentSheet.Address.Builder) =
            apply { this.address = addressBuilder.build() }

        fun email(email: String?) = apply { this.email = email }
        fun name(name: String?) = apply { this.name = name }
        fun phone(phone: String?) = apply { this.phone = phone }

        fun build() = BillingDetails(address, email, name, phone)
    }
}
