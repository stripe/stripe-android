package com.stripe.android.elements

import android.app.Application
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import dagger.Lazy
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton
import com.stripe.android.elements.ExpressCheckoutElement as ExpressCheckoutElementView

/**
 * Owns the intent, elements/session metadata, and confirmation lifecycle shared by its elements.
 * Configure with a PaymentIntent, SetupIntent, or deferred intent before displaying an element.
 * Reconfigure the controller when the intent or configuration changes.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ElementsController @Inject internal constructor(
    private val stateHolder: ElementsControllerStateHolder,
    private val stateLoader: ElementsStateLoader,
    private val operationCoordinator: ElementsOperationCoordinator,
    private val expressCheckoutElementProvider: Lazy<ExpressCheckoutElementView>,
    private val confirmationHandler: ConfirmationHandler,
    @PaymentElementCallbackIdentifier private val callbackIdentifier: String,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val resultCallback: PaymentSheetResultCallback,
    private val savedState: ElementsControllerSavedState,
) {
    /** The latest intent metadata, or null until configuration succeeds. */
    val session: StateFlow<Session?> = stateHolder.session

    /** Whether configuration or confirmation is in progress. Shared by all elements. */
    val isUpdating: StateFlow<Boolean> = operationCoordinator.isUpdating

    /** The display-only Express Checkout Element backed by this controller's shared state. */
    val expressCheckoutElement: ExpressCheckoutElementView
        get() = expressCheckoutElementProvider.get()

    init {
        coroutineScope.launch {
            operationCoordinator.result.collect(resultCallback::onPaymentSheetResult)
        }
    }

    /**
     * Binds payment launchers to the current activity. Call in every activity's onCreate, before it
     * starts. Launchers unregister when that activity is destroyed; the controller remains usable.
     */
    @MainThread
    fun register(activity: ComponentActivity) {
        check(coroutineScope.isActive) { "ElementsController has been destroyed." }
        operationCoordinator.statusBarColor = StatusBarCompat.color(activity)
        confirmationHandler.register(
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "ElementsController(instance = $callbackIdentifier)",
                registryOwner = activity,
            ),
            lifecycleOwner = activity,
        )
    }

    /** Cancels the controller's work and clears its callbacks and saved state. Call when its owner is done. */
    @MainThread
    fun destroy() {
        coroutineScope.cancel()
        PaymentElementCallbackReferences.remove(callbackIdentifier)
        stateHolder.state = null
        savedState.clear()
    }

    /** Loads an existing PaymentIntent and its elements/session metadata. */
    suspend fun configureWithPaymentIntent(
        clientSecret: String,
        configuration: Configuration,
    ): Result<Unit> = configure(PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret), configuration)

    /** Loads an existing SetupIntent and its elements/session metadata. */
    suspend fun configureWithSetupIntent(
        clientSecret: String,
        configuration: Configuration,
    ): Result<Unit> = configure(PaymentElementLoader.InitializationMode.SetupIntent(clientSecret), configuration)

    /**
     * Loads elements/session metadata before creating an intent.
     * Supply [Builder.createIntentCallback] to create the intent on your server after the customer
     * supplies their payment details. The SDK confirms it and handles required authentication.
     */
    suspend fun configure(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: Configuration,
    ): Result<Unit> = configure(
        PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration),
        configuration,
    )

    private suspend fun configure(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: Configuration,
    ): Result<Unit> {
        if (!coroutineScope.isActive) {
            return Result.failure(IllegalStateException("ElementsController has been destroyed."))
        }
        val task = coroutineScope.async {
            operationCoordinator.configure {
                stateLoader.load(initializationMode, configuration.build(), initializedViaCompose = false)
            }
        }
        return try {
            task.await()
        } finally {
            task.cancel()
        }
    }

    /** A projection of the loaded elements/session metadata. */
    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Session internal constructor(
        /**
         * The intent loaded from elements/session. For deferred configuration, this is a placeholder;
         * your server creates the real intent during confirmation using [Builder.createIntentCallback].
         */
        val intent: StripeIntent,
    )

    /**
     * Creates an independent controller from application context and saved state. No activity,
     * ViewModel, or external coroutine scope is required. Call [destroy] when the owner is done.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder(
        private val application: Application,
        private val savedStateHandle: SavedStateHandle,
    ) {
        internal var resultCallback: PaymentSheetResultCallback = PaymentSheetResultCallback {}
            private set
        private var integrationName: String = "stripe_elements"
        internal var createIntentCallback: CreateIntentCallback? = null
            private set
        internal var shippingAddressChangeCallback: ExpressCheckoutElement.ShippingAddressChangeCallback? = null
            private set

        /** Receives payment completion, cancellation, or failure. */
        fun resultCallback(callback: PaymentSheetResultCallback): Builder = apply {
            resultCallback = callback
        }

        /** Namespaces saved state when multiple controllers share a SavedStateHandle. */
        fun integrationName(integrationName: String): Builder = apply {
            this.integrationName = integrationName
        }

        /** Creates an intent on your server when using deferred [configure]. */
        fun createIntentCallback(callback: CreateIntentCallback): Builder = apply {
            createIntentCallback = callback
        }

        /**
         * Handles shipping address changes from any supported wallet. Update your server-side order
         * before resolving with the new total or line items, or reject the address.
         */
        fun shippingAddressChangeCallback(
            callback: ExpressCheckoutElement.ShippingAddressChangeCallback,
        ): Builder = apply {
            shippingAddressChangeCallback = callback
        }

        /** Builds a controller with its own coroutine scope. Register an activity before displaying payment UI. */
        @MainThread
        fun build(): ElementsController = createElementsController(application, savedStateHandle, this, integrationName)
    }

    /** Shipping events and order updates for the controller's Express Checkout Element. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class ExpressCheckoutElement private constructor() {
        /** Handles a shipping address selected in a wallet. The address may be redacted for privacy. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun interface ShippingAddressChangeCallback {
            suspend fun onShippingAddressChange(event: ShippingAddressChangeEvent): ShippingChangeResult
        }

        @Poko
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class ShippingAddressChangeEvent internal constructor(val address: ShippingAddress)

        /** A partial shipping address used to calculate shipping and tax before confirmation. */
        @Poko
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class ShippingAddress internal constructor(
            val country: String,
            val state: String?,
            val city: String?,
            val postalCode: String?,
        )

        /** A display line item. Amounts use the intent currency's smallest unit; discounts may be negative. */
        @Parcelize
        @Poko
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class LineItem(val name: String, val amount: Long) : Parcelable

        /** Resolve or reject a shipping change after updating the order on your server. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        sealed class ShippingChangeResult private constructor() {
            /**
             * Accepts the change. Null fields preserve the current value. An empty lineItems list clears
             * the displayed items.
             * [amount] is the new total in the intent currency's smallest unit; it does not update the intent.
             */
            @Poko
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class Resolve(
                val amount: Long?,
                val lineItems: List<LineItem>?,
            ) : ShippingChangeResult() {
                init {
                    require(amount == null || amount >= 0) { "The total amount must not be negative." }
                }
            }

            /** Rejects the address and displays [message] in the wallet. */
            @Poko
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class Reject(val message: String) : ShippingChangeResult()
        }
    }

    /** Shared intent configuration and per-element options. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration(
        private val merchantDisplayName: String,
    ) {
        private var googlePay: PaymentSheet.GooglePayConfiguration? = null
        private var expressCheckoutElementConfiguration: ExpressCheckoutElementView.Configuration? = null
        private var billingDetails: PaymentSheet.BillingDetails? = null
        private var allowedShippingCountryCodes: Set<String> = emptySet()
        private var shippingAddressRequired: Boolean = false

        /** Enables Google Pay. A currency code is required for SetupIntent integrations. */
        fun googlePay(configuration: PaymentSheet.GooglePayConfiguration): Configuration = apply {
            googlePay = configuration
        }

        /** Enables Express Checkout Element and configures its wallet visibility and appearance. */
        fun expressCheckoutElement(configuration: ExpressCheckoutElementView.Configuration): Configuration = apply {
            expressCheckoutElementConfiguration = configuration
        }

        /** Supplies default billing details to payment flows. */
        fun defaultBillingDetails(details: PaymentSheet.BillingDetails): Configuration = apply {
            billingDetails = details
        }

        /** Requests a shipping address from supported wallets. Link is hidden when shipping is required. */
        fun shippingAddress(
            required: Boolean,
            allowedCountryCodes: Set<String>,
        ): Configuration = apply {
            shippingAddressRequired = required
            allowedShippingCountryCodes = allowedCountryCodes.toSet()
        }

        internal fun build(): State = State(
            merchantDisplayName = merchantDisplayName,
            googlePay = googlePay,
            expressCheckoutElementConfiguration = expressCheckoutElementConfiguration?.build(),
            defaultBillingDetails = billingDetails,
            allowedShippingCountryCodes = allowedShippingCountryCodes,
            shippingAddressRequired = shippingAddressRequired,
        )

        @Parcelize
        internal data class State(
            val merchantDisplayName: String,
            val googlePay: PaymentSheet.GooglePayConfiguration?,
            val expressCheckoutElementConfiguration: ExpressCheckoutElementView.Configuration.State?,
            val defaultBillingDetails: PaymentSheet.BillingDetails?,
            val allowedShippingCountryCodes: Set<String>,
            val shippingAddressRequired: Boolean,
        ) : Parcelable
    }
}
