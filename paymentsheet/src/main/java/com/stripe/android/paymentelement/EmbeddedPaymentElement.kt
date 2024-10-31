package com.stripe.android.paymentelement

import androidx.annotation.RestrictTo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import com.stripe.android.paymentsheet.PaymentSheet
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalEmbeddedPaymentElementApi
class EmbeddedPaymentElement internal constructor() {
    /**
     * Contains information about the customer's selected payment option.
     * Use this to display the payment option in your own UI.
     */
    val paymentOption: StateFlow<PaymentOptionDisplayData?> = MutableStateFlow(null)

    /**
     * Call this method to initialize [EmbeddedPaymentElement] or when the IntentConfiguration values you used to
     *  initialize [EmbeddedPaymentElement] (amount, currency, etc.) change.
     *
     * This ensures the appropriate payment methods are displayed, collect the right fields, etc.
     * - Note: Upon completion, [paymentOption] may become null if it's no longer available.
     * - Note: If you call [configure] while a previous call to [configure] is still in progress, the previous call
     *      returns [ConfigureResult.Cancelled].
     */
    suspend fun configure(): ConfigureResult {
        return ConfigureResult.Failed(IllegalStateException("Not implemented."))
    }

    /**
     * A composable function that displays payment methods.
     *
     * It can present a sheet to collect more details or display saved payment methods.
     */
    @Composable
    fun Content() {
        Text("Hello World!")
    }

    /**
     * The result of an [configure] call.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ExperimentalEmbeddedPaymentElementApi
    sealed interface ConfigureResult {
        /**
         * The configure succeeded.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @ExperimentalEmbeddedPaymentElementApi
        class Succeeded internal constructor() : ConfigureResult

        /**
         * The configure was cancelled. This is only returned when a subsequent configure call cancels previous ones.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @ExperimentalEmbeddedPaymentElementApi
        class Cancelled internal constructor() : ConfigureResult

        /**
         * The configure call failed e.g. due to network failure or because of an invalid IntentConfiguration.
         *
         * Your integration should retry with exponential backoff.
         */
        @Poko
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @ExperimentalEmbeddedPaymentElementApi
        class Failed internal constructor(val error: Exception) : ConfigureResult
    }

    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ExperimentalEmbeddedPaymentElementApi
    class PaymentOptionDisplayData internal constructor(
        /**
         * An image representing a payment method; e.g. the Google Pay logo or a VISA logo.
         */
        val iconPainter: Painter,

        /**
         * A user facing string representing the payment method; e.g. "Google Pay" or "···· 4242" for a card.
         */
        val label: String,

        /**
         * The billing details associated with the customer's desired payment method.
         */
        val billingDetails: PaymentSheet.BillingDetails?,

        /**
         * A string representation of the customer's desired payment method:
         * - If this is a Stripe payment method, see
         *      https://stripe.com/docs/api/payment_methods/object#payment_method_object-type for possible values.
         * - If this is an external payment method, see
         *      https://stripe.com/docs/payments/external-payment-methods?platform=ios#available-external-payment-methods
         *      for possible values.
         * - If this is Apple Pay, the value is "apple_pay".
         */
        val paymentMethodType: String,

        /**
         * If you set configuration.hidesMandateText = true, this text must be displayed to the customer near your “Buy”
         *  button to comply with regulations.
         */
        val mandateText: AnnotatedString?,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @ExperimentalEmbeddedPaymentElementApi
        fun create(): EmbeddedPaymentElement {
            return EmbeddedPaymentElement()
        }
    }
}
