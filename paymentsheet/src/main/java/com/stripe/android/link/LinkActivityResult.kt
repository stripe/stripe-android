package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerShippingAddress
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

internal sealed class LinkActivityResult : Parcelable {

    abstract val linkAccountUpdate: LinkAccountUpdate?

    /**
     * Indicates that the flow was completed successfully.
     */
    @Parcelize
    internal data class Completed(
        override val linkAccountUpdate: LinkAccountUpdate,
        val selectedPayment: LinkPaymentMethod? = null,
        val shippingAddress: ConsumerShippingAddress? = null,
    ) : LinkActivityResult()

    /**
     * Indicates that the user selected a payment method. This payment method has not yet been confirmed.
     */
    @Parcelize
    internal data class PaymentMethodObtained(
        val paymentMethod: PaymentMethod
    ) : LinkActivityResult() {

        override val linkAccountUpdate: LinkAccountUpdate?
            get() = null
    }

    /**
     * The user cancelled the Link flow without completing it.
     */
    @Parcelize
    internal data class Canceled(
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
    internal data class Failed(
        val error: Throwable,
        override val linkAccountUpdate: LinkAccountUpdate
    ) : LinkActivityResult()
}

@Parcelize
internal sealed interface LinkAccountUpdate : Parcelable {
    @Parcelize
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
    data object None : LinkAccountUpdate

    fun asValue(): Value = when (this) {
        None -> Value(null)
        is Value -> this
    }
}
