package com.stripe.android.paymentsheet.example.playground.viewmodel

import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryCode.Companion.US
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.model.InitializationType
import com.stripe.android.paymentsheet.example.playground.model.SavedToggles
import com.stripe.android.paymentsheet.example.playground.model.Shipping
import com.stripe.android.paymentsheet.model.PaymentOption

data class PaymentSheetPlaygroundViewState(
    val isLoading: Boolean = false,
    val isConfigured: Boolean = false,
    val status: String? = null,
    val customerConfig: PaymentSheet.CustomerConfiguration? = null,
    val clientSecret: String? = null,
    val initializationType: InitializationType = InitializationType.Normal,
    val customer: CheckoutCustomer = CheckoutCustomer.Guest,
    val currency: CheckoutCurrency = CheckoutCurrency.USD,
    val merchantCountry: CountryCode = US,
    val checkoutMode: CheckoutMode = CheckoutMode.Payment,
    val linkEnabled: Boolean = true,
    val googlePayEnabled: Boolean = true,
    val shipping: Shipping = Shipping.On,
    val setDefaultBillingAddress: Boolean = false,
    val setAutomaticPaymentMethods: Boolean = true,
    val setDelayedPaymentMethods: Boolean = false,
    val customLabel: String? = null,
    val paymentMethodTypes: List<String> = emptyList(),
    val paymentOption: PaymentOption? = null,
    val paymentResult: PaymentSheetResult? = null,
) {

    private val needsReload: Boolean
        get() = paymentResult is PaymentSheetResult.Completed || paymentResult is PaymentSheetResult.Failed

    val currencyIndex: Int
        get() = PaymentSheetPlaygroundViewModel.stripeSupportedCurrencies.indexOf(currency.value)

    val merchantCountryIndex: Int
        get() = PaymentSheetPlaygroundViewModel.countryCurrencyPairs.map { it.first.code }.indexOf(merchantCountry)

    val readyForCheckout: Boolean
        get() = when (initializationType) {
            InitializationType.Normal -> clientSecret != null
            InitializationType.Deferred -> true
        }

    val disableViews: Boolean
        get() = needsReload || !readyForCheckout || isLoading

    val setDefaultShippingAddress: Boolean
        get() = shipping == Shipping.OnWithDefaults

    val allowsPaymentMethodsRequiringShippingAddress: Boolean
        get() = shipping == Shipping.On || shipping == Shipping.OnWithDefaults

    val googlePayConfig: PaymentSheet.GooglePayConfiguration?
        get() = if (googlePayEnabled) {
            PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "US",
                currencyCode = currency.value,
            )
        } else {
            null
        }
}

fun SavedToggles.toViewState(): PaymentSheetPlaygroundViewState {
    return PaymentSheetPlaygroundViewState(
        isLoading = false,
        status = null,
        customerConfig = null,
        clientSecret = null,
        initializationType = InitializationType.values().first { it.value == initialization },
        customer = CheckoutCustomer.valueOf(customer),
        currency = CheckoutCurrency(currency),
        merchantCountry = CountryCode(merchantCountryCode),
        checkoutMode = CheckoutMode.values().first { it.value == mode },
        linkEnabled = link,
        googlePayEnabled = googlePay,
        shipping = Shipping.values().first { it.value == shippingAddress },
        setAutomaticPaymentMethods = setAutomaticPaymentMethods,
        setDefaultBillingAddress = setDefaultBillingAddress,
        setDelayedPaymentMethods = setDelayedPaymentMethods,
    )
}
