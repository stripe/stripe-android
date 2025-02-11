package com.stripe.android.customersheet

/**
 * Whether the default payment method feature is enabled and if so, which payment method is the current default.
 *
 * This is different from how we model this state in PaymentSheet. In PaymentSheet, the default payment method ID is
 * mutable, because it is possible for users to modify their default payment method and keep PaymentSheet open. However,
 * whether the feature is enabled is immutable. We track the two parts of the state separately so that we don't mix
 * immutable and mutable states. In CustomerSheet, both whether the feature is enabled and what the default payment
 * method ID is are immutable, so we track them together.
 * */
internal sealed class DefaultPaymentMethodState {
    data class Enabled(val defaultPaymentMethodId: String?) : DefaultPaymentMethodState()
    data object Disabled : DefaultPaymentMethodState()
}
