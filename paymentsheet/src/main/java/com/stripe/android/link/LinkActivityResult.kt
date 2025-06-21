package com.stripe.android.link

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerShippingAddress
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class LinkActivityResult : Parcelable {

    abstract val linkAccountUpdate: LinkAccountUpdate?

    /**
     * Indicates that the flow was completed successfully.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Completed(
        override val linkAccountUpdate: LinkAccountUpdate,
        val selectedPayment: LinkPaymentMethod? = null,
        val shippingAddress: ConsumerShippingAddress? = null,
    ) : LinkActivityResult()

    /**
     * Indicates that the user selected a payment method. This payment method has not yet been confirmed.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class PaymentMethodObtained(
        val paymentMethod: PaymentMethod
    ) : LinkActivityResult() {

        override val linkAccountUpdate: LinkAccountUpdate?
            get() = null
    }

    /**
     * The user cancelled the Link flow without completing it.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Canceled(
        val reason: Reason = Reason.BackPressed,
        override val linkAccountUpdate: LinkAccountUpdate
    ) : LinkActivityResult() {
        enum class Reason {
            BackPressed,
            LoggedOut,
            PayAnotherWay
        }
    }

    /**
     * Something went wrong. See [error] for more information.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failed(
        val error: Throwable,
        override val linkAccountUpdate: LinkAccountUpdate
    ) : LinkActivityResult()
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface LinkAccountUpdate : Parcelable {
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Value(
        val account: LinkAccount?,
        val lastUpdateReason: UpdateReason? = null
    ) : LinkAccountUpdate {
        enum class UpdateReason {
            /**
             * The user has logged out of Link.
             */
            LoggedOut,

            /**
             * The user has confirmed a payment method in Link.
             */
            PaymentConfirmed
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object None : LinkAccountUpdate

    fun asValue(): Value = when (this) {
        None -> Value(null)
        is Value -> this
    }
}
