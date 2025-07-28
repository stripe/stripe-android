package com.stripe.android.elements

import android.os.Parcelable
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet.CustomerAccessType
import kotlinx.parcelize.Parcelize

@Parcelize
data class CustomerConfiguration internal constructor(
    /**
     * The identifier of the Stripe Customer object.
     * See [Stripe's documentation](https://stripe.com/docs/api/customers/object#customer_object-id).
     */
    val id: String,

    /**
     * A short-lived token that allows the SDK to access a Customer's payment methods.
     */
    val ephemeralKeySecret: String,

    internal val accessType: CustomerAccessType,
) : Parcelable {

    constructor(
        id: String,
        ephemeralKeySecret: String,
    ) : this(
        id = id,
        ephemeralKeySecret = ephemeralKeySecret,
        accessType = CustomerAccessType.LegacyCustomerEphemeralKey(ephemeralKeySecret)
    )

    companion object {
        @ExperimentalCustomerSessionApi
        fun createWithCustomerSession(
            id: String,
            clientSecret: String
        ): CustomerConfiguration {
            return CustomerConfiguration(
                id = id,
                ephemeralKeySecret = "",
                accessType = CustomerAccessType.CustomerSession(clientSecret)
            )
        }
    }
}
