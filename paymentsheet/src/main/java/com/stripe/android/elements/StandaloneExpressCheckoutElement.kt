package com.stripe.android.elements

import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.elements.ece.ExpressCheckoutElementInteractor
import com.stripe.android.elements.ece.StandaloneExpressCheckoutElementController
import com.stripe.android.elements.ece.createStandaloneExpressCheckoutElement
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.state.PaymentElementLoader

/**
 * Owns configuration and confirmation for an [ExpressCheckoutElement] without a Checkout Session.
 * Reconfigure this wrapper to load a new intent or update the wallet configuration.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StandaloneExpressCheckoutElement internal constructor(
    private val controller: StandaloneExpressCheckoutElementController,
    interactor: ExpressCheckoutElementInteractor,
) {
    /** The display-only element backed by this standalone integration. */
    val expressCheckoutElement: ExpressCheckoutElement = ExpressCheckoutElement(interactor)

    /** Loads wallet availability and ordering for an existing PaymentIntent. */
    suspend fun configureWithPaymentIntent(
        clientSecret: String,
        configuration: Configuration,
    ): Result<Unit> = controller.configure(
        PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret),
        configuration,
    )

    /** Loads wallet availability and ordering for an existing SetupIntent. */
    suspend fun configureWithSetupIntent(
        clientSecret: String,
        configuration: Configuration,
    ): Result<Unit> = controller.configure(
        PaymentElementLoader.InitializationMode.SetupIntent(clientSecret),
        configuration,
    )

    /**
     * Loads wallet availability and ordering before creating an intent.
     *
     * Supply [Builder.createIntentCallback] to create the PaymentIntent or SetupIntent on your
     * server after the customer supplies their wallet details. The returned intent must match
     * [intentConfiguration]. The SDK confirms the intent and handles any required authentication.
     */
    suspend fun configure(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: Configuration,
    ): Result<Unit> = controller.configure(
        PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration),
        configuration,
    )

    /** Creates an Express Checkout Element without a Checkout Session. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder(
        internal val resultCallback: PaymentSheetResultCallback,
    ) {
        internal var createIntentCallback: CreateIntentCallback? = null
            private set
        internal var googlePayPaymentDataUpdateCallback: GooglePayPaymentDataUpdateCallback? = null
            private set

        /** Creates an intent on your server when using deferred [configure]. */
        fun createIntentCallback(callback: CreateIntentCallback): Builder = apply {
            createIntentCallback = callback
        }

        /**
         * Handles Google Pay sheet updates, such as a shipping address change.
         *
         * Update your server-side order before returning the new transaction total or an error.
         * For deferred integrations, create the intent using that updated order in
         * [createIntentCallback]. This callback does not create or update intents for you.
         */
        fun googlePayPaymentDataUpdateCallback(callback: GooglePayPaymentDataUpdateCallback): Builder = apply {
            googlePayPaymentDataUpdateCallback = callback
        }

        /**
         * Call during activity initialization, before the activity is started.
         * Use a distinct, stable [key] for each element when hosting multiple elements.
         */
        fun build(
            activity: ComponentActivity,
            key: String,
        ): StandaloneExpressCheckoutElement = createStandaloneExpressCheckoutElement(activity, this, key)
    }

    /** Configuration for an Express Checkout Element used without a Checkout Session. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration(
        internal val merchantDisplayName: String,
    ) {
        internal var googlePay: PaymentSheet.GooglePayConfiguration? = null
            private set
        internal var elementConfiguration: ExpressCheckoutElement.Configuration = ExpressCheckoutElement.Configuration()
            private set
        internal var billingDetails: PaymentSheet.BillingDetails? = null
            private set
        internal var shippingAddressParameters: GooglePayJsonFactory.ShippingAddressParameters? = null
            private set
        internal var shippingAddressRequired: Boolean = false
            private set

        /** Enables Google Pay. A currency code is required for SetupIntent integrations. */
        fun googlePay(configuration: PaymentSheet.GooglePayConfiguration): Configuration = apply {
            googlePay = configuration
        }

        /** Configures wallet visibility, ordering, and button appearance. */
        fun elementConfiguration(configuration: ExpressCheckoutElement.Configuration): Configuration = apply {
            elementConfiguration = configuration
        }

        /** Supplies default billing details to wallet flows. */
        fun defaultBillingDetails(details: PaymentSheet.BillingDetails): Configuration = apply {
            billingDetails = details
        }

        /** Requests a shipping address in Google Pay. Link is hidden when shipping is required. */
        fun shippingAddress(
            required: Boolean,
            allowedCountryCodes: Set<String>,
        ): Configuration = apply {
            shippingAddressRequired = required
            shippingAddressParameters = GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = required,
                allowedCountryCodes = allowedCountryCodes,
            )
        }
    }
}
