package com.stripe.android.customersheet

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.customersheet.injection.CustomerSheetComponent
import com.stripe.android.paymentsheet.PaymentSheet
import javax.inject.Inject

/**
 * 🏗 This feature is under construction 🏗
 *
 * [CustomerSheet] A class that presents a bottom sheet to manage a customer through the
 * [CustomerAdapter].
 */
@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CustomerSheet @Inject internal constructor(
    activityResultCaller: ActivityResultCaller,
    private val callback: CustomerSheetResultCallback,
) {

    private val customerSheetActivityLauncher = activityResultCaller.registerForActivityResult(
        CustomerSheetContract(),
        ::onCustomerSheetResult
    )

    /**
     * Presents a sheet to manage the customer through a [CustomerAdapter]. Results of the sheet
     * are delivered through the callback passed in [CustomerSheet.create].
     */
    fun present() {
        customerSheetActivityLauncher.launch(
            CustomerSheetContract.Args
        )
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

    private fun onCustomerSheetResult(result: InternalCustomerSheetResult?) {
        requireNotNull(result)
        callback.onResult(result.toPublicResult())
    }

    /**
     * Configuration for [CustomerSheet]
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    ) {
        @ExperimentalCustomerSheetApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Builder {
            private var appearance: PaymentSheet.Appearance = PaymentSheet.Appearance()
            private var googlePayEnabled: Boolean = false
            private var headerTextForSelectionScreen: String? = null

            fun appearance(appearance: PaymentSheet.Appearance) = apply {
                this.appearance = appearance
            }

            fun googlePayEnabled(googlePayConfiguration: Boolean) = apply {
                this.googlePayEnabled = googlePayConfiguration
            }

            fun headerTextForSelectionScreen(headerTextForSelectionScreen: String?) = apply {
                this.headerTextForSelectionScreen = headerTextForSelectionScreen
            }

            fun build() = Configuration(
                appearance = appearance,
                googlePayEnabled = googlePayEnabled,
                headerTextForSelectionScreen = headerTextForSelectionScreen,
            )
        }
    }

    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        /**
         * Create a [CustomerSheet]
         *
         * @param activity The [Activity] that is presenting [CustomerSheet].
         * @param configuration The [Configuration] options used to render the [CustomerSheet].
         * @param customerAdapter The bridge to communicate with your server to manage a customer.
         * @param callback called when a [CustomerSheetResult] is available.
         */
        fun create(
            activity: ComponentActivity,
            configuration: Configuration,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return getInstance(
                viewModelStoreOwner = activity,
                activityResultCaller = activity,
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
        fun create(
            fragment: Fragment,
            configuration: Configuration,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return getInstance(
                viewModelStoreOwner = fragment,
                activityResultCaller = fragment,
                configuration = configuration,
                customerAdapter = customerAdapter,
                callback = callback,
            )
        }

        private fun getInstance(
            viewModelStoreOwner: ViewModelStoreOwner,
            activityResultCaller: ActivityResultCaller,
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
            )

            val customerSheetComponent: CustomerSheetComponent =
                customerSessionComponent.customerSheetComponentBuilder
                    .activityResultCaller(activityResultCaller)
                    .build()

            return customerSheetComponent.customerSheet
        }
    }
}
