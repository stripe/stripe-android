package com.stripe.android.shoppay.webview

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Represents a handle click request from the web view.
 */
@Parcelize
data class HandleClickRequest(
    val requestId: String,
    val eventData: EventData,
    val timestamp: Long
) : StripeModel

/**
 * Represents event data within a handle click request.
 */
@Parcelize
data class EventData(
    val expressPaymentType: String
) : StripeModel 