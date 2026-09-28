package com.stripe.android.paymentsheet.repositories

import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent

internal object CheckoutSessionResponseFactory {
    fun create(
        id: String = DEFAULT_CHECKOUT_SESSION_ID,
        amount: Long = 1000L,
        currency: String = "usd",
        paymentStatus: CheckoutSessionResponse.PaymentStatus = CheckoutSessionResponse.PaymentStatus.UNPAID,
        status: CheckoutSessionResponse.Status = CheckoutSessionResponse.Status.OPEN,
        liveMode: Boolean = false,
        customerEmail: String? = null,
        elementsSession: ElementsSession? = null,
        paymentIntent: PaymentIntent? = null,
        setupIntent: SetupIntent? = null,
        customer: CheckoutSessionResponse.Customer? = null,
        savedPaymentMethodsOfferSave: CheckoutSessionResponse.SavedPaymentMethodsOfferSave? = null,
        checkoutItems: List<CheckoutSessionResponse.CheckoutItem> = listOf(checkoutItem(amount, currency)),
        recurringDetails: CheckoutSessionResponse.RecurringDetails? = null,
        adaptivePricingInfo: CheckoutSessionResponse.AdaptivePricingInfo? = null,
        taxMeta: CheckoutSessionResponse.TaxMeta? = CheckoutSessionResponse.TaxMeta(
            CheckoutSessionResponse.TaxComputationType.OFF,
            null,
        ),
        automaticTaxEnabled: Boolean = false,
        taxAddressSource: CheckoutSessionResponse.TaxAddressSource? = null,
        allowedShippingCountries: List<String>? = null,
        requiresShippingAddress: Boolean = false,
        requiresBillingAddress: Boolean = false,
        merchantCountry: String? = "US",
        businessName: String? = "Example, Inc.",
    ) = CheckoutSessionResponse(
        id = id,
        currency = currency,
        paymentStatus = paymentStatus,
        status = status,
        livemode = liveMode,
        customerEmail = customerEmail,
        elementsSession = elementsSession,
        paymentIntent = paymentIntent,
        setupIntent = setupIntent,
        customer = customer,
        savedPaymentMethodsOfferSave = savedPaymentMethodsOfferSave,
        checkoutItems = checkoutItems,
        recurringDetails = recurringDetails,
        adaptivePricingInfo = adaptivePricingInfo,
        taxMeta = taxMeta,
        automaticTaxEnabled = automaticTaxEnabled,
        taxAddressSource = taxAddressSource,
        allowedShippingCountries = allowedShippingCountries,
        requiresShippingAddress = requiresShippingAddress,
        requiresBillingAddress = requiresBillingAddress,
        merchantCountry = merchantCountry,
        businessName = businessName,
    )

    fun checkoutItem(
        total: Long = 1000L,
        currency: String = "usd",
        name: String = "Widget",
        quantity: Int = 1,
        subtotal: Long = total,
        unitAmount: Long = total,
        key: String = "group_1",
        innerItemKey: String = "item_1",
    ) = CheckoutSessionResponse.CheckoutItem(
        key = key,
        oneTimePrice = CheckoutSessionResponse.OneTimePrice(
            items = listOf(
                CheckoutSessionResponse.OneTimePriceItem(
                    innerItemKey = innerItemKey,
                    price = CheckoutSessionResponse.Price(
                        id = "price_1",
                        currency = currency,
                        unitAmount = unitAmount,
                        product = CheckoutSessionResponse.Product(name, emptyList()),
                    ),
                    quantity = quantity,
                    subtotal = subtotal,
                    total = total,
                    unitAmount = unitAmount,
                    unitAmountDecimal = null,
                    unitLabel = null,
                    taxAmounts = emptyList(),
                    taxInclusive = 0,
                    taxExclusive = 0,
                    adjustableQuantity = null,
                )
            )
        )
    )
}
