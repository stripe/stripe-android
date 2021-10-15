package com.stripe.android.model

import com.stripe.android.StripePaymentController

fun StripeIntent.getRequestCode() = StripePaymentController.getRequestCode(this)

/**
 * Check if this [StripeIntent] needs to be refreshed until it moves out "requires_action" state.
 */
internal fun StripeIntent.shouldRefresh() =
    this is PaymentIntent &&
        REFRESHABLE_PAYMENT_METHODS.contains(this.paymentMethod?.type) &&
        this.requiresAction()

/**
 * A set of PMs that don't transfer the PI status immediately after confirmation, needs to poll
 * the refresh endpoint until the PI status transfers out of "requires_action".
 */
internal val REFRESHABLE_PAYMENT_METHODS = setOf(
    PaymentMethod.Type.WeChatPay
)
