package com.stripe.android.link

import android.app.Activity
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LinkActivityResult(
    val resultCode: Int
) : Parcelable {

    /**
     * The Link flow was completed successfully.
     */
    sealed class Success : LinkActivityResult(Activity.RESULT_OK) {

        /**
         * When Link was launched with [LinkActivityContract.Args.completePayment] set to false,
         * this class indicates that the flow was completed successfully and the user has selected
         * a payment method.
         *
         * @param paymentDetails The payment method selected by the user.
         */
        @Parcelize
        data class Selected(val paymentDetails: LinkPaymentDetails) : Success()

        /**
         * When Link was launched with [LinkActivityContract.Args.completePayment] set to true, this
         * object indicates that the flow was completed successfully and the Stripe Intent was
         * confirmed.
         */
        @Parcelize
        object Completed : Success()
    }

    /**
     * The user cancelled the Link flow without completing it.
     */
    @Parcelize
    object Canceled : LinkActivityResult(Activity.RESULT_CANCELED)

    /**
     * Something went wrong. See [error] for more information.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : LinkActivityResult(Activity.RESULT_CANCELED)
}
