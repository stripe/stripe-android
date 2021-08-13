package com.stripe.android.paymentsheet

import android.content.res.ColorStateList
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.parcelize.Parcelize

/**
 * A drop-in class that presents a bottom sheet to collect and process a customer's payment.
 */
class PaymentSheet internal constructor(
    private val paymentSheetLauncher: PaymentSheetLauncher
) {
    /**
     * Constructor to be used when launching the payment sheet from an Activity.
     *
     * @param activity  the Activity that is presenting the payment sheet.
     * @param callback  called with the result of the payment after the payment sheet is dismissed.
     */
    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(activity, callback)
    )

    /**
     * Constructor to be used when launching the payment sheet from a Fragment.
     *
     * @param fragment the Fragment that is presenting the payment sheet.
     * @param callback called with the result of the payment after the payment sheet is dismissed.
     */
    constructor(
        fragment: Fragment,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(fragment, callback)
    )

    /**
     * Present the payment sheet to process a [PaymentIntent].
     * If the [PaymentIntent] is already confirmed, [PaymentSheetResultCallback] will be invoked
     * with [PaymentSheetResult.Completed].
     *
     * @param paymentIntentClientSecret the client secret for the [PaymentIntent].
     * @param configuration optional [PaymentSheet] settings.
     */
    @JvmOverloads
    fun presentWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: Configuration? = null
    ) {
        paymentSheetLauncher.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }

    /**
     * Present the payment sheet to process a [SetupIntent].
     * If the [SetupIntent] is already confirmed, [PaymentSheetResultCallback] will be invoked
     * with [PaymentSheetResult.Completed].
     *
     * @param setupIntentClientSecret the client secret for the [SetupIntent].
     * @param configuration optional [PaymentSheet] settings.
     */
    @JvmOverloads
    fun presentWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: Configuration? = null
    ) {
        paymentSheetLauncher.presentWithSetupIntent(setupIntentClientSecret, configuration)
    }

    /** Configuration for [PaymentSheet] **/
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
         * The color of the Pay or Add button. Keep in mind the text color is white.
         *
         * If set, PaymentSheet displays the button with this color.
         */
        var primaryButtonColor: ColorStateList? = null,

        /**
         * The billing information for the user.
         *
         * If set, PaymentSheet will pre-populate the form fiels with the values provided.
         */
        var billingDetails: BillingDetails? = null
    ) : Parcelable

    @Parcelize
    data class Address(
        val city: String? = null,
        val country: String? = null,
        val line1: String? = null,
        val line2: String? = null,
        val postalCode: String? = null,
        val state: String? = null
    ) : Parcelable

    @Parcelize
    data class BillingDetails(
        val address: Address,
        val email: String? = null,
        val name: String? = null,
        val phone: String? = null
    ) : Parcelable

    @Parcelize
    data class CustomerConfiguration(
        /**
         * The identifier of the Stripe Customer object.
         * See [Stripe's documentation](https://stripe.com/docs/api/customers/object#customer_object-id).
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
         * See [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment) for more information.
         */
        val environment: Environment,
        /**
         * The two-letter ISO 3166 code of the country of your business, e.g. "US".
         * See your account's country value [here](https://dashboard.stripe.com/settings/account).
         */
        val countryCode: String,
        /**
         * The three-letter ISO 4217 alphabetic currency code, e.g. "USD" or "EUR".
         * Required in order to support Google Pay when processing a Setup Intent.
         */
        val currencyCode: String? = null
    ) : Parcelable {
        constructor(
            environment: Environment,
            countryCode: String
        ) : this(environment, countryCode, null)

        enum class Environment {
            Production,
            Test
        }
    }

    /**
     * A class that presents the individual steps of a payment sheet flow.
     */
    interface FlowController {

        /**
         * Configure the FlowController to process a [PaymentIntent].
         *
         * @param paymentIntentClientSecret the client secret for the [PaymentIntent].
         * @param configuration optional [PaymentSheet] settings.
         * @param callback called with the result of configuring the FlowController.
         */
        fun configureWithPaymentIntent(
            paymentIntentClientSecret: String,
            configuration: Configuration? = null,
            callback: ConfigCallback
        )

        /**
         * Configure the FlowController to process a [SetupIntent].
         *
         * @param setupIntentClientSecret the client secret for the [SetupIntent].
         * @param configuration optional [PaymentSheet] settings.
         * @param callback called with the result of configuring the FlowController.
         */
        fun configureWithSetupIntent(
            setupIntentClientSecret: String,
            configuration: Configuration? = null,
            callback: ConfigCallback
        )

        /**
         * Retrieve information about the customer's desired payment option.
         * You can use this to e.g. display the payment option in your UI.
         */
        fun getPaymentOption(): PaymentOption?

        /**
         * Present a sheet where the customer chooses how to pay, either by selecting an existing
         * payment method or adding a new one.
         * Call this when your "Select a payment method" button is tapped.
         */
        fun presentPaymentOptions()

        /**
         * Complete the payment or setup.
         */
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

            /**
             * Create the FlowController when launching the payment sheet from an Activity.
             *
             * @param activity  the Activity that is presenting the payment sheet.
             * @param paymentOptionCallback called when the customer's desired payment method
             *      changes.  Called in response to the [PaymentSheet#presentPaymentOptions()]
             * @param paymentResultCallback called when a [PaymentSheetResult] is available.
             */
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

            /**
             * Create the FlowController when launching the payment sheet from a Fragment.
             *
             * @param fragment the Fragment that is presenting the payment sheet.
             * @param paymentOptionCallback called when the customer's [PaymentOption] selection changes.
             * @param paymentResultCallback called when a [PaymentSheetResult] is available.
             */
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
