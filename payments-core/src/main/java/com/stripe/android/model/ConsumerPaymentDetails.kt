package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Deprecated(
    message = "Use ConsumerPaymentDetails from payments-model.",
    replaceWith = ReplaceWith("ConsumerPaymentDetails2"),
)
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConsumerPaymentDetails internal constructor(
    val paymentDetails: List<PaymentDetails>
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class PaymentDetails(
        open val id: String,
        open val type: String
    ) : Parcelable

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Card(
        override val id: String,
        val last4: String,
    ) : PaymentDetails(id, "card") {

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release.",
        )
        companion object {
            const val type = "card"

            /**
             * Reads the address from the [PaymentMethodCreateParams] mapping format to the format
             * used by [ConsumerPaymentDetails2].
             */
            fun getAddressFromMap(
                cardPaymentMethodCreateParamsMap: Map<String, Any>
            ): Pair<String, Any>? {
                return getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams(cardPaymentMethodCreateParamsMap)
            }
        }
    }
}
