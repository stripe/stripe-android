package com.stripe.android.paymentsheet.analytics

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import dagger.Reusable
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@Reusable
internal class InitEventHelper @Inject constructor() {
    private val hasPendingInit = AtomicBoolean(false)

    fun onInit() {
        hasPendingInit.set(true)
    }

    fun publishableKeyForInit(publishableKey: String?, paymentMethodMetadata: PaymentMethodMetadata?): String? {
        val resolvedPublishableKey = publishableKey ?: paymentMethodMetadata?.apiConfiguration?.publishableKey
        return resolvedPublishableKey?.takeIf {
            hasPendingInit.compareAndSet(true, false)
        }
    }
}
