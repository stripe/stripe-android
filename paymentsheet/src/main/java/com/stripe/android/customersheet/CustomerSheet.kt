package com.stripe.android.customersheet

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.customersheet.injection.CustomerSheetComponent
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.AnimationConstants
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
) {

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
     * Presents a sheet to manage the customer through a [CustomerAdapter]. Results of the sheet
     * are delivered through the callback passed in [CustomerSheet.create].
     */
    fun present() {
        val args = CustomerSheetContract.Args

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
        CustomerSessionViewModel.clear()
    }

    /**
     * Retrieves the customer's saved payment option selection or null if the customer does not have
     * a persisted payment option selection.
     */
    suspend fun retrievePaymentOptionSelection(): CustomerSheetResult = coroutineScope {
        val adapter = CustomerSessionViewModel.component.customerAdapter
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
            }?.toPaymentOptionSelection(paymentOptionFactory)
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
    class Configuration internal constructor(
        /**
         * Describes the appearance of [CustomerSheet].
         */
        val appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),

        /**
         * Whether [CustomerSheet] displays Google Pay as a payment option.
         */
        val googlePayEnabled: Boolean = false,

        /**
         * The text to display at the top of the presented bottom sheet.
         */
        val headerTextForSelectionScreen: String? = null,

        /**
         * [CustomerSheet] pre-populates fields with the values provided. If
         * [PaymentSheet.BillingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod]
         * is true, these values will be attached to the payment method even if they are not
         * collected by the [CustomerSheet] UI.
         */
        val defaultBillingDetails: PaymentSheet.BillingDetails = PaymentSheet.BillingDetails(),

        /**
         * Describes how billing details should be collected. All values default to
         * [PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic].
         * If [PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never] is
         * used for a required field for the Payment Method used while adding this payment method
         * you must provide an appropriate value as part of [defaultBillingDetails].
         */
        val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),

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
        val preferredNetworks: List<CardBrand> = emptyList(),
    ) {

        // Hide no-argument constructor init
        internal constructor(merchantDisplayName: String) : this(
            appearance = PaymentSheet.Appearance(),
            googlePayEnabled = false,
            headerTextForSelectionScreen = null,
            defaultBillingDetails = PaymentSheet.BillingDetails(),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            merchantDisplayName = merchantDisplayName,
        )

        fun newBuilder(): Builder {
            return Builder(merchantDisplayName)
                .appearance(appearance)
                .googlePayEnabled(googlePayEnabled)
                .headerTextForSelectionScreen(headerTextForSelectionScreen)
                .defaultBillingDetails(defaultBillingDetails)
                .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
        }

        @ExperimentalCustomerSheetApi
        class Builder internal constructor(private val merchantDisplayName: String) {
            private var appearance: PaymentSheet.Appearance = PaymentSheet.Appearance()
            private var googlePayEnabled: Boolean = false
            private var headerTextForSelectionScreen: String? = null
            private var defaultBillingDetails: PaymentSheet.BillingDetails = PaymentSheet.BillingDetails()
            private var billingDetailsCollectionConfiguration:
                PaymentSheet.BillingDetailsCollectionConfiguration =
                    PaymentSheet.BillingDetailsCollectionConfiguration()
            private var preferredNetworks: List<CardBrand> = emptyList()

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

            fun build() = Configuration(
                appearance = appearance,
                googlePayEnabled = googlePayEnabled,
                headerTextForSelectionScreen = headerTextForSelectionScreen,
                defaultBillingDetails = defaultBillingDetails,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                merchantDisplayName = merchantDisplayName,
                preferredNetworks = preferredNetworks,
            )
        }

        companion object {

            @JvmStatic
            fun builder(merchantDisplayName: String): Builder {
                return Builder(merchantDisplayName)
            }
        }
    }

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
            configuration: Configuration,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return getInstance(
                lifecycleOwner = activity,
                viewModelStoreOwner = activity,
                activityResultRegistryOwner = activity,
                statusBarColor = { activity.window.statusBarColor },
                configuration = configuration,
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
            configuration: Configuration,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return getInstance(
                lifecycleOwner = fragment,
                viewModelStoreOwner = fragment,
                activityResultRegistryOwner = (fragment.host as? ActivityResultRegistryOwner)
                    ?: fragment.requireActivity(),
                statusBarColor = { fragment.activity?.window?.statusBarColor },
                configuration = configuration,
                customerAdapter = customerAdapter,
                callback = callback,
            )
        }

        internal fun getInstance(
            lifecycleOwner: LifecycleOwner,
            viewModelStoreOwner: ViewModelStoreOwner,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            statusBarColor: () -> Int?,
            configuration: Configuration,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            val customerSessionViewModel =
                ViewModelProvider(viewModelStoreOwner)[CustomerSessionViewModel::class.java]

            val customerSessionComponent = customerSessionViewModel.createCustomerSessionComponent(
                configuration = configuration,
                customerAdapter = customerAdapter,
                callback = callback,
                statusBarColor = statusBarColor,
            )

            val customerSheetComponent: CustomerSheetComponent =
                customerSessionComponent.customerSheetComponentBuilder
                    .lifecycleOwner(lifecycleOwner)
                    .activityResultRegistryOwner(activityResultRegistryOwner)
                    .build()

            return customerSheetComponent.customerSheet
        }

        internal fun PaymentSelection?.toPaymentOptionSelection(
            paymentOptionFactory: PaymentOptionFactory
        ): PaymentOptionSelection? {
            return when (this) {
                is PaymentSelection.GooglePay -> {
                    PaymentOptionSelection.GooglePay(
                        paymentOption = paymentOptionFactory.create(this)
                    )
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
