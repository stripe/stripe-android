package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Model for mandate data.
 *
 * See [Mandate Data](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-mandate_data)
 * and [Customer Acceptance](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-mandate_data-customer_acceptance)
 * in the Stripe API reference.
 */
@Parcelize
@Poko
class MandateData
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val customerAcceptance: CustomerAcceptance
) : Parcelable {

    /**
     * Model for mandate customer acceptance information.
     */
    @Parcelize
    @Poko
    class CustomerAcceptance
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        /**
         * If this is a Mandate accepted online, this hash contains details about the online acceptance.
         */
        val online: Online? = null,

        /**
         * The type of customer acceptance information included with the Mandate.
         */
        val type: String,
    ) : Parcelable {

        /**
         * Model for mandate customer acceptance information collected online.
         */
        @Parcelize
        @Poko
        class Online
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        constructor(
            /**
             * The IP address from which the Mandate was accepted by the customer.
             */
            val ipAddress: String? = null,
            /**
             * The user agent of the browser from which the Mandate was accepted by the customer.
             */
            val userAgent: String? = null,
        ) : Parcelable

    }

}
