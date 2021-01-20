package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.parcelize.Parcelize

internal class PaymentSheet internal constructor(
    private val paymentSheetLauncher: PaymentSheetLauncher
) {
    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(activity, callback)
    )

    /**
     * Create PaymentSheet with a Customer
     */
    fun present(
        paymentIntentClientSecret: String,
        configuration: Configuration
    ) {
        paymentSheetLauncher.present(paymentIntentClientSecret, configuration)
    }

    /**
     * Create PaymentSheet without a Customer
     */
    fun present(
        paymentIntentClientSecret: String
    ) {
        paymentSheetLauncher.present(paymentIntentClientSecret)
    }

    @Parcelize
    data class Configuration(
        /**
         * Your customer-facing business name.
         *
         * The default value is the name of your app.
         */
        var merchantDisplayName: String,

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
        var billingAddressCollection: BillingAddressCollectionLevel = BillingAddressCollectionLevel.Automatic,

        /**
         * If set, the customer can select a previously saved payment method within PaymentSheet.
         */
        var customer: CustomerConfiguration? = null
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

        fun init(
            paymentIntentClientSecret: String,
            configuration: Configuration,
            callback: InitCallback
        )

        fun init(
            paymentIntentClientSecret: String,
            callback: InitCallback
        )

        fun presentPaymentOptions()

        fun confirmPayment()

        fun isPaymentResult(
            requestCode: Int,
            intent: Intent?
        ): Boolean

        fun onPaymentResult(
            requestCode: Int,
            intent: Intent?,
            callback: PaymentSheetResultCallback
        )

        sealed class Result {
            object Success : Result()

            class Failure(
                val error: Throwable
            ) : Result()
        }

        fun interface InitCallback {
            fun onInit(
                success: Boolean,
                error: Throwable?
            )
        }

        companion object {
            fun create(
                activity: ComponentActivity,
                paymentOptionCallback: PaymentOptionCallback,
                paymentResultCallback: PaymentSheetResultCallback
            ): FlowController {
                return FlowControllerFactory(
                    activity,
                    PaymentConfiguration.getInstance(activity),
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }
        }
    }
}
