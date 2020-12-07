package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentSheet internal constructor(
    private val args: PaymentSheetActivityStarter.Args
) {
    /**
     * Create PaymentSheet with a Customer
     */
    constructor(
        paymentIntentClientSecret: String,
        customerConfiguration: CustomerConfiguration
    ) : this(
        PaymentSheetActivityStarter.Args.Default(
            paymentIntentClientSecret,
            customerConfiguration
        )
    )

    /**
     * Create PaymentSheet without a Customer
     */
    constructor(
        clientSecret: String
    ) : this(
        PaymentSheetActivityStarter.Args.Guest(clientSecret)
    )

    fun confirm(activity: ComponentActivity, callback: (PaymentResult) -> Unit) {
        // TODO: Use ActivityResultContract and call callback instead of using onActivityResult
        // when androidx.activity:1.2.0 hits GA
        PaymentSheetActivityStarter(activity)
            .startForResult(args)
    }

    @Parcelize
    data class Configuration(
        /**
         * Your customer-facing business name.
         *
         * The default value is the name of your app.
         */
        var merchantDisplayName: String? = null,

        /**
         * Configuration related to the Stripe Customer making a payment.
         *
         * If set, PaymentSheet displays Google Pay as a payment option.
         */
        var googlePay: GooglePayConfiguration? = null,

        /**
         * If set, the customer can select a previously saved payment method within PaymentSheet.
         */
        var customer: CustomerConfiguration? = null
    ) : Parcelable

    @Parcelize
    data class CustomerConfiguration(
        /**
         * The identifier of the Stripe Customer object.
         * See https://stripe.com/docs/api/customers/object#customer_object-id
         */
        val id: String,

        /**
         * A short-lived token that allows the SDK to access a Customer's payment methods.
         */
        val ephemeralKeySecret: String
    ) : Parcelable

    @Parcelize
    data class GooglePayConfiguration(
        /**
         * The two-letter ISO 3166 code of the country of your business, e.g. "US"
         * See your account's country value here https://dashboard.stripe.com/settings/account
         */
        val countryCode: String
    ) : Parcelable

    @Parcelize
    internal data class Result(val status: PaymentResult) : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            return bundleOf(ActivityStarter.Result.EXTRA to this)
        }

        companion object {
            @JvmStatic
            fun fromIntent(intent: Intent?): Result? {
                return intent?.getParcelableExtra(ActivityStarter.Result.EXTRA)
            }
        }
    }
}
