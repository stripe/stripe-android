package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Response from checkout session APIs:
 * - Init API (`/v1/payment_pages/{cs_id}/init`) - returns [elementsSession]
 * - Confirm API (`/v1/payment_pages/{cs_id}/confirm`) - returns [paymentIntent]
 *
 * For init responses, [elementsSession] contains payment method preferences, Link settings,
 * customer data, and other configuration needed by PaymentSheet.
 * For confirm responses, [paymentIntent] contains the confirmed payment intent.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class CheckoutSessionResponse(
    /**
     * The checkout session ID (e.g., "cs_xxx").
     */
    val id: String,

    /**
     * The payment amount in the smallest currency unit (e.g., cents for USD).
     */
    val amount: Long,

    /**
     * The three-letter ISO currency code (e.g., "usd").
     */
    val currency: String,

    /**
     * The embedded ElementsSession containing payment method preferences, Link settings,
     * customer data, and other configuration needed by PaymentSheet.
     * Only populated in responses from the init API.
     */
    val elementsSession: ElementsSession? = null,

    /**
     * The PaymentIntent created/confirmed during checkout session confirmation.
     * Only populated in responses from the confirm API.
     */
    val paymentIntent: PaymentIntent? = null,

    /**
     * Customer data from the checkout session init response.
     * This is parsed from the top-level "customer" field in the init response.
     * For checkout sessions, customer is associated server-side when the session is created,
     * so we get customer data directly in the init response rather than through customer session auth.
     */
    val customer: Customer? = null,

    /**
     * Server-side flag controlling the "Save for future use" checkbox.
     * Parsed from `customer_managed_saved_payment_methods_offer_save` in the init response.
     */
    val savedPaymentMethodsOfferSave: SavedPaymentMethodsOfferSave? = null,
) : StripeModel {

    /**
     * Controls whether the "Save for future use" checkbox is shown and its initial state.
     *
     * This data comes from the checkout session's `customer_managed_saved_payment_methods_offer_save`
     * configuration, which is set when creating the checkout session.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class SavedPaymentMethodsOfferSave(
        /**
         * Whether the save checkbox should be shown to the user.
         */
        val enabled: Boolean,
        /**
         * The initial state of the checkbox.
         */
        val status: Status,
    ) : StripeModel {
        /**
         * Represents the initial checked state of the save checkbox.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class Status {
            /**
             * Checkbox should be pre-checked (user has previously agreed to save).
             */
            ACCEPTED,

            /**
             * Checkbox should be unchecked by default.
             */
            NOT_ACCEPTED,
        }
    }

    /**
     * Customer data from checkout session.
     *
     * This is simpler than [ElementsSession.Customer] because checkout sessions don't use
     * customer session authentication - the customer is associated server-side when the
     * checkout session is created.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Customer(
        /**
         * The customer ID (e.g., "cus_xxx").
         */
        val id: String,

        /**
         * The customer's saved payment methods.
         */
        val paymentMethods: List<PaymentMethod>,
    ) : StripeModel
}
