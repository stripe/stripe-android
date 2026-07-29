package com.stripe.android.checkout

import android.graphics.Bitmap
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.checkout.CheckoutController.Session.PaymentOptionDisplayData
import com.stripe.android.checkout.ece.ExpressButtonType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptionsFactory
import com.stripe.android.uicore.format.CurrencyFormatter
import java.util.Currency
import kotlin.math.pow

private const val MAJOR_UNIT_BASE = 10.0

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
        minorUnitsAmountDivisor = minorUnitsAmountDivisor(currency),
        customerEmail = customerEmail,
        tax = taxStatus.asTax(),
        totalSummary = totalSummary?.asTotalSummary(currency),
        lineItems = lineItems.map { it.asLineItem(currency) },
        shippingOptions = shippingOptions.map { it.asShippingRate(currency) },
        paymentOptionDisplayData = paymentOptionDisplayData,
        currencySelectorOptions = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            flagImages = flagImages,
        ),
        availableExpressButtonTypes = availableExpressButtonTypes,
    )
}

/**
 * Wraps a raw minor-units [Long] in a [Session.Amount], attaching a localized, currency-formatted
 * display string produced by the shared [CurrencyFormatter].
 */
@OptIn(CheckoutSessionPreview::class)
private fun Long.asAmount(currency: String): Session.Amount = Session.Amount(
    minorUnitsAmount = this,
    formatted = CurrencyFormatter.format(amount = this, amountCurrencyCode = currency),
)

/**
 * The factor to convert amounts in the smallest currency unit to the major unit (e.g. 100 for USD,
 * 1 for JPY), derived from the same decimal-digit logic [CurrencyFormatter] uses. `null` when the
 * currency code isn't recognized.
 */
private fun minorUnitsAmountDivisor(currency: String): Int? = runCatching {
    val decimalDigits = CurrencyFormatter.getDefaultDecimalDigits(Currency.getInstance(currency.uppercase()))
    MAJOR_UNIT_BASE.pow(decimalDigits).toInt()
}.getOrNull()

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
private fun CheckoutSessionResponse.TotalSummaryResponse.asTotalSummary(currency: String): Session.TotalSummary {
    return Session.TotalSummary(
        subtotal = subtotal.asAmount(currency),
        totalDueToday = totalDueToday.asAmount(currency),
        totalAmountDue = totalAmountDue.asAmount(currency),
        discountAmounts = discountAmounts.map { it.asDiscountAmount(currency) },
        taxAmounts = taxAmounts.map { it.asTaxAmount(currency) },
        shippingRate = shippingRate?.asShippingRate(currency),
        appliedBalance = appliedBalance?.asAmount(currency),
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.DiscountAmount.asDiscountAmount(currency: String): Session.DiscountAmount {
    return Session.DiscountAmount(
        amount = amount.asAmount(currency),
        displayName = displayName,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TaxAmount.asTaxAmount(currency: String): Session.TaxAmount {
    return Session.TaxAmount(
        amount = amount.asAmount(currency),
        inclusive = inclusive,
        displayName = displayName,
        percentage = percentage,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.ShippingRate.asShippingRate(currency: String): Session.ShippingRate {
    return Session.ShippingRate(
        id = id,
        amount = amount.asAmount(currency),
        displayName = displayName,
        deliveryEstimate = deliveryEstimate,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.LineItem.asLineItem(currency: String): Session.LineItem {
    return Session.LineItem(
        id = id,
        name = name,
        quantity = quantity,
        unitAmount = unitAmount?.asAmount(currency),
        subtotal = subtotal.asAmount(currency),
        total = total.asAmount(currency),
    )
}
