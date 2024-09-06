package com.stripe.android.customersheet

import android.app.Application
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.utils.AnimationConstants
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * 🏗 This feature is in private beta and could change 🏗
 *
 * [CustomerSheet] A class that presents a bottom sheet to manage a customer through the
 * [CustomerAdapter].
 */
@ExperimentalCustomerSheetApi
class CustomerSheet @Inject internal constructor(
    private val application: Application,
    lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val callback: CustomerSheetResultCallback,
    private val statusBarColor: () -> Int?,
) {
    private var configureRequest: ConfigureRequest? = null

    private val customerSheetActivityLauncher =
        activityResultRegistryOwner.activityResultRegistry.register(
            "CustomerSheet",
            CustomerSheetContract(),
            ::onCustomerSheetResult,
        )

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
        this.configureRequest = ConfigureRequest(
            configuration = configuration,
        )
    }

    /**
     * Presents a sheet to manage the customer through a [CustomerAdapter]. Results of the sheet
     * are delivered through the callback passed in [CustomerSheet.create].
     */
    fun present() {
        val request = configureRequest ?: run {
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
        CustomerSheetHacks.clear()
    }

    /**
     * Retrieves the customer's saved payment option selection or null if the customer does not have
     * a persisted payment option selection.
     */
    suspend fun retrievePaymentOptionSelection(): CustomerSheetResult = coroutineScope {
        val request = configureRequest
            ?: return@coroutineScope CustomerSheetResult.Failed(
                IllegalStateException(
                    "Must call `configure` first before attempting to fetch the saved payment option!"
                )
            )

        val adapter = CustomerSheetHacks.adapter.await()

        val selectedPaymentOptionDeferred = async {
            adapter.retrieveSelectedPaymentOption()
        }
        val paymentMethodsDeferred = async {
            adapter.retrievePaymentMethods()
        }
        val selectedPaymentOption = selectedPaymentOptionDeferred.await()
        val paymentMethods = paymentMethodsDeferred.await()

        val selection = selectedPaymentOption.mapCatching { paymentOption ->
            paymentOption?.toPaymentSelection {
                paymentMethods.getOrNull()?.find {
                    it.id == paymentOption.id
                }
            }?.toPaymentOptionSelection(paymentOptionFactory, request.configuration.googlePayEnabled)
        }

        selection.fold(
            onSuccess = {
                CustomerSheetResult.Selected(it)
            },
            onFailure = { cause, _ ->
                CustomerSheetResult.Failed(cause)
            }
        )
    }

    private fun onCustomerSheetResult(result: InternalCustomerSheetResult) {
        callback.onCustomerSheetResult(
            result.toPublicResult(paymentOptionFactory)
        )
    }

    /**
     * Configuration for [CustomerSheet]
     */
    @ExperimentalCustomerSheetApi
    @Parcelize
    @Poko
    class Configuration internal constructor(
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

        @ExperimentalCustomerSheetApi
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

            fun appearance(appearance: PaymentSheet.Appearance) = apply {
                this.appearance = appearance
            }

            fun googlePayEnabled(googlePayConfiguration: Boolean) = apply {
                this.googlePayEnabled = googlePayConfiguration
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
            )
        }

        companion object {

            @JvmStatic
            fun builder(merchantDisplayName: String): Builder {
                return Builder(merchantDisplayName)
            }
        }
    }

    private data class ConfigureRequest(
        val configuration: Configuration,
    )

    @ExperimentalCustomerSheetApi
    companion object {

        /**
         * Create a [CustomerSheet]
         *
         * @param activity The [Activity] that is presenting [CustomerSheet].
         * @param configuration The [Configuration] options used to render the [CustomerSheet].
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
                activityResultRegistryOwner = activity,
                statusBarColor = { activity.window.statusBarColor },
                customerAdapter = customerAdapter,
                callback = callback,
            )
        }

        /**
         * Create a [CustomerSheet]
         *
         * @param fragment The [Fragment] that is presenting [CustomerSheet].
         * @param configuration The [Configuration] options used to render the [CustomerSheet].
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
                activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner)
                    ?: fragment.requireActivity(),
                statusBarColor = { fragment.activity?.window?.statusBarColor },
                customerAdapter = customerAdapter,
                callback = callback,
            )
        }

        internal fun getInstance(
            application: Application,
            lifecycleOwner: LifecycleOwner,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            statusBarColor: () -> Int?,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            CustomerSheetHacks.initialize(
                lifecycleOwner = lifecycleOwner,
                adapter = customerAdapter,
            )

            return CustomerSheet(
                application = application,
                lifecycleOwner = lifecycleOwner,
                activityResultRegistryOwner = activityResultRegistryOwner,
                paymentOptionFactory = PaymentOptionFactory(
                    resources = application.resources,
                    imageLoader = StripeImageLoader(application),
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
