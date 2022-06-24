package com.stripe.android.paymentsheet.example.playground.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.model.CountryCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.model.SavedToggles
import com.stripe.android.paymentsheet.example.playground.model.Toggle

class PaymentSheetPlaygroundViewModel(
    application: Application
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    val customerConfig = MutableLiveData<PaymentSheet.CustomerConfiguration?>()
    val clientSecret = MutableLiveData<String?>()

    val readyToCheckout: LiveData<Boolean> = clientSecret.map {
        it != null
    }

    var checkoutMode: CheckoutMode? = null
    var temporaryCustomerId: String? = null

    private val sharedPreferencesName = "playgroundToggles"

    fun storeToggleState(
        customer: String,
        link: Boolean,
        googlePay: Boolean,
        currency: String,
        merchantCountryCode: String,
        mode: String,
        setShippingAddress: Boolean,
        setDefaultBillingAddress: Boolean,
        setAutomaticPaymentMethods: Boolean,
        setDelayedPaymentMethods: Boolean,
    ) {
        val sharedPreferences = getApplication<Application>().getSharedPreferences(
            sharedPreferencesName,
            AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()

        editor.putString(Toggle.Customer.key, customer)
        editor.putBoolean(Toggle.Link.key, link)
        editor.putBoolean(Toggle.GooglePay.key, googlePay)
        editor.putString(Toggle.Currency.key, currency)
        editor.putString(Toggle.MerchantCountryCode.key, merchantCountryCode)
        editor.putString(Toggle.Mode.key, mode)
        editor.putBoolean(Toggle.SetShippingAddress.key, setShippingAddress)
        editor.putBoolean(Toggle.SetDefaultBillingAddress.key, setDefaultBillingAddress)
        editor.putBoolean(Toggle.SetAutomaticPaymentMethods.key, setAutomaticPaymentMethods)
        editor.putBoolean(Toggle.SetDelayedPaymentMethods.key, setDelayedPaymentMethods)
        editor.apply()
    }

    fun getSavedToggleState(): SavedToggles {
        val sharedPreferences = getApplication<Application>().getSharedPreferences(
            sharedPreferencesName,
            AppCompatActivity.MODE_PRIVATE
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
        val setShippingAddress = sharedPreferences.getBoolean(
            Toggle.SetShippingAddress.key,
            Toggle.SetShippingAddress.default as Boolean
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
            customer= customer.toString(),
            googlePay = googlePay,
            currency = currency.toString(),
            merchantCountryCode = merchantCountryCode.toString(),
            mode = mode.toString(),
            setShippingAddress = setShippingAddress,
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
    fun prepareCheckout(
        customer: CheckoutCustomer,
        currency: CheckoutCurrency,
        merchantCountry: CountryCode,
        mode: CheckoutMode,
        linkEnabled: Boolean,
        setShippingAddress: Boolean,
        setAutomaticPaymentMethod: Boolean,
        backendUrl: String
    ) {
        customerConfig.value = null
        clientSecret.value = null

        inProgress.postValue(true)

        val requestBody = CheckoutRequest(
            customer = customer.value,
            currency = currency.value.lowercase(),
            mode = mode.value,
            set_shipping_address = setShippingAddress,
            automatic_payment_methods = setAutomaticPaymentMethod,
            use_link = linkEnabled,
            merchant_country_code = merchantCountry.value
        )

        Fuel.post(backendUrl + "checkout")
            .jsonBody(Gson().toJson(requestBody))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        status.postValue(
                            "Preparing checkout failed:\n${result.getException().message}"
                        )
                    }
                    is Result.Success -> {
                        val checkoutResponse = Gson()
                            .fromJson(result.get(), CheckoutResponse::class.java)
                        checkoutMode = mode
                        temporaryCustomerId = if (customer == CheckoutCustomer.New) {
                            checkoutResponse.customerId
                        } else {
                            null
                        }

                        // Init PaymentConfiguration with the publishable key returned from the backend,
                        // which will be used on all Stripe API calls
                        PaymentConfiguration.init(getApplication(), checkoutResponse.publishableKey)

                        customerConfig.postValue(checkoutResponse.makeCustomerConfig())
                        clientSecret.postValue(checkoutResponse.intentClientSecret)
                    }
                }
                inProgress.postValue(false)
            }
    }
}
