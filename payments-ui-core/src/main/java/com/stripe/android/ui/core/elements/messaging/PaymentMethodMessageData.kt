package com.stripe.android.ui.core.elements.messaging

import android.graphics.Bitmap
import com.stripe.android.model.PaymentMethodMessage

/**
 * Contains the data needed to render the message view. It should not be constructed. When
 * initializing a compose view using [rememberMessagingState], pass this class to the
 * [PaymentMethodMessage] composable view.
 */
internal data class PaymentMethodMessageData internal constructor(
    val message: PaymentMethodMessage,
    val images: Map<String, Bitmap>,
    val config: PaymentMethodMessageView.Configuration
)
