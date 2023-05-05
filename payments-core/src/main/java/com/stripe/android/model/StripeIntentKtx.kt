package com.stripe.android.model

import com.stripe.android.StripePaymentController
import com.stripe.android.model.PaymentMethod.Type.CashAppPay
import com.stripe.android.model.PaymentMethod.Type.WeChatPay

fun StripeIntent.getRequestCode() = StripePaymentController.getRequestCode(this)

/**
 * Check if this [StripeIntent] needs to be refreshed until it moves out "requires_action" state.
 */
internal fun StripeIntent.shouldRefresh(): Boolean {
    return requiresAction() && paymentMethod?.type in refreshablePaymentMethods
}

/**
 * A set of PMs that don't transfer the intent status immediately after confirmation. Needs to poll
 * the refresh endpoint until the intent status transfers out of "requires_action".
 */
private val StripeIntent.refreshablePaymentMethods: Set<PaymentMethod.Type>
    get() = when (this) {
        is PaymentIntent -> setOf(WeChatPay, CashAppPay)
        is SetupIntent -> setOf(CashAppPay)
    }
