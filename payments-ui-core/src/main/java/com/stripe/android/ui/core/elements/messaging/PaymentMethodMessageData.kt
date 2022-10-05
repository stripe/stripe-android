package com.stripe.android.ui.core.elements.messaging

import android.graphics.Bitmap
import com.stripe.android.model.PaymentMethodMessage

data class PaymentMethodMessageData internal constructor(
    val message: PaymentMethodMessage,
    val images: Map<String, Bitmap>,
    val config: PaymentMethodMessagingView.Configuration
)
