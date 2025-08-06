package com.stripe.android.elements

import android.os.Parcelable
import com.stripe.android.elements.payment.PaymentSheet
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class CustomerConfiguration internal constructor(
    /**
     * The identifier of the Stripe Customer object.
     * See [Stripe's documentation](https://stripe.com/docs/api/customers/object#customer_object-id).
     */
    val id: String,

    /**
     * A short-lived token that allows the SDK to access a Customer's payment methods.
     */
    internal val ephemeralKeySecret: String,

    internal val accessType: PaymentSheet.CustomerAccessType,
) : Parcelable {
    constructor(
        id: String,
        ephemeralKeySecret: String,
    ) : this(
        id = id,
        ephemeralKeySecret = ephemeralKeySecret,
        accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey(ephemeralKeySecret)
    )

    companion object {
        @CustomerSessionApiPreview
        fun createWithCustomerSession(
            id: String,
            clientSecret: String
        ): CustomerConfiguration {
            return CustomerConfiguration(
                id = id,
                ephemeralKeySecret = "",
                accessType = PaymentSheet.CustomerAccessType.CustomerSession(clientSecret)
            )
        }
    }
}
