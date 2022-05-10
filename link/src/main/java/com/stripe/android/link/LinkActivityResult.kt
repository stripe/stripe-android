package com.stripe.android.link

import android.app.Activity
import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethodCreateParams
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
         * @param paymentDetails The [ConsumerPaymentDetails.PaymentDetails] selected by the user
         * @param paymentMethodCreateParams The [PaymentMethodCreateParams] to be used to confirm
         *                                  the Stripe Intent.
         */
        @Parcelize
        data class Selected(
            val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
            val paymentMethodCreateParams: PaymentMethodCreateParams
        ) : Success()

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
