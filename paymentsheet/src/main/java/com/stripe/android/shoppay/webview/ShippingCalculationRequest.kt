package com.stripe.android.shoppay.webview

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Represents a shipping calculation request from the web view.
 */
@Parcelize
data class ShippingCalculationRequest(
    val requestId: String,
    val shippingAddress: ShippingAddress,
    val timestamp: Long
) : StripeModel

/**
 * Represents a shipping address within a shipping calculation request.
 */
@Parcelize
data class ShippingAddress(
    val address1: String?,
    val address2: String?,
    val city: String?,
    val companyName: String?,
    val countryCode: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val postalCode: String?,
    val provinceCode: String?
) : StripeModel 