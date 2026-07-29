package com.stripe.android.checkout

import android.graphics.Bitmap
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.checkout.CheckoutController.Session.PaymentOptionDisplayData
import com.stripe.android.checkout.ece.ExpressButtonType
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptionsFactory
import com.stripe.android.uicore.format.CurrencyFormatter
import java.util.Currency
import kotlin.math.pow

private const val MAJOR_UNIT_BASE = 10.0
private const val LAST_PAYMENT_ERROR_ANALYTICS_VALUE = "checkoutSessionLastPaymentError"

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutSessionResponse.asCheckoutSession(
    flagImages: Map<String, Bitmap>?,
    collectedDetails: CheckoutCollectedDetails,
    paymentOptionDisplayData: PaymentOptionDisplayData?,
    availableExpressButtonTypes: List<ExpressButtonType>,
): Session {
    return Session(
        id = id,
        status = asStatus(),
        liveMode = liveMode,
        businessName = businessName,
        currency = currency,
        minorUnitsAmountDivisor = minorUnitsAmountDivisor(currency),
        presentmentDetails = adaptivePricingInfo?.let {
            Session.PresentmentDetails(presentmentCurrency = it.activePresentmentCurrency)
        },
        email = customerEmail,
        shippingAddress = collectedDetails.asShippingAddress(),
        tax = taxStatus.asTax(),
        totalSummary = totalSummary?.asTotalSummary(currency),
        lineItems = lineItems.map { it.asLineItem(currency) },
        shippingOptions = shippingOptions.map { it.asShippingRate(currency) },
        paymentOptionDisplayData = paymentOptionDisplayData,
        lastPaymentError = lastPaymentError(),
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

/**
 * Projects the locally collected shipping details onto the public [Session.ShippingAddress]. Returns
 * `null` until a shipping address has been collected (a name alone isn't enough to form an address).
 */
@OptIn(CheckoutSessionPreview::class)
private fun CheckoutCollectedDetails.asShippingAddress(): Session.ShippingAddress? {
    val address = shippingAddress ?: return null
    return Session.ShippingAddress(
        name = shippingName,
        address = Session.Address(
            country = address.country,
            line1 = address.line1,
            line2 = address.line2,
            city = address.city,
            postalCode = address.postalCode,
            state = address.state,
        ),
    )
}

/**
 * Maps the session lifecycle status. The public [Session.Status] intentionally has no `Unknown` case
 * (per the API design), so an unrecognized server status is treated as [Session.Status.Open] — the
 * conservative, non-terminal state. When the session is complete, the payment status is derived from
 * [paymentStatus].
 */
@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.asStatus(): Session.Status {
    return when (status) {
        CheckoutSessionResponse.Status.OPEN -> Session.Status.Open
        CheckoutSessionResponse.Status.EXPIRED -> Session.Status.Expired
        CheckoutSessionResponse.Status.COMPLETE -> Session.Status.Complete(paymentStatus())
        CheckoutSessionResponse.Status.UNKNOWN -> Session.Status.Open
    }
}

/**
 * Best-guess derivation of the payment status for a completed session (the `/init` contract doesn't
 * expose it directly): setup-mode sessions and zero-amount orders require no payment; otherwise the
 * payment is considered collected only when the PaymentIntent has succeeded or is awaiting capture.
 */
@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.paymentStatus(): Session.Status.PaymentStatus {
    return when {
        mode == CheckoutSessionResponse.Mode.SETUP -> Session.Status.PaymentStatus.NoPaymentRequired
        amount == 0L -> Session.Status.PaymentStatus.NoPaymentRequired
        paymentIntent?.status == StripeIntent.Status.Succeeded ||
            paymentIntent?.status == StripeIntent.Status.RequiresCapture ->
            Session.Status.PaymentStatus.Paid
        else -> Session.Status.PaymentStatus.Unpaid
    }
}

/**
 * Surfaces the error from the last confirmation attempt, wrapped as a [Throwable]. Best guess: the
 * `/init` contract doesn't confirm which intent carries it, so we prefer the PaymentIntent's error
 * and fall back to the SetupIntent's.
 */
@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.lastPaymentError(): Throwable? {
    paymentIntent?.lastPaymentError?.let { error ->
        return LocalStripeException(
            displayMessage = error.message,
            analyticsValue = LAST_PAYMENT_ERROR_ANALYTICS_VALUE,
            errorCode = error.code,
            declineCode = error.declineCode,
        )
    }
    setupIntent?.lastSetupError?.let { error ->
        return LocalStripeException(
            displayMessage = error.message,
            analyticsValue = LAST_PAYMENT_ERROR_ANALYTICS_VALUE,
            errorCode = error.code,
            declineCode = error.declineCode,
        )
    }
    return null
}

/**
 * Maps the tax computation status. The public [Session.Tax.Status] intentionally has no `Unknown`
 * case, so an unrecognized server status is treated as [Session.Tax.Status.RequiresShippingAddress]
 * — the conservative "not ready to confirm" state.
 */
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
            Session.Tax.Status.RequiresShippingAddress
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
