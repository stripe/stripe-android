package com.stripe.android.paymentelement.confirmation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

internal fun ConfirmationHandler.bootstrapHelper(
    paymentMethodMetadata: PaymentMethodMetadata
) {
    bootstrap(paymentMethodMetadata)
}

internal fun ConfirmationHandler.bootstrapHelper(
    paymentMethodMetadataFlow: Flow<PaymentMethodMetadata?>,
    lifecycleOwner: LifecycleOwner
) {
    lifecycleOwner.lifecycleScope.launch {
        paymentMethodMetadataFlow
            .mapNotNull { it }
            .take(1)
            .collect {
                bootstrapHelper(it)
            }
    }
}
