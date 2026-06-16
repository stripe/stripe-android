package com.stripe.android.checkout

import android.graphics.Bitmap
import android.os.Parcelable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptionsFactory
import kotlinx.parcelize.Parcelize

@OptIn(CheckoutSessionPreview::class)
@Parcelize
internal data class InternalState(
    val key: String,
    val configuration: Checkout.Configuration.State,
    val checkoutSessionResponse: CheckoutSessionResponse,
    private val flagImages: Map<String, Bitmap>?,
    val shippingName: String? = null,
    val billingName: String? = null,
    val shippingPhoneNumber: String? = null,
    val billingPhoneNumber: String? = null,
    val shippingAddress: Address.State? = null,
    val billingAddress: Address.State? = null,
    val integrationLaunched: Boolean = false,
) : Parcelable {
    val initializationMode: PaymentElementLoader.InitializationMode.CheckoutSession
        get() = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = key,
            checkoutSessionResponse = checkoutSessionResponse,
        )

    fun asCheckoutSession(): CheckoutSession {
        return checkoutSessionResponse.asCheckoutSession(flagImages)
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.asCheckoutSession(
    flagImages: Map<String, Bitmap>?,
): CheckoutSession {
    return CheckoutSession(
        id = id,
        status = status.asStatus(),
        liveMode = liveMode,
        currency = currency,
        customerEmail = customerEmail,
        tax = taxStatus.asTax(),
        totalSummary = totalSummary?.asTotalSummary(),
        lineItems = lineItems.map { it.asLineItem() },
        shippingOptions = shippingOptions.map { it.asShippingRate() },
        currencySelectorOptions = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            flagImages = flagImages,
        ),
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.Status.asStatus(): CheckoutSession.Status {
    return when (this) {
        CheckoutSessionResponse.Status.OPEN -> CheckoutSession.Status.Open
        CheckoutSessionResponse.Status.COMPLETE -> CheckoutSession.Status.Complete
        CheckoutSessionResponse.Status.EXPIRED -> CheckoutSession.Status.Expired
        CheckoutSessionResponse.Status.UNKNOWN -> CheckoutSession.Status.Unknown
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TaxStatus.asTax(): CheckoutSession.Tax {
    val status = when (this) {
        CheckoutSessionResponse.TaxStatus.READY -> {
            CheckoutSession.Tax.Status.Ready
        }
        CheckoutSessionResponse.TaxStatus.REQUIRES_SHIPPING_ADDRESS -> {
            CheckoutSession.Tax.Status.RequiresShippingAddress
        }
        CheckoutSessionResponse.TaxStatus.REQUIRES_BILLING_ADDRESS -> {
            CheckoutSession.Tax.Status.RequiresBillingAddress
        }
        CheckoutSessionResponse.TaxStatus.UNKNOWN -> {
            CheckoutSession.Tax.Status.Unknown
        }
    }
    return CheckoutSession.Tax(status = status)
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TotalSummaryResponse.asTotalSummary(): CheckoutSession.TotalSummary {
    return CheckoutSession.TotalSummary(
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
private fun CheckoutSessionResponse.DiscountAmount.asDiscountAmount(): CheckoutSession.DiscountAmount {
    return CheckoutSession.DiscountAmount(
        amount = amount,
        displayName = displayName,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TaxAmount.asTaxAmount(): CheckoutSession.TaxAmount {
    return CheckoutSession.TaxAmount(
        amount = amount,
        inclusive = inclusive,
        displayName = displayName,
        percentage = percentage,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.ShippingRate.asShippingRate(): CheckoutSession.ShippingRate {
    return CheckoutSession.ShippingRate(
        id = id,
        amount = amount,
        displayName = displayName,
        deliveryEstimate = deliveryEstimate,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.LineItem.asLineItem(): CheckoutSession.LineItem {
    return CheckoutSession.LineItem(
        id = id,
        name = name,
        quantity = quantity,
        unitAmount = unitAmount,
        subtotal = subtotal,
        total = total,
    )
}
