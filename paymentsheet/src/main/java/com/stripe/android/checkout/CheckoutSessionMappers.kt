@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.graphics.Bitmap
import com.stripe.android.checkout.CheckoutElementsController.Session
import com.stripe.android.checkout.CheckoutElementsController.Session.PaymentOptionDisplayData
import com.stripe.android.elements.ece.ExpressButtonType
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptionsFactory
import com.stripe.android.uicore.format.CurrencyFormatter
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.pow

private const val EXTRA_DECIMAL_PRICE_PRECISION = 12

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutSessionResponse.asCheckoutSession(
    collectedEmail: String?,
    collectedShippingName: String?,
    collectedShippingAddress: CheckoutElementsController.Address.State?,
    flagImages: Map<String, Bitmap>?,
    paymentOption: PaymentOptionDisplayData?,
    availableExpressButtonTypes: List<ExpressButtonType>,
    locale: Locale = Locale.getDefault(),
): Session {
    val presentmentCurrency = currency
    return Session(
        id = id,
        businessName = businessName,
        status = status.asStatus(paymentStatus),
        livemode = livemode,
        currency = integrationCurrency,
        presentmentDetails = adaptivePricingInfo?.let {
            Session.PresentmentDetails(it.activePresentmentCurrency)
        },
        discountAmounts = emptyList(),
        email = fixedEmail ?: collectedEmail,
        orderSummaryItems = checkoutItems.map { it.asOrderSummaryItem(locale) },
        minorUnitsAmountDivisor = 10.0.pow(currencyDigits(presentmentCurrency)).toInt(),
        paymentOption = paymentOption,
        shippingAddress = collectedShippingAddress?.let {
            Session.ShippingAddress(collectedShippingName, it.asShippingAddress())
        },
        tax = taxMeta.asTax(taxAddressSource),
        taxAmounts = totalSummary.totalTaxAmounts.map {
            it.asTaxAmount(presentmentCurrency, locale)
        },
        totals = totals(presentmentCurrency, locale),
        currencySelectorOptions = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            locale = locale,
            flagImages = flagImages,
        ),
        availableExpressButtonTypes = availableExpressButtonTypes,
    )
}

private fun CheckoutSessionResponse.Status.asStatus(
    paymentStatus: CheckoutSessionResponse.PaymentStatus,
): Session.Status = when (this) {
    CheckoutSessionResponse.Status.OPEN -> Session.Status.Open()
    CheckoutSessionResponse.Status.EXPIRED -> Session.Status.Expired()
    CheckoutSessionResponse.Status.COMPLETE -> Session.Status.Complete(paymentStatus.asPaymentStatus())
}

internal fun CheckoutSessionResponse.PaymentStatus.asPaymentStatus(): Session.Status.PaymentStatus = when (this) {
    CheckoutSessionResponse.PaymentStatus.PAID -> Session.Status.PaymentStatus.Paid
    CheckoutSessionResponse.PaymentStatus.UNPAID -> Session.Status.PaymentStatus.Unpaid
    CheckoutSessionResponse.PaymentStatus.NO_PAYMENT_REQUIRED -> Session.Status.PaymentStatus.NoPaymentRequired
}

private fun CheckoutSessionResponse.TaxMeta?.asTax(
    addressSource: CheckoutSessionResponse.TaxAddressSource?,
): Session.Tax? {
    this ?: return null
    if (computationType != CheckoutSessionResponse.TaxComputationType.AUTOMATIC) {
        return Session.Tax(Session.Tax.Status.Ready)
    }
    val status = when (status) {
        CheckoutSessionResponse.TaxStatus.COMPLETE -> Session.Tax.Status.Ready
        CheckoutSessionResponse.TaxStatus.REQUIRES_LOCATION_INPUTS -> when (addressSource) {
            CheckoutSessionResponse.TaxAddressSource.SHIPPING -> Session.Tax.Status.RequiresShippingAddress
            CheckoutSessionResponse.TaxAddressSource.BILLING -> Session.Tax.Status.RequiresBillingAddress
            null -> return null
        }
        CheckoutSessionResponse.TaxStatus.FAILED, null -> return null
    }
    return Session.Tax(status)
}

private fun CheckoutSessionResponse.CheckoutItem.asOrderSummaryItem(locale: Locale): Session.OrderSummaryItem {
    return Session.OrderSummaryItem.OneTimePrice(
        key = key,
        description = null,
        items = oneTimePrice.items.map { item ->
            val itemCurrency = item.price.currency
            val unitAmount = item.unitAmount ?: item.price.unitAmount ?: 0L
            Session.OrderSummaryItem.OneTimePrice.Item(
                key = item.innerItemKey,
                displayName = item.price.product.name,
                images = item.price.product.images,
                unitAmount = amount(unitAmount, itemCurrency, locale),
                unitAmountDecimal = item.unitAmountDecimal?.let {
                    decimalAmount(it, itemCurrency, locale)
                },
                unitLabel = item.unitLabel,
                quantity = item.quantity,
                adjustableQuantity = item.adjustableQuantity?.takeIf { it.enabled }?.let {
                    Session.AdjustableQuantity(true, requireNotNull(it.maximum), requireNotNull(it.minimum))
                },
                amountDetails = Session.OrderSummaryItem.OneTimePrice.Item.AmountDetails(
                    total = amount(item.total, itemCurrency, locale),
                    subtotal = amount(item.subtotal, itemCurrency, locale),
                    taxAmounts = item.taxAmounts.map { it.asTaxAmount(itemCurrency, locale) }.ifEmpty { null },
                    taxInclusive = amount(item.taxInclusive, itemCurrency, locale),
                    taxExclusive = amount(item.taxExclusive, itemCurrency, locale),
                ),
            )
        },
    )
}

private fun CheckoutSessionResponse.TaxAmount.asTaxAmount(
    currency: String,
    locale: Locale,
): Session.TaxAmount {
    val publicAmount = amount(amount, currency, locale)
    return Session.TaxAmount(
        amount = publicAmount.amount,
        minorUnitsAmount = publicAmount.minorUnitsAmount,
        inclusive = inclusive,
        displayName = taxRate.displayName,
        percentage = taxRate.percentage.takeUnless {
            taxRate.rateType == CheckoutSessionResponse.TaxRateType.FLAT_AMOUNT
        },
    )
}

private fun CheckoutSessionResponse.totals(currency: String, locale: Locale): Session.Totals {
    return Session.Totals(
        subtotal = amount(totalSummary.subtotal, currency, locale),
        taxExclusive = amount(totalSummary.taxExclusive, currency, locale),
        taxInclusive = amount(totalSummary.taxInclusive, currency, locale),
        discount = amount(0L, currency, locale),
        total = amount(totalSummary.total, currency, locale),
    )
}

private fun CheckoutElementsController.Address.State.asShippingAddress() = Session.ShippingAddress.Address(
    city = city,
    country = country,
    line1 = line1,
    line2 = line2,
    postalCode = postalCode,
    state = state,
)

private fun currencyDigits(currency: String): Int = CurrencyFormatter.getDefaultDecimalDigits(
    Currency.getInstance(currency.uppercase())
)

private fun amount(
    minorUnitsAmount: Long,
    currency: String,
    locale: Locale,
): Session.Amount = Session.Amount(
    amount = formatAmount(BigDecimal.valueOf(minorUnitsAmount), currency, locale, supportsSubcentPrecision = false),
    minorUnitsAmount = minorUnitsAmount,
)

private fun decimalAmount(
    minorUnitsAmount: BigDecimal,
    currency: String,
    locale: Locale,
): Session.DecimalAmount = Session.DecimalAmount(
    amount = formatAmount(minorUnitsAmount, currency, locale, supportsSubcentPrecision = true),
    minorUnitsAmount = minorUnitsAmount,
)

private fun formatAmount(
    minorUnitsAmount: BigDecimal,
    currency: String,
    locale: Locale,
    supportsSubcentPrecision: Boolean,
): String {
    val digits = currencyDigits(currency)
    val majorUnits = minorUnitsAmount.movePointLeft(digits)
    val formatter = NumberFormat.getCurrencyInstance(locale)
    formatter.currency = Currency.getInstance(currency.uppercase())
    formatter.minimumFractionDigits = digits
    formatter.maximumFractionDigits = digits + if (supportsSubcentPrecision) {
        EXTRA_DECIMAL_PRICE_PRECISION
    } else {
        0
    }
    if (formatter is DecimalFormat) {
        formatter.decimalFormatSymbols = formatter.decimalFormatSymbols.apply {
            this.currency = Currency.getInstance(currency.uppercase())
            currencySymbol = this.currency.getSymbol(locale)
        }
    }
    return formatter.format(majorUnits)
}
