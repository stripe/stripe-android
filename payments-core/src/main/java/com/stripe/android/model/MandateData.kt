package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(cttsai-stripe): MOBILESDK-4044 should convert this data class to @Poko class in next major release
data class MandateData internal constructor(
    val customerAcceptance: CustomerAcceptance
) : StripeModel {

    /**
     * Model for mandate customer acceptance information.
     */
    @Parcelize
    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class CustomerAcceptance internal constructor(
        /**
         * Details about the mandate's online acceptance. Null if mandate is accepted offline.
         */
        val online: Online?,

        /**
         * The type of customer acceptance information included with the Mandate, such as: online or offline.
         */
        val type: String,
    ) : Parcelable {

        /**
         * Model for mandate customer acceptance information collected online.
         */
        @Parcelize
        @Poko
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Online internal constructor(
            /**
             * The IP address from which the Mandate was accepted by the customer.
             */
            val ipAddress: String?,
            /**
             * The user agent of the browser from which the Mandate was accepted by the customer.
             */
            val userAgent: String?,
        ) : Parcelable
    }
}
