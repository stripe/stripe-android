package com.stripe.android.paymentsheet.wallet

import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CustomerAdapter

/**
 * 🏗 This features is under construction 🏗
 *
 * [SavedPaymentMethodsSheet] A class that presents a bottom sheet to manage a customer's
 * saved payment methods.
 */
@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SavedPaymentMethodsSheet {

    /**
     * Present's the saved payment methods sheet for the customer
     */
    fun present()

    /**
     * Configuration for [SavedPaymentMethodsSheet]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Configuration(
        /**
         * Your customer-facing business name.
         *
         * The default value is the name of your app.
         */
        val merchantDisplayName: String,

        /**
         * Describes the appearance of SavedPaymentMethodsSheet
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun create(
            activity: ComponentActivity,
            customerAdapter: CustomerAdapter,
            configuration: Configuration,
            callback: SavedPaymentMethodsSheetResultCallback,
        ): SavedPaymentMethodsSheet {
            TODO()
        }

        fun create(
            fragment: Fragment,
            customerAdapter: CustomerAdapter,
            configuration: Configuration,
            callback: SavedPaymentMethodsSheetResultCallback,
        ): SavedPaymentMethodsSheet {
            TODO()
        }
    }
}
