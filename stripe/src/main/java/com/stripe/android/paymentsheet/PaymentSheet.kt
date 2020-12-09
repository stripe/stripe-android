package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import com.stripe.android.paymentsheet.model.PaymentOption
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
        configuration: Configuration
    ) : this(
        PaymentSheetActivityStarter.Args(
            paymentIntentClientSecret,
            configuration
        )
    )

    /**
     * Create PaymentSheet without a Customer
     */
    constructor(
        paymentIntentClientSecret: String
    ) : this(
        PaymentSheetActivityStarter.Args(
            paymentIntentClientSecret,
            config = null
        )
    )

    fun present(activity: ComponentActivity) {
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
         * The Google Pay environment to use.
         *
         * See https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment for more information.
         */
        val environment: Environment,
        /**
         * The two-letter ISO 3166 code of the country of your business, e.g. "US"
         * See your account's country value here https://dashboard.stripe.com/settings/account
         */
        val countryCode: String
    ) : Parcelable {
        enum class Environment {
            Production,
            Test
        }
    }

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

    interface FlowController {
        fun presentPaymentOptions(
            activity: ComponentActivity,
            onComplete: (PaymentOption?) -> Unit
        )

        fun onPaymentOptionResult(intent: Intent?): PaymentOption?

        fun confirmPayment(
            activity: ComponentActivity,
            onComplete: (PaymentResult) -> Unit
        )

        sealed class Result {
            class Success(
                val flowController: FlowController
            ) : Result()

            class Failure(
                val error: Throwable
            ) : Result()
        }

        companion object {
            fun create(
                context: Context,
                clientSecret: String,
                configuration: PaymentSheet.Configuration,
                onComplete: (Result) -> Unit
            ) {
                PaymentSheetFlowControllerFactory(context).create(
                    clientSecret,
                    configuration,
                    onComplete
                )
            }

            fun create(
                context: Context,
                clientSecret: String,
                onComplete: (Result) -> Unit
            ) {
                PaymentSheetFlowControllerFactory(context).create(
                    clientSecret,
                    onComplete
                )
            }
        }
    }
}
