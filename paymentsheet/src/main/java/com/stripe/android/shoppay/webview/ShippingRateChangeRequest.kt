package com.stripe.android.shoppay.webview

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Represents a shipping rate change request from the web view.
 */
@Parcelize
data class ShippingRateChangeRequest(
    val requestId: String,
    val shippingRate: ShippingRate,
    val currentAmount: Long,
    val timestamp: Long
) : StripeModel

/**
 * Represents a shipping rate within a shipping rate change request.
 */
@Parcelize
data class ShippingRate(
    val id: String,
    val displayName: String,
    val amount: Long,
    val deliveryEstimate: String?
) : StripeModel 