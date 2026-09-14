package com.stripe.android.checkout

import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun PaymentElement.Configuration.CardBrandAcceptance.asPaymentSheet(): PaymentSheet.CardBrandAcceptance =
    when (this) {
        is PaymentElement.Configuration.CardBrandAcceptance.All -> PaymentSheet.CardBrandAcceptance.All
        is PaymentElement.Configuration.CardBrandAcceptance.Allowed -> {
            PaymentSheet.CardBrandAcceptance.Allowed(brands.map { it.asPaymentSheet() })
        }
        is PaymentElement.Configuration.CardBrandAcceptance.Disallowed -> {
            PaymentSheet.CardBrandAcceptance.Disallowed(brands.map { it.asPaymentSheet() })
        }
    }

@OptIn(CheckoutSessionPreview::class)
private fun PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.asPaymentSheet():
    PaymentSheet.CardBrandAcceptance.BrandCategory =
    when (this) {
        PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Visa ->
            PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
        PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Mastercard ->
            PaymentSheet.CardBrandAcceptance.BrandCategory.Mastercard
        PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Amex ->
            PaymentSheet.CardBrandAcceptance.BrandCategory.Amex
        PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Discover ->
            PaymentSheet.CardBrandAcceptance.BrandCategory.Discover
    }
