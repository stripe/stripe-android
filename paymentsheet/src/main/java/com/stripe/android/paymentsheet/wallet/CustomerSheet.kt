package com.stripe.android.paymentsheet.wallet

import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CustomerAdapter

/**
 * 🏗 This feature is under construction 🏗
 *
 * [CustomerSheet] A class that presents a bottom sheet to manage a customer's
 * saved payment methods.
 */
@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CustomerSheet internal constructor() {

    /**
     * Presents the saved payment methods sheet for the customer. Results are delivered to the
     * callback that the sheet was created with.
     */
    fun present() {
        TODO()
    }

    /**
     * Configuration for [CustomerSheet]
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration(
        /**
         * Your customer-facing business name.
         *
         * The default value is the name of your app.
         */
        val merchantDisplayName: String,

        /**
         * Describes the appearance of [CustomerSheet].
         */
        val appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),

        /**
         * Configuration for GooglePay.
         *
         * If set, PaymentSheet displays Google Pay as a payment option.
         */
        val googlePayConfiguration: PaymentSheet.GooglePayConfiguration? = null,

        /**
         * The text to display at the top of the presented bottom sheet.
         */
        val headerTextForSelectionScreen: String? = null,
    )

    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun create(
            activity: ComponentActivity,
            configuration: Configuration,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return CustomerSheet()
        }

        @Suppress("UNUSED_PARAMETER")
        fun create(
            fragment: Fragment,
            configuration: Configuration,
            customerAdapter: CustomerAdapter,
            callback: CustomerSheetResultCallback,
        ): CustomerSheet {
            return CustomerSheet()
        }
    }
}
