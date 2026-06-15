package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptions
import dev.drewhamilton.poko.Poko

/**
 * A [CheckoutSession] tracks the process of collecting a payment from your customer.
 *
 * - [Checkout Sessions Overview](https://docs.stripe.com/payments/checkout)
 * - [Checkout Sessions API Reference](https://docs.stripe.com/api/checkout/sessions)
 */
@Poko
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckoutSession internal constructor(
    /**
     * The checkout session ID (e.g., "cs_xxx").
     */
    val id: String,
    /**
     * The status of the [CheckoutSession] (open, complete, or expired).
     */
    val status: Status,
    /**
     * Whether this checkout session was created in live mode.
     */
    val liveMode: Boolean,
    /**
     * The three-letter ISO currency code (e.g., "usd").
     */
    val currency: String,
    /**
     * The customer's email address from the checkout session.
     */
    val customerEmail: String?,
    /**
     * The tax computation status for this checkout session.
     */
    val tax: Tax,
    /**
     * Summary of totals including subtotal, discounts, taxes, and shipping.
     */
    val totalSummary: TotalSummary?,
    /**
     * The products or services being purchased in this checkout session.
     */
    val lineItems: List<LineItem>,
    /**
     * Available shipping options for this checkout session.
     */
    val shippingOptions: List<ShippingRate>,
    internal val currencySelectorOptions: CurrencySelectorOptions?,
) {

    /**
     * The status of a checkout session.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Status {
        /**
         * The checkout session is still in progress. Payment processing has not started.
         */
        Open,

        /**
         * The checkout session is complete. Payment processing may still be in progress.
         */
        Complete,

        /**
         * The checkout session has expired. No further processing will occur.
         */
        Expired,

        /**
         * A status not recognized by this version of the SDK.
         */
        Unknown,
    }

    /**
     * Tax computation state for a checkout session.
     */
    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Tax internal constructor(
        /**
         * The current tax computation status.
         */
        val status: Status,
    ) {
        /**
         * The status of tax computation.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class Status {
            /**
             * The final tax amount is computed, and the session is ready for confirmation.
             */
            Ready,

            /**
             * A shipping address must be provided to calculate tax.
             */
            RequiresShippingAddress,

            /**
             * A billing address must be provided to calculate tax.
             */
            RequiresBillingAddress,

            /**
             * A tax status not recognized by this version of the SDK.
             */
            Unknown,
        }
    }

    /**
     * Summary of all totals for the checkout session.
     */
    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class TotalSummary internal constructor(
        /**
         * The subtotal before discounts, taxes, and shipping.
         */
        val subtotal: Long,
        /**
         * The amount due today, accounting for applied balances.
         */
        val totalDueToday: Long,
        /**
         * The total amount due including all charges.
         */
        val totalAmountDue: Long,
        /**
         * Discounts applied to the checkout session.
         */
        val discountAmounts: List<DiscountAmount>,
        /**
         * Tax amounts applied to the checkout session.
         */
        val taxAmounts: List<TaxAmount>,
        /**
         * The selected shipping rate, if any.
         */
        val shippingRate: ShippingRate?,
        /**
         * The customer's account balance applied to this session, if any.
         */
        val appliedBalance: Long?,
    )

    /**
     * A discount applied to the checkout session.
     */
    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class DiscountAmount internal constructor(
        /**
         * The discount amount in the smallest currency unit.
         */
        val amount: Long,
        /**
         * The display name of the discount.
         */
        val displayName: String,
    )

    /**
     * A tax amount applied to the checkout session.
     */
    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class TaxAmount internal constructor(
        /**
         * The tax amount in the smallest currency unit.
         */
        val amount: Long,
        /**
         * Whether this tax is inclusive (already included in the price).
         */
        val inclusive: Boolean,
        /**
         * The display name of the tax (e.g., "Sales Tax").
         */
        val displayName: String,
        /**
         * The tax rate as a percentage (e.g., 8.25).
         */
        val percentage: Double,
    )

    /**
     * A shipping rate option for the checkout session.
     */
    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class ShippingRate internal constructor(
        /**
         * The shipping rate ID.
         */
        val id: String,
        /**
         * The shipping amount in the smallest currency unit.
         */
        val amount: Long,
        /**
         * The display name of the shipping option (e.g., "Standard Shipping").
         */
        val displayName: String,
        /**
         * The estimated delivery time, if available (e.g., "3-5 business days").
         */
        val deliveryEstimate: String?,
    )

    /**
     * A line item in the checkout session.
     */
    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class LineItem internal constructor(
        /**
         * The line item ID.
         */
        val id: String,
        /**
         * The display name of the item.
         */
        val name: String,
        /**
         * The quantity of this item.
         */
        val quantity: Int,
        /**
         * The unit price in the smallest currency unit, if available.
         */
        val unitAmount: Long?,
        /**
         * The subtotal before discounts and taxes.
         */
        val subtotal: Long,
        /**
         * The total after discounts and taxes.
         */
        val total: Long,
    )
}
