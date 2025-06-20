package com.stripe.android.paymentsheet

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.flowcontroller.LinkCoordinatorFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.parcelize.Parcelize

/**
 * A class that presents the individual steps of a Link payment flow.
 */
interface LinkCoordinator {

    /**
     * Configure the LinkCoordinator with Link configuration.
     *
     * @param configuration The Link configuration to use.
     * @param callback called with the result of configuring the LinkCoordinator.
     */
    fun configure(
        configuration: Configuration,
        callback: ConfigCallback
    )

    /**
     * Retrieve information about the customer's selected payment option from Link.
     */
    fun getPaymentOption(): PaymentOption?

    /**
     * Present Link to the customer for payment method selection.
     */
    fun present()

    /**
     * Complete the payment using the selected Link payment method.
     */
    fun confirm()

    @Parcelize
    data class Configuration(
        val stripeIntent: StripeIntent,
        val merchantName: String,
        val customerEmail: String?,
    ) : Parcelable

    sealed class Result {
        object Success : Result()

        class Failure(
            val error: Throwable
        ) : Result()
    }

    fun interface ConfigCallback {
        fun onConfigured(
            success: Boolean,
            error: Throwable?
        )
    }

    /**
     * Builder utility to set callbacks for [LinkCoordinator].
     *
     * @param paymentOptionCallback Called when the customer's payment option changes.
     */
    class Builder(
        internal val paymentOptionCallback: (PaymentOption?) -> Unit
    ) {

        /**
         * Returns a [LinkCoordinator].
         *
         * @param activity The Activity that is presenting [LinkCoordinator].
         */
        fun build(activity: ComponentActivity): LinkCoordinator {
            return LinkCoordinatorFactory(activity, paymentOptionCallback).create()
        }

        /**
         * Returns a [LinkCoordinator].
         *
         * @param fragment The Fragment that is presenting [LinkCoordinator].
         */
        fun build(fragment: Fragment): LinkCoordinator {
            return LinkCoordinatorFactory(fragment, paymentOptionCallback).create()
        }
    }
} 