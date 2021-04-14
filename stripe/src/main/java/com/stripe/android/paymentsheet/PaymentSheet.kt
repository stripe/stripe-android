package com.stripe.android.paymentsheet

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.parcelize.Parcelize

class PaymentSheet internal constructor(
    private val paymentSheetLauncher: PaymentSheetLauncher
) {
    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(activity, callback)
    )

    constructor(
        fragment: Fragment,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(fragment, callback)
    )

    /**
     * Create PaymentSheet with a [Configuration].
     *
     * @param[intentClientSecret] the client secret for a [PaymentIntent] or [SetupIntent] object.
     * If [intentClientSecret] represents a [PaymentIntent] or [SetupIntent] that is already
     * confirmed, [PaymentSheetResultCallback] will be invoked with [PaymentSheetResult.Completed].
     */
    fun present(
        intentClientSecret: String,
        configuration: Configuration
    ) {
        paymentSheetLauncher.present(intentClientSecret, configuration)
    }

    /**
     * Create PaymentSheet without a [Configuration].
     *
     * @param[intentClientSecret] the client secret for a [PaymentIntent] or [SetupIntent] object.
     * If [intentClientSecret] represents a [PaymentIntent] or [SetupIntent] that is already
     * confirmed, [PaymentSheetResultCallback] will be invoked with [PaymentSheetResult.Completed].
     */
    fun present(
        intentClientSecret: String
    ) {
        paymentSheetLauncher.present(intentClientSecret)
    }

    @Parcelize
    data class Configuration @JvmOverloads constructor(
        /**
         * Your customer-facing business name.
         *
         * The default value is the name of your app.
         */
        var merchantDisplayName: String,

        /**
         * If set, the customer can select a previously saved payment method within PaymentSheet.
         */
        var customer: CustomerConfiguration? = null,

        /**
         * Configuration related to the Stripe Customer making a payment.
         *
         * If set, PaymentSheet displays Google Pay as a payment option.
         */
        var googlePay: GooglePayConfiguration? = null,

        /**
         * The amount of billing address details to collect.
         *
         * See [BillingAddressCollectionLevel]
         */
        var billingAddressCollection: BillingAddressCollectionLevel = BillingAddressCollectionLevel.Automatic
    ) : Parcelable

    enum class BillingAddressCollectionLevel {
        /**
         * (Default) PaymentSheet will only collect the necessary billing address information.
         */
        Automatic,

        /**
         * PaymentSheet will always collect full billing address details.
         */
        Required
    }

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

    interface FlowController {
        fun getPaymentOption(): PaymentOption?

        fun configure(
            intentClientSecret: String,
            configuration: Configuration,
            callback: ConfigCallback
        )

        fun configure(
            intentClientSecret: String,
            callback: ConfigCallback
        )

        fun presentPaymentOptions()

        fun confirm()

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

        companion object {
            @JvmStatic
            fun create(
                activity: ComponentActivity,
                paymentOptionCallback: PaymentOptionCallback,
                paymentResultCallback: PaymentSheetResultCallback
            ): FlowController {
                return FlowControllerFactory(
                    activity,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            @JvmStatic
            fun create(
                fragment: Fragment,
                paymentOptionCallback: PaymentOptionCallback,
                paymentResultCallback: PaymentSheetResultCallback
            ): FlowController {
                return FlowControllerFactory(
                    fragment,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }
        }
    }
}
