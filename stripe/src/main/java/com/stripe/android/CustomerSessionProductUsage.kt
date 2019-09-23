package com.stripe.android

import com.stripe.android.view.AddPaymentMethodActivity
import com.stripe.android.view.PaymentFlowActivity
import com.stripe.android.view.PaymentMethodsActivity

internal class CustomerSessionProductUsage {
    private val data: MutableSet<String> = mutableSetOf()

    fun add(token: String?) {
        if (token != null && VALID_TOKENS.contains(token)) {
            data.add(token)
        }
    }

    fun reset() {
        data.clear()
    }

    fun get(): Set<String> {
        return data.toSet()
    }

    companion object {
        private val VALID_TOKENS = setOf(
            AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY,
            PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY,
            PaymentFlowActivity.TOKEN_PAYMENT_FLOW_ACTIVITY,
            PaymentSession.TOKEN_PAYMENT_SESSION,
            PaymentFlowActivity.TOKEN_SHIPPING_INFO_SCREEN,
            PaymentFlowActivity.TOKEN_SHIPPING_METHOD_SCREEN
        )
    }
}
