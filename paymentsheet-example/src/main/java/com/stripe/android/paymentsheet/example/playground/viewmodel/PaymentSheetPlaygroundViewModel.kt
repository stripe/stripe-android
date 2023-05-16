package com.stripe.android.paymentsheet.example.playground.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.stripe.android.CreateIntentResult
import com.stripe.android.DelicatePaymentSheetApi
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.model.CountryCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.model.ConfirmIntentRequest
import com.stripe.android.paymentsheet.example.playground.model.ConfirmIntentResponse
import com.stripe.android.paymentsheet.example.playground.model.InitializationType
import com.stripe.android.paymentsheet.example.playground.model.SavedToggles
import com.stripe.android.paymentsheet.example.playground.model.Toggle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PaymentSheetPlaygroundViewModel(
    application: Application
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    val customerConfig = MutableLiveData<PaymentSheet.CustomerConfiguration?>(null)
    val clientSecret = MutableLiveData<String?>(null)

    val initializationType = MutableStateFlow(InitializationType.Normal)
    val amount = MutableStateFlow<Long>(0)
    val paymentMethodTypes = MutableStateFlow<List<String>>(emptyList())

    val readyToCheckout = combine(
        initializationType,
        clientSecret.asFlow(),
    ) { type, secret ->
        when (type) {
            InitializationType.Normal -> secret != null
            InitializationType.DeferredClientSideConfirmation,
            InitializationType.DeferredServerSideConfirmation,
            InitializationType.DeferredManualConfirmation,
            InitializationType.DeferredMultiprocessor -> paymentMethodTypes.value.isNotEmpty()
        }
    }

    val checkoutMode = MutableStateFlow(CheckoutMode.Payment)
    var temporaryCustomerId: String? = null

    private val sharedPreferencesName = "playgroundToggles"

    suspend fun storeToggleState(
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
        attachDefaultBillingAddress: Boolean,
        collectName: String,
        collectEmail: String,
        collectPhone: String,
        collectAddress: String,
    ) = withContext(Dispatchers.IO) {
        val sharedPreferences = getApplication<Application>().getSharedPreferences(
            sharedPreferencesName,
            AppCompatActivity.MODE_PRIVATE
        )

        sharedPreferences.edit {
            putString(Toggle.Initialization.key, initializationType.value.value)
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
            putBoolean(Toggle.AttachDefaults.key, attachDefaultBillingAddress)
            putString(Toggle.CollectName.key, collectName)
            putString(Toggle.CollectEmail.key, collectEmail)
            putString(Toggle.CollectPhone.key, collectPhone)
            putString(Toggle.CollectAddress.key, collectAddress)
        }
    }

    suspend fun getSavedToggleState(): SavedToggles = withContext(Dispatchers.IO) {
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
        val attachDefaults = sharedPreferences.getBoolean(
            Toggle.AttachDefaults.key,
            Toggle.AttachDefaults.default as Boolean
        )
        val collectName = sharedPreferences.getString(
            Toggle.CollectName.key,
            Toggle.CollectName.default as String
        )
        val collectEmail = sharedPreferences.getString(
            Toggle.CollectEmail.key,
            Toggle.CollectEmail.default as String
        )
        val collectPhone = sharedPreferences.getString(
            Toggle.CollectPhone.key,
            Toggle.CollectPhone.default as String
        )
        val collectAddress = sharedPreferences.getString(
            Toggle.CollectAddress.key,
            Toggle.CollectAddress.default as String
        )

        SavedToggles(
            initialization = initialization.toString(),
            customer = customer.toString(),
            googlePay = googlePay,
            currency = currency.toString(),
            merchantCountryCode = merchantCountryCode.toString(),
            mode = mode.toString(),
            shippingAddress = shippingAddress!!,
            setAutomaticPaymentMethods = setAutomaticPaymentMethods,
            setDelayedPaymentMethods = setDelayedPaymentMethods,
            setDefaultBillingAddress = setDefaultBillingAddress,
            link = setLink,
            attachDefaults = attachDefaults,
            collectName = collectName.toString(),
            collectEmail = collectEmail.toString(),
            collectPhone = collectPhone.toString(),
            collectAddress = collectAddress.toString(),
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
        backendUrl: String,
        supportedPaymentMethods: List<String>?,
    ) {
        customerConfig.value = null
        clientSecret.value = null

        inProgress.postValue(true)

        val initializationType = initializationType.value

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
            .jsonBody(Json.encodeToString(CheckoutRequest.serializer(), requestBody))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        status.postValue("Preparing checkout failed:\n${result.getException().message}")
                    }
                    is Result.Success -> {
                        val checkoutResponse = Json.decodeFromString(
                            CheckoutResponse.serializer(),
                            result.get(),
                        )
                        checkoutMode.value = mode
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

                        amount.value = checkoutResponse.amount
                        paymentMethodTypes.value = checkoutResponse.paymentMethodTypes
                            .orEmpty()
                            .split(",")
                    }
                }
                inProgress.postValue(false)
            }
    }

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class, DelicatePaymentSheetApi::class)
    @Suppress("UNUSED_PARAMETER")
    fun createIntent(
        paymentMethodId: String,
        merchantCountryCode: String,
        mode: String,
        returnUrl: String,
        backendUrl: String,
    ): CreateIntentResult {
        val initializationType = initializationType.value

        val clientSecret = if (initializationType == InitializationType.DeferredMultiprocessor) {
            PaymentSheet.IntentConfiguration.DISMISS_WITH_SUCCESS
        } else {
            // Note: This is not how you'd do this in a real application. Instead, your app would
            // call your backend and create (and optionally confirm) a payment or setup intent.
            clientSecret.value!!
        }

        return CreateIntentResult.Success(clientSecret)
    }

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class, DelicatePaymentSheetApi::class)
    suspend fun createAndConfirmIntent(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
        merchantCountryCode: String,
        mode: String,
        returnUrl: String,
        backendUrl: String,
    ): CreateIntentResult {
        return if (initializationType.value == InitializationType.DeferredMultiprocessor) {
            CreateIntentResult.Success(PaymentSheet.IntentConfiguration.DISMISS_WITH_SUCCESS)
        } else {
            createAndConfirmIntentInternal(
                paymentMethodId = paymentMethodId,
                shouldSavePaymentMethod = shouldSavePaymentMethod,
                merchantCountryCode = merchantCountryCode,
                mode = mode,
                returnUrl = returnUrl,
                backendUrl = backendUrl,
            )
        }
    }

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class)
    private suspend fun createAndConfirmIntentInternal(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
        merchantCountryCode: String,
        mode: String,
        returnUrl: String,
        backendUrl: String,
    ): CreateIntentResult {
        // Note: This is not how you'd do this in a real application. You wouldn't have a client
        // secret available at this point, but you'd call your backend to create (and optionally
        // confirm) a payment or setup intent.
        val request = ConfirmIntentRequest(
            clientSecret = clientSecret.value!!,
            paymentMethodId = paymentMethodId,
            shouldSavePaymentMethod = shouldSavePaymentMethod,
            merchantCountryCode = merchantCountryCode,
            mode = mode,
            returnUrl = returnUrl,
        )

        return suspendCoroutine { continuation ->
            Fuel.post(backendUrl + "confirm_intent")
                .jsonBody(Json.encodeToString(ConfirmIntentRequest.serializer(), request))
                .responseString { _, _, result ->
                    when (result) {
                        is Result.Failure -> {
                            val message = "Creating intent failed:\n${result.getException().message}"
                            status.postValue(message)

                            val error = if (result.error.cause is IOException) {
                                ConfirmIntentNetworkException()
                            } else {
                                ConfirmIntentEndpointException()
                            }

                            continuation.resume(
                                CreateIntentResult.Failure(
                                    cause = error,
                                    displayMessage = message
                                )
                            )
                        }
                        is Result.Success -> {
                            val confirmIntentResponse = Json.decodeFromString(
                                ConfirmIntentResponse.serializer(),
                                result.get(),
                            )

                            continuation.resume(
                                CreateIntentResult.Success(
                                    clientSecret = confirmIntentResponse.clientSecret,
                                )
                            )
                        }
                    }
                    inProgress.postValue(false)
                }
        }
    }
}

class ConfirmIntentEndpointException : Exception()

class ConfirmIntentNetworkException : Exception()
