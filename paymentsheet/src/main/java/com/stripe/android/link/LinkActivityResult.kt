package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

internal sealed class LinkActivityResult : Parcelable {
    /**
     * Indicates that the flow was completed successfully.
     */
    @Parcelize
    data class Completed(
        val linkAccountUpdate: LinkAccountUpdate
    ) : LinkActivityResult()

    /**
     * Indicates that the user selected a payment method. This payment method has not yet been confirmed.
     */
    @Parcelize
    data class PaymentMethodObtained(
        val paymentMethod: PaymentMethod
    ) : LinkActivityResult()

    /**
     * The user cancelled the Link flow without completing it.
     */
    @Parcelize
    data class Canceled(
        val reason: Reason = Reason.BackPressed,
        val linkAccountUpdate: LinkAccountUpdate
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
    data class Failed(
        val error: Throwable,
        val linkAccountUpdate: LinkAccountUpdate
    ) : LinkActivityResult()
}

@Parcelize
internal sealed interface LinkAccountUpdate : Parcelable {
    @Parcelize
    data class Value(val linkAccount: LinkAccount?) : LinkAccountUpdate

    @Parcelize
    data object None : LinkAccountUpdate
}
