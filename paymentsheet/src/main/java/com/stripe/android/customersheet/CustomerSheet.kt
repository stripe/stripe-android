package com.stripe.android.customersheet

import android.app.Application
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.ExperimentalCardBrandFilteringApi
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.data.CustomerSheetSession
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.customersheet.util.getDefaultPaymentMethodsEnabledForCustomerSheet
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.CardBrandAcceptance
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.utils.AnimationConstants
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.parcelize.Parcelize

/**
 * A drop-in class that presents a bottom sheet to manage a customer's saved payment methods.
 */
class CustomerSheet internal constructor(
    private val application: Application,
    lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    viewModelStoreOwner: ViewModelStoreOwner,
    private val integrationType: CustomerSheetIntegration.Type,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val callback: CustomerSheetResultCallback,
    private val statusBarColor: () -> Int?,
) {
    private val customerSheetActivityLauncher =
        activityResultRegistryOwner.activityResultRegistry.register(
            "CustomerSheet",
            CustomerSheetContract(),
            ::onCustomerSheetResult,
        )

    private val viewModel = ViewModelProvider(
        owner = viewModelStoreOwner,
        factory = CustomerSheetConfigViewModel.Factory,
    )[CustomerSheetConfigViewModel::class.java]

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    customerSheetActivityLauncher.unregister()
                    super.onDestroy(owner)
                }
            }
        )
    }

    /**
     * Configures [CustomerSheet] with to use a given [CustomerSheet.Configuration] instance. Can be called multiple
     * times to re-configure [CustomerSheet].
     */
    fun configure(
        configuration: Configuration,
    ) {
        viewModel.configureRequest = CustomerSheetConfigureRequest(
            configuration = configuration,
        )
    }

    /**
     * Presents a sheet to manage the customer. Results of the sheet are delivered through the callback
     * passed in [CustomerSheet.create].
     */
    fun present() {
        val request = viewModel.configureRequest ?: run {
            callback.onCustomerSheetResult(
                CustomerSheetResult.Failed(
                    IllegalStateException(
                        "Must call `configure` first before attempting to present `CustomerSheet`!"
                    )
                )
            )

            return
        }

        val args = CustomerSheetContract.Args(
            configuration = request.configuration,
            integrationType = integrationType,
            statusBarColor = statusBarColor(),
        )

        val options = ActivityOptionsCompat.makeCustomAnimation(
            application.applicationContext,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        customerSheetActivityLauncher.launch(args, options)
    }

    /**
     * Clears the customer session state. This is only needed in a single activity workflow. You
     * should call this when leaving the customer management workflow in your single activity app.
     * Otherwise, if your app is using a multi-activity workflow, then [CustomerSheet] will work out
     * of the box and clearing manually is not required.
     */
    fun resetCustomer() {
        viewModel.configureRequest = null
        CustomerSheetHacks.clear()
    }

    /**
     * Retrieves the customer's saved payment option selection or null if the customer does not have
     * a persisted payment option selection.
     */
    suspend fun retrievePaymentOptionSelection(): CustomerSheetResult {
        val configuration = (viewModel.configureRequest
            ?: return CustomerSheetResult.Failed(
                IllegalStateException(
                    "Must call `configure` first before attempting to fetch the saved payment option!"
                )
            )).configuration

        return coroutineScope {
            val customerSheetSession =
                CustomerSheetHacks.initializationDataSource.await().loadCustomerSheetSession(
                    configuration
                )

            return@coroutineScope customerSheetSession.toResult().fold(
                onSuccess = { loadedCustomerSheetSession ->
                    if (getDefaultPaymentMethodsEnabledForCustomerSheet(
                            loadedCustomerSheetSession.elementsSession
                    )) {
                        useDefaultPaymentMethodFromBackend(loadedCustomerSheetSession)
                    } else {
                        val savedSelection = loadedCustomerSheetSession.savedSelection
                        useLocalUserSelection(
                            configuration,
                            savedSelection,
                            loadedCustomerSheetSession.paymentMethods
                        )
                    }
                },
            onFailure = { _ ->
                // TODO: add comment about how if we don't know whether default PMs are enabled, we need to act as though they are not.
                useLocalUserSelection(configuration, null, paymentMethods = emptyList())
                },
            )
        }
    }

    private fun useDefaultPaymentMethodFromBackend(loadedCustomerSheetSession: CustomerSheetSession): CustomerSheetResult.Selected {
        val paymentMethods = loadedCustomerSheetSession.paymentMethods
        val defaultPaymentMethod = paymentMethods.find { paymentMethod ->
            paymentMethod.id == loadedCustomerSheetSession.defaultPaymentMethodId
        } ?: paymentMethods.firstOrNull()

        return CustomerSheetResult.Selected(
            defaultPaymentMethod?.let {
                PaymentSelection.Saved(it).toPaymentOptionSelection(
                    paymentOptionFactory,
                    canUseGooglePay = false, // Not supported when using default PMs.
                )
            }
        )
    }

    private suspend fun useLocalUserSelection(
        configuration: Configuration,
        customerSessionSavedSelection: SavedSelection?,
        paymentMethods: List<PaymentMethod>,
    ): CustomerSheetResult {
        return coroutineScope {
            val savedSelection = customerSessionSavedSelection?.let { Result.success(it) } ?:
            loadSavedSelection().await()

            val selection = savedSelection.map { selection ->
                selection?.toPaymentOption()
            }.mapCatching { paymentOption ->
                paymentOption?.toPaymentSelection {
                    paymentMethods.find {
                        it.id == paymentOption.id
                    }
                }?.toPaymentOptionSelection(paymentOptionFactory, configuration.googlePayEnabled)
            }
            return@coroutineScope selection.fold(
                onSuccess = {
                    CustomerSheetResult.Selected(it)
                },
                onFailure = { cause ->
                    CustomerSheetResult.Failed(cause)
                }
            )
        }
    }

    private suspend fun loadSavedSelection(): Deferred<Result<SavedSelection?>> {
        return coroutineScope {
            val savedSelectionDeferred = async {
                CustomerSheetHacks.savedSelectionDataSource.await().retrieveSavedSelection().toResult()
            }
            return@coroutineScope savedSelectionDeferred
        }
    }

    private fun onCustomerSheetResult(result: InternalCustomerSheetResult) {
        callback.onCustomerSheetResult(
            result.toPublicResult(paymentOptionFactory)
        )
    }

    /**
     * Configuration for [CustomerSheet]
     */
    @Parcelize
    @Poko
    class Configuration
    internal constructor(
        /**
         * Describes the appearance of [CustomerSheet].
         */
        val appearance: PaymentSheet.Appearance = ConfigurationDefaults.appearance,

        /**
         * Whether [CustomerSheet] displays Google Pay as a payment option.
         */
        val googlePayEnabled: Boolean = ConfigurationDefaults.googlePayEnabled,

        /**
         * The text to display at the top of the presented bottom sheet.
         */
        val headerTextForSelectionScreen: String? = ConfigurationDefaults.headerTextForSelectionScreen,

        /**
         * [CustomerSheet] pre-populates fields with the values provided. If
         * [PaymentSheet.BillingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod]
         * is true, these values will be attached to the payment method even if they are not
         * collected by the [CustomerSheet] UI.
         */
        val defaultBillingDetails: PaymentSheet.BillingDetails = ConfigurationDefaults.billingDetails,

        /**
         * Describes how billing details should be collected. All values default to
         * [PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic].
         * If [PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never] is
         * used for a required field for the Payment Method used while adding this payment method
         * you must provide an appropriate value as part of [defaultBillingDetails].
         */
        val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            ConfigurationDefaults.billingDetailsCollectionConfiguration,

        /**
         * Your customer-facing business name. The default value is the name of your app.
         */
        val merchantDisplayName: String,

        /**
         * A list of preferred networks that should be used to process payments made with a co-branded card if your user
         * hasn't selected a network themselves.
         *
         * The first preferred network that matches an available network will be used. If no preferred network is
         * applicable, Stripe will select the network.
         */
        val preferredNetworks: List<CardBrand> = ConfigurationDefaults.preferredNetworks,

        internal val allowsRemovalOfLastSavedPaymentMethod: Boolean =
            ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,

        internal val paymentMethodOrder: List<String> = ConfigurationDefaults.paymentMethodOrder,

        internal val cardBrandAcceptance: CardBrandAcceptance = ConfigurationDefaults.cardBrandAcceptance,
    ) : Parcelable {

        // Hide no-argument constructor init
        internal constructor(merchantDisplayName: String) : this(
            appearance = ConfigurationDefaults.appearance,
            googlePayEnabled = ConfigurationDefaults.googlePayEnabled,
            headerTextForSelectionScreen = ConfigurationDefaults.headerTextForSelectionScreen,
            defaultBillingDetails = ConfigurationDefaults.billingDetails,
            billingDetailsCollectionConfiguration = ConfigurationDefaults.billingDetailsCollectionConfiguration,
            merchantDisplayName = merchantDisplayName,
            allowsRemovalOfLastSavedPaymentMethod = ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,
        )

        fun newBuilder(): Builder {
            @OptIn(ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class)
            return Builder(merchantDisplayName)
                .appearance(appearance)
                .googlePayEnabled(googlePayEnabled)
                .headerTextForSelectionScreen(headerTextForSelectionScreen)
                .defaultBillingDetails(defaultBillingDetails)
                .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
                .allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod)
                .paymentMethodOrder(paymentMethodOrder)
        }

        class Builder internal constructor(private val merchantDisplayName: String) {
            private var appearance: PaymentSheet.Appearance = ConfigurationDefaults.appearance
            private var googlePayEnabled: Boolean = ConfigurationDefaults.googlePayEnabled
            private var headerTextForSelectionScreen: String? = ConfigurationDefaults.headerTextForSelectionScreen
            private var defaultBillingDetails: PaymentSheet.BillingDetails = ConfigurationDefaults.billingDetails
            private var billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
                ConfigurationDefaults.billingDetailsCollectionConfiguration
            private var preferredNetworks: List<CardBrand> = ConfigurationDefaults.preferredNetworks
            private var allowsRemovalOfLastSavedPaymentMethod: Boolean =
                ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod
            private var paymentMethodOrder: List<String> = ConfigurationDefaults.paymentMethodOrder
            private var cardBrandAcceptance: CardBrandAcceptance = ConfigurationDefaults.cardBrandAcceptance

            fun appearance(appearance: PaymentSheet.Appearance) = apply {
                this.appearance = appearance
            }

            fun googlePayEnabled(googlePayEnabled: Boolean) = apply {
                this.googlePayEnabled = googlePayEnabled
            }

            fun headerTextForSelectionScreen(headerTextForSelectionScreen: String?) = apply {
                this.headerTextForSelectionScreen = headerTextForSelectionScreen
            }

            fun defaultBillingDetails(details: PaymentSheet.BillingDetails) = apply {
                this.defaultBillingDetails = details
            }

            fun billingDetailsCollectionConfiguration(
                configuration: PaymentSheet.BillingDetailsCollectionConfiguration
            ) = apply {
                this.billingDetailsCollectionConfiguration = configuration
            }

            fun preferredNetworks(
                preferredNetworks: List<CardBrand>
            ) = apply {
                this.preferredNetworks = preferredNetworks
            }

            @ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
            fun allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod: Boolean) = apply {
                this.allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod
            }

            /**
             * By default, CustomerSheet will use a dynamic ordering that optimizes payment method display for the
             * customer. You can override the default order in which payment methods are displayed in PaymentSheet with
             * a list of payment method types.
             *
             * See https://stripe.com/docs/api/payment_methods/object#payment_method_object-type for the list of valid
             *  types.
             * - Example: listOf("card")
             * - Note: If you omit payment methods from this list, they’ll be automatically ordered by Stripe after the
             *  ones you provide. Invalid payment methods are ignored.
             */
            fun paymentMethodOrder(paymentMethodOrder: List<String>): Builder = apply {
                this.paymentMethodOrder = paymentMethodOrder
            }

            /**
             * By default, CustomerSheet will accept all supported cards by Stripe.
             * You can specify card brands CustomerSheet should block or allow
             * payment for by providing a list of those card brands.
             * **Note**: This is only a client-side solution.
             * **Note**: Card brand filtering is not currently supported in Link.
             */
            @ExperimentalCardBrandFilteringApi
            fun cardBrandAcceptance(
                cardBrandAcceptance: CardBrandAcceptance
            ) = apply {
                this.cardBrandAcceptance = cardBrandAcceptance
            }

            fun build() = Configuration(
                appearance = appearance,
                googlePayEnabled = googlePayEnabled,
                headerTextForSelectionScreen = headerTextForSelectionScreen,
                defaultBillingDetails = defaultBillingDetails,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                merchantDisplayName = merchantDisplayName,
                preferredNetworks = preferredNetworks,
                allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
                paymentMethodOrder = paymentMethodOrder,
                cardBrandAcceptance = cardBrandAcceptance
            )
        }

        companion object {

            @JvmStatic
            fun builder(merchantDisplayName: String): Builder {
                return Builder(merchantDisplayName)
            }
        }
    }

    /**
     * [IntentConfiguration] contains the details necessary for configuring and creating an intent
     * to attach payment methods with.
     */
    @ExperimentalCustomerSessionApi
    class IntentConfiguration internal constructor(
        internal val paymentMethodTypes: List<String>,
    ) {
        /**
         * Builder for creating a [IntentConfiguration]
         */
        @ExperimentalCustomerSessionApi
        class Builder {
            private var paymentMethodTypes = listOf<String>()

            /**
             * The payment methods types to display. If empty, we dynamically determine the
             * payment method types using your [Stripe Dashboard settings]
             * (https://dashboard.stripe.com/settings/payment_methods).
             */
            fun paymentMethodTypes(paymentMethodTypes: List<String>) = apply {
                this.paymentMethodTypes = paymentMethodTypes
            }

            /**
             * Creates the [IntentConfiguration] instance.
             */
            fun build(): IntentConfiguration {
                return IntentConfiguration(
                    paymentMethodTypes = paymentMethodTypes,
                )
            }
        }
    }

    /**
     * A [CustomerSessionClientSecret] contains the parameters necessary for claiming a
     * customer session. This will be used to securely access a customer's saved payment methods.
     */
    @Poko
    @ExperimentalCustomerSessionApi
    class CustomerSessionClientSecret internal constructor(
        internal val customerId: String,
        internal val clientSecret: String
    ) {
        @ExperimentalCustomerSessionApi
        companion object {
            /**
             * Creates an instance of a [CustomerSessionClientSecret]
             *
             * @param customerId the Stripe identifier for a customer
             * @param clientSecret the customer session client secret value
             */
            @JvmStatic
            fun create(
                customerId: String,
                clientSecret: String
            ): CustomerSessionClientSecret {
                return CustomerSessionClientSecret(
                    customerId = customerId,
                    clientSecret = clientSecret,
                )
            }
        }
    }

    /**
     * [CustomerSessionProvider] provides the necessary functions
     */
    @ExperimentalCustomerSessionApi
    abstract class CustomerSessionProvider {
        /**
         * Creates an [IntentConfiguration] that is used when configuring the intent used when
         * displaying saved payment methods to a customer.
         */
        open suspend fun intentConfiguration(): Result<IntentConfiguration> {
            return Result.success(IntentConfiguration.Builder().build())
        }

        /**
         * Provides the `SetupIntent` client secret that is used when attaching a payment method
         * to a customer.
         *
         * @param customerId the Stripe identifier of the customer. This will be equivalent to
         *    the customer identifier passed through with the [CustomerSessionClientSecret].
         */
        abstract suspend fun provideSetupIntentClientSecret(customerId: String): Result<String>

        /**
         * Provides the [CustomerSessionClientSecret] that will be claimed and used to access a
         * customer's saved payment methods.
         */
        abstract suspend fun providesCustomerSessionClientSecret(): Result<CustomerSessionClientSecret>
    }

    companion object {

        /**
         * Create a [CustomerSheet]
         *
         * @param activity The [ComponentActivity] that is presenting [CustomerSheet].
         * @param customerAdapter The bridge to communicate with your server to manage a customer.
         * @param callback called when a [CustomerSheetResult] is available.
         */
        @JvmStatic
        fun create(
            activity: ComponentActivity,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return getInstance(
                application = activity.application,
                lifecycleOwner = activity,
                viewModelStoreOwner = activity,
                activityResultRegistryOwner = activity,
                statusBarColor = { activity.window.statusBarColor },
                integration = CustomerSheetIntegration.Adapter(customerAdapter),
                callback = callback,
            )
        }

        /**
         * Create a [CustomerSheet] with `CustomerSession` support.
         *
         * @param activity The [ComponentActivity] that is presenting [CustomerSheet].
         * @param customerSessionProvider provider for providing customer session elements
         * @param callback called when a [CustomerSheetResult] is available.
         */
        @ExperimentalCustomerSessionApi
        @JvmStatic
        fun create(
            activity: ComponentActivity,
            customerSessionProvider: CustomerSessionProvider,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return getInstance(
                application = activity.application,
                lifecycleOwner = activity,
                viewModelStoreOwner = activity,
                activityResultRegistryOwner = activity,
                statusBarColor = { activity.window.statusBarColor },
                integration = CustomerSheetIntegration.CustomerSession(customerSessionProvider),
                callback = callback,
            )
        }

        /**
         * Create a [CustomerSheet]
         *
         * @param fragment The [Fragment] that is presenting [CustomerSheet].
         * @param customerAdapter The bridge to communicate with your server to manage a customer.
         * @param callback called when a [CustomerSheetResult] is available.
         */
        @JvmStatic
        fun create(
            fragment: Fragment,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return getInstance(
                application = fragment.requireActivity().application,
                lifecycleOwner = fragment,
                viewModelStoreOwner = fragment,
                activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner)
                    ?: fragment.requireActivity(),
                statusBarColor = { fragment.activity?.window?.statusBarColor },
                integration = CustomerSheetIntegration.Adapter(customerAdapter),
                callback = callback,
            )
        }

        /**
         * Create a [CustomerSheet] with `CustomerSession` support.
         *
         * @param fragment The [Fragment] that is presenting [CustomerSheet].
         * @param customerSessionProvider provider for providing customer session elements
         * @param callback called when a [CustomerSheetResult] is available.
         */
        @ExperimentalCustomerSessionApi
        @JvmStatic
        fun create(
            fragment: Fragment,
            customerSessionProvider: CustomerSessionProvider,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return getInstance(
                application = fragment.requireActivity().application,
                lifecycleOwner = fragment,
                viewModelStoreOwner = fragment,
                activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner)
                    ?: fragment.requireActivity(),
                statusBarColor = { fragment.activity?.window?.statusBarColor },
                integration = CustomerSheetIntegration.CustomerSession(customerSessionProvider),
                callback = callback,
            )
        }

        internal fun getInstance(
            application: Application,
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            statusBarColor: () -> Int?,
            integration: CustomerSheetIntegration,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            CustomerSheetHacks.initialize(
                application = application,
                lifecycleOwner = lifecycleOwner,
                integration = integration,
            )

            return CustomerSheet(
                application = application,
                viewModelStoreOwner = viewModelStoreOwner,
                lifecycleOwner = lifecycleOwner,
                activityResultRegistryOwner = activityResultRegistryOwner,
                integrationType = integration.type,
                paymentOptionFactory = PaymentOptionFactory(
                    iconLoader = PaymentSelection.IconLoader(
                        resources = application.resources,
                        imageLoader = StripeImageLoader(application),
                    ),
                    context = application,
                ),
                callback = callback,
                statusBarColor = statusBarColor,
            )
        }

        internal fun PaymentSelection?.toPaymentOptionSelection(
            paymentOptionFactory: PaymentOptionFactory,
            canUseGooglePay: Boolean,
        ): PaymentOptionSelection? {
            return when (this) {
                is PaymentSelection.GooglePay -> {
                    PaymentOptionSelection.GooglePay(
                        paymentOption = paymentOptionFactory.create(this),
                    ).takeIf {
                        canUseGooglePay
                    }
                }
                is PaymentSelection.Saved -> {
                    PaymentOptionSelection.PaymentMethod(
                        paymentMethod = this.paymentMethod,
                        paymentOption = paymentOptionFactory.create(this)
                    )
                }
                else -> null
            }
        }
    }
}
