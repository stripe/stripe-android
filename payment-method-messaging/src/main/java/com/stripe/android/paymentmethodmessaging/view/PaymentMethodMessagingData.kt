package com.stripe.android.paymentmethodmessaging.view

import android.graphics.Bitmap
import com.stripe.android.model.PaymentMethodMessage

/**
 * Contains the data needed to render the message view. It should not be constructed. When
 * initializing a compose view using [rememberMessagingState], pass this class to the
 * [PaymentMethodMessaging] composable view.
 */
internal data class PaymentMethodMessagingData(
    val message: PaymentMethodMessage,
    val images: Map<String, Bitmap>,
    val config: PaymentMethodMessagingView.Configuration
)
