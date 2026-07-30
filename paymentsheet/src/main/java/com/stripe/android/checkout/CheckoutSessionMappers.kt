package com.stripe.android.checkout

import android.graphics.Bitmap
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.checkout.CheckoutController.Session.PaymentOptionDisplayData
import com.stripe.android.checkout.ece.ExpressButtonType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptionsFactory

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutSessionResponse.asCheckoutSession(
    flagImages: Map<String, Bitmap>?,
    paymentOptionDisplayData: PaymentOptionDisplayData?,
    availableExpressButtonTypes: List<ExpressButtonType>,
): Session {
    return Session(
        id = id,
        status = status.asStatus(),
        liveMode = liveMode,
        currency = currency,
        customerEmail = customerEmail,
        tax = taxStatus.asTax(),
        totalSummary = totalSummary?.asTotalSummary(),
        lineItems = lineItems.map { it.asLineItem() },
        shippingOptions = shippingOptions.map { it.asShippingRate() },
        paymentOptionDisplayData = paymentOptionDisplayData,
        currencySelectorOptions = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            flagImages = flagImages,
        ),
        availableExpressButtonTypes = availableExpressButtonTypes,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.Status.asStatus(): Session.Status {
    return when (this) {
        CheckoutSessionResponse.Status.OPEN -> Session.Status.Open
        CheckoutSessionResponse.Status.COMPLETE -> Session.Status.Complete
        CheckoutSessionResponse.Status.EXPIRED -> Session.Status.Expired
        CheckoutSessionResponse.Status.UNKNOWN -> Session.Status.Unknown
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TaxStatus.asTax(): Session.Tax {
    val status = when (this) {
        CheckoutSessionResponse.TaxStatus.READY -> {
            Session.Tax.Status.Ready
        }
        CheckoutSessionResponse.TaxStatus.REQUIRES_SHIPPING_ADDRESS -> {
            Session.Tax.Status.RequiresShippingAddress
        }
        CheckoutSessionResponse.TaxStatus.REQUIRES_BILLING_ADDRESS -> {
            Session.Tax.Status.RequiresBillingAddress
        }
        CheckoutSessionResponse.TaxStatus.UNKNOWN -> {
            Session.Tax.Status.Unknown
        }
    }
    return Session.Tax(status = status)
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TotalSummaryResponse.asTotalSummary(): Session.TotalSummary {
    return Session.TotalSummary(
        subtotal = subtotal,
        totalDueToday = totalDueToday,
        totalAmountDue = totalAmountDue,
        discountAmounts = discountAmounts.map { it.asDiscountAmount() },
        taxAmounts = taxAmounts.map { it.asTaxAmount() },
        shippingRate = shippingRate?.asShippingRate(),
        appliedBalance = appliedBalance,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.DiscountAmount.asDiscountAmount(): Session.DiscountAmount {
    return Session.DiscountAmount(
        amount = amount,
        displayName = displayName,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TaxAmount.asTaxAmount(): Session.TaxAmount {
    return Session.TaxAmount(
        amount = amount,
        inclusive = inclusive,
        displayName = displayName,
        percentage = percentage,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.ShippingRate.asShippingRate(): Session.ShippingRate {
    return Session.ShippingRate(
        id = id,
        amount = amount,
        displayName = displayName,
        deliveryEstimate = deliveryEstimate,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.LineItem.asLineItem(): Session.LineItem {
    return Session.LineItem(
        id = id,
        name = name,
        quantity = quantity,
        unitAmount = unitAmount,
        subtotal = subtotal,
        total = total,
    )
}
