package com.stripe.android.paymentsheet.example.playground.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.model.InitializationType
import com.stripe.android.paymentsheet.example.playground.model.SavedToggles
import com.stripe.android.paymentsheet.example.playground.model.Shipping
import com.stripe.android.paymentsheet.example.playground.model.Toggle
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class PaymentSheetPlaygroundViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val settings = Settings(application)

    private val _viewState = MutableStateFlow<PaymentSheetPlaygroundViewState?>(null)
    val viewState: StateFlow<PaymentSheetPlaygroundViewState?> = _viewState

    var temporaryCustomerId: String? = null

    private val sharedPreferencesName = "playgroundToggles"

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val savedToggles = getSavedToggleState()
            val viewState = savedToggles.toViewState()
            render(viewState)
        }
    }

    private fun render(viewState: PaymentSheetPlaygroundViewState) {
        _viewState.value = viewState
    }

    fun updateInitializationType(type: InitializationType) {
        _viewState.update {
            it?.copy(initializationType = type)
        }
    }

    fun updateCheckoutMode(checkoutMode: CheckoutMode) {
        _viewState.update {
            it?.copy(checkoutMode = checkoutMode)
        }
    }

    fun updateLinkEnabled(isEnabled: Boolean) {
        _viewState.update {
            it?.copy(linkEnabled = isEnabled)
        }
    }

    fun updateGooglePayEnabled(isEnabled: Boolean) {
        _viewState.update {
            it?.copy(googlePayEnabled = isEnabled)
        }
    }

    fun reset() {
        PaymentSheet.resetCustomer(getApplication())

        val defaultViewState = PaymentSheetPlaygroundViewState()
        _viewState.value = defaultViewState

        storeToggleState(defaultViewState)
    }

    fun onConfigured(
        paymentOption: PaymentOption?,
        error: Throwable?,
    ) {
        _viewState.update {
            it?.copy(
                status = error?.let { e ->
                    "Failed to configure PaymentSheetFlowController: ${e.message}"
                },
                isConfigured = error == null,
            )
        }

        if (error == null) {
            onPaymentOption(paymentOption)
        }
    }

    fun onPaymentOption(paymentOption: PaymentOption?) {
        _viewState.update {
            it?.copy(paymentOption = paymentOption)
        }
    }

    fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        _viewState.update {
            it?.copy(
                paymentResult = paymentResult,
                status = when (paymentResult) {
                    is PaymentSheetResult.Completed -> "Payment completed!"
                    is PaymentSheetResult.Canceled -> "Payment canceled…"
                    is PaymentSheetResult.Failed -> "Payment failed: ${paymentResult.error.message}"
                },
            )
        }
    }

    fun statusDisplayed() {
        _viewState.update {
            it?.copy(status = null)
        }
    }

    fun updateCustomer(customer: CheckoutCustomer) {
        _viewState.update {
            it?.copy(customer = customer)
        }
    }

    fun updateShipping(shipping: Shipping) {
        _viewState.update {
            it?.copy(shipping = shipping)
        }
    }

    fun updateSetDefaultBillingAddress(setDefaultBillingAddress: Boolean) {
        _viewState.update {
            it?.copy(setDefaultBillingAddress = setDefaultBillingAddress)
        }
    }

    fun updateAutomaticPaymentMethods(automaticPaymentMethods: Boolean) {
        _viewState.update {
            it?.copy(setAutomaticPaymentMethods = automaticPaymentMethods)
        }
    }

    fun updateAllowDelayedPaymentMethods(allowDelayedPaymentMethods: Boolean) {
        _viewState.update {
            it?.copy(setDelayedPaymentMethods = allowDelayedPaymentMethods)
        }
    }

    fun updateCurrency(position: Int) {
        val currency = CheckoutCurrency(stripeSupportedCurrencies[position])
        _viewState.update {
            it?.copy(currency = currency)
        }
    }

    fun updateMerchantCountry(position: Int) {
        val (country, currency) = countryCurrencyPairs[position]

        _viewState.update {
            it?.copy(
                merchantCountry = country.code,
                currency = CheckoutCurrency(currency),
            )
        }

        // when the merchant changes, so the new customer id
        // created might not match the previous new customer
        temporaryCustomerId = null
    }

    fun updateCustomLabel(customLabel: String?) {
        _viewState.update {
            it?.copy(
                customLabel = customLabel,
            )
        }
    }

    fun reload(
        supportedPaymentMethods: List<String>?,
    ) {
        val viewState = viewState.value ?: return

        storeToggleState(viewState)

        prepareCheckout(
            initializationType = viewState.initializationType,
            customer = viewState.customer,
            currency = viewState.currency,
            merchantCountry = viewState.merchantCountry,
            mode = viewState.checkoutMode,
            linkEnabled = viewState.linkEnabled,
            setShippingAddress = viewState.setDefaultShippingAddress,
            setAutomaticPaymentMethod = viewState.setAutomaticPaymentMethods,
            backendUrl = settings.playgroundBackendUrl,
            supportedPaymentMethods = supportedPaymentMethods,
        )
    }

    private fun storeToggleState(viewState: PaymentSheetPlaygroundViewState) {
        storeToggleState(
            initializationType = viewState.initializationType.value,
            customer = viewState.customer.value,
            link = viewState.linkEnabled,
            googlePay = viewState.googlePayEnabled,
            currency = viewState.currency.value,
            merchantCountryCode = viewState.merchantCountry.value,
            mode = viewState.checkoutMode.value,
            shipping = viewState.shipping.value,
            setDefaultBillingAddress = viewState.setDefaultBillingAddress,
            setAutomaticPaymentMethods = viewState.setAutomaticPaymentMethods,
            setDelayedPaymentMethods = viewState.setDelayedPaymentMethods,
        )
    }

    private fun storeToggleState(
        initializationType: String,
        customer: String,
        link: Boolean,
        googlePay: Boolean,
        currency: String,
        merchantCountryCode: String,
        mode: String,
        shipping: String,
        setDefaultBillingAddress: Boolean,
        setAutomaticPaymentMethods: Boolean,
        setDelayedPaymentMethods: Boolean,
    ) {
        val sharedPreferences = getApplication<Application>().getSharedPreferences(
            sharedPreferencesName,
            AppCompatActivity.MODE_PRIVATE
        )

        sharedPreferences.edit {
            putString(Toggle.Initialization.key, initializationType)
            putString(Toggle.Customer.key, customer)
            putBoolean(Toggle.Link.key, link)
            putBoolean(Toggle.GooglePay.key, googlePay)
            putString(Toggle.Currency.key, currency)
            putString(Toggle.MerchantCountryCode.key, merchantCountryCode)
            putString(Toggle.Mode.key, mode)
            putString(Toggle.ShippingAddress.key, shipping)
            putBoolean(Toggle.SetDefaultBillingAddress.key, setDefaultBillingAddress)
            putBoolean(Toggle.SetAutomaticPaymentMethods.key, setAutomaticPaymentMethods)
            putBoolean(Toggle.SetDelayedPaymentMethods.key, setDelayedPaymentMethods)
        }
    }

    private fun getSavedToggleState(): SavedToggles {
        val sharedPreferences = getApplication<Application>().getSharedPreferences(
            sharedPreferencesName,
            AppCompatActivity.MODE_PRIVATE
        )

        val initialization = sharedPreferences.getString(
            Toggle.Initialization.key,
            Toggle.Initialization.default.toString(),
        )
        val customer = sharedPreferences.getString(
            Toggle.Customer.key,
            Toggle.Customer.default.toString()
        )
        val googlePay = sharedPreferences.getBoolean(
            Toggle.GooglePay.key,
            Toggle.GooglePay.default as Boolean
        )
        val currency = sharedPreferences.getString(
            Toggle.Currency.key,
            Toggle.Currency.default.toString()
        )
        val merchantCountryCode = sharedPreferences.getString(
            Toggle.MerchantCountryCode.key,
            Toggle.MerchantCountryCode.default.toString()
        )
        val mode = sharedPreferences.getString(
            Toggle.Mode.key,
            Toggle.Mode.default.toString()
        )
        val shippingAddress = sharedPreferences.getString(
            Toggle.ShippingAddress.key,
            Toggle.ShippingAddress.default as String
        )
        val setAutomaticPaymentMethods = sharedPreferences.getBoolean(
            Toggle.SetAutomaticPaymentMethods.key,
            Toggle.SetAutomaticPaymentMethods.default as Boolean
        )
        val setDelayedPaymentMethods = sharedPreferences.getBoolean(
            Toggle.SetDelayedPaymentMethods.key,
            Toggle.SetDelayedPaymentMethods.default as Boolean
        )
        val setDefaultBillingAddress = sharedPreferences.getBoolean(
            Toggle.SetDefaultBillingAddress.key,
            Toggle.SetDefaultBillingAddress.default as Boolean
        )
        val setLink = sharedPreferences.getBoolean(
            Toggle.Link.key,
            Toggle.Link.default as Boolean
        )

        return SavedToggles(
            initialization = initialization.toString(),
            customer= customer.toString(),
            googlePay = googlePay,
            currency = currency.toString(),
            merchantCountryCode = merchantCountryCode.toString(),
            mode = mode.toString(),
            shippingAddress = shippingAddress!!,
            setAutomaticPaymentMethods = setAutomaticPaymentMethods,
            setDelayedPaymentMethods = setDelayedPaymentMethods,
            setDefaultBillingAddress = setDefaultBillingAddress,
            link = setLink
        )

    }

    /**
     * Calls the backend to prepare for checkout. The server creates a new Payment or Setup Intent
     * that will be confirmed on the client using Payment Sheet.
     */
    private fun prepareCheckout(
        initializationType: InitializationType,
        customer: CheckoutCustomer,
        currency: CheckoutCurrency,
        merchantCountry: CountryCode,
        mode: CheckoutMode,
        linkEnabled: Boolean,
        setShippingAddress: Boolean,
        setAutomaticPaymentMethod: Boolean,
        backendUrl: String,
        supportedPaymentMethods: List<String>?
    ) {
        _viewState.update {
            it?.copy(
                isLoading = true,
                customerConfig = null,
                clientSecret = null,
                isConfigured = false,
            )
        }

        val requestBody = CheckoutRequest(
            initialization = initializationType.value,
            customer = customer.value,
            currency = currency.value.lowercase(),
            mode = mode.value,
            set_shipping_address = setShippingAddress,
            automatic_payment_methods = setAutomaticPaymentMethod,
            use_link = linkEnabled,
            merchant_country_code = merchantCountry.value,
            supported_payment_methods = supportedPaymentMethods
        )

        Fuel.post(backendUrl + "checkout")
            .jsonBody(Gson().toJson(requestBody))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        _viewState.update {
                            it?.copy(
                                isLoading = false,
                                status = "Preparing checkout failed:\n${result.getException().message}",
                            )
                        }
                    }
                    is Result.Success -> {
                        val checkoutResponse = Gson().fromJson(
                            result.get(),
                            CheckoutResponse::class.java
                        )

                        _viewState.update {
                            it?.copy(
                                isLoading = false,
                                checkoutMode = mode,
                                customerConfig = checkoutResponse.makeCustomerConfig(),
                                clientSecret = checkoutResponse.intentClientSecret,
                                paymentMethodTypes = checkoutResponse.paymentMethodTypes.orEmpty(),
                            )
                        }

                        // TODO Move to ViewState
                        temporaryCustomerId = if (customer == CheckoutCustomer.New) {
                            checkoutResponse.customerId
                        } else {
                            null
                        }

                        // Init PaymentConfiguration with the publishable key returned from the backend,
                        // which will be used on all Stripe API calls
                        PaymentConfiguration.init(getApplication(), checkoutResponse.publishableKey)
                    }
                }
            }
    }

    companion object {

        val stripeSupportedCurrencies = listOf("AUD", "EUR", "GBP", "USD", "INR")

        val countryCurrencyPairs = CountryUtils
            .getOrderedCountries(Locale.getDefault())
            .filter { country ->
                /**
                 * Modify this list if you want to change the countries displayed in the playground.
                 */
                country.code.value in setOf("US", "GB", "AU", "FR", "IN")
            }.map { country ->
                /**
                 * Modify this statement to change the default currency associated with each
                 * country.  The currency values should match the stripeSupportedCurrencies.
                 */
                when (country.code.value) {
                    "GB" -> {
                        country to "GBP"
                    }
                    "FR" -> {
                        country to "EUR"
                    }
                    "AU" -> {
                        country to "AUD"
                    }
                    "US" -> {
                        country to "USD"
                    }
                    "IN" -> {
                        country to "INR"
                    }
                    else -> {
                        country to "USD"
                    }
                }
            }
    }
}
