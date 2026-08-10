package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.callback.GooglePayIntermediatePaymentData
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataChangedCallback
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataRequestUpdate
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.example.databinding.GooglePayActivityBinding

/**
 * Demonstrates Google Pay dynamic callbacks that update shipping options and transaction totals
 * when the customer changes their shipping address.
 *
 * When the shipping address is in Canada, import duties are added and Canada-specific shipping
 * options are shown. US addresses receive domestic shipping options with no duties.
 */
class GooglePayDynamicCallbacksExampleActivity : StripeIntentActivity() {
    private var clientSecret = ""
    private var isGooglePayReady = false

    private val viewBinding: GooglePayActivityBinding by lazy {
        GooglePayActivityBinding.inflate(layoutInflater)
    }

    private val googlePayButton: GooglePayButton by lazy {
        viewBinding.googlePayButton
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        applyEdgeToEdgeInsets()

        savedInstanceState?.let {
            clientSecret = it.getString(SAVED_CLIENT_SECRET, "")
        }

        if (clientSecret.isBlank()) {
            viewModel.createPaymentIntent(COUNTRY_CODE)
                .observe(this) { result ->
                    result.fold(
                        onSuccess = ::onPaymentIntentCreated,
                        onFailure = { error ->
                            snackbarController.show(
                                "Could not create PaymentIntent. ${error.message}"
                            )
                        }
                    )
                }
        }

        val googlePayLauncher = GooglePayPaymentMethodLauncher(
            activity = this,
            config = GooglePayPaymentMethodLauncher.Config(
                environment = GooglePayEnvironment.Test,
                merchantCountryCode = COUNTRY_CODE,
                merchantName = "Widget Store",
                billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
                    isRequired = true,
                    format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
                    isPhoneNumberRequired = false,
                ),
                existingPaymentMethodRequired = false,
            ),
            readyCallback = ::onGooglePayReady,
            resultCallback = ::onGooglePayResult,
            dynamicCallbacksConfig = createDynamicCallbacksConfig(),
        )

        viewBinding.googlePayButton.setOnClickListener {
            viewBinding.progressBar.isVisible = true
            googlePayLauncher.present(
                currencyCode = CURRENCY_CODE,
                amount = BASE_PRICE_CENTS,
                label = "Widget",
            )
        }

        updateUi()
    }

    private fun applyEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.coordinator) { view, windowInsets ->
            val systemBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBarInsets.left,
                top = systemBarInsets.top,
                right = systemBarInsets.right,
                bottom = systemBarInsets.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVED_CLIENT_SECRET, clientSecret)
    }

    private fun updateUi() {
        val isLoadingComplete = isGooglePayReady && clientSecret.isNotBlank()
        viewBinding.progressBar.isInvisible = isLoadingComplete
        googlePayButton.isEnabled = isLoadingComplete
    }

    private fun onPaymentIntentCreated(json: org.json.JSONObject) {
        clientSecret = json.getString("secret")
        updateUi()
    }

    private fun onGooglePayReady(isReady: Boolean) {
        snackbarController.show("Google Pay ready? $isReady")
        isGooglePayReady = isReady
        updateUi()
    }

    private fun onGooglePayResult(result: GooglePayPaymentMethodLauncher.Result) {
        viewBinding.progressBar.isInvisible = true

        when (result) {
            is GooglePayPaymentMethodLauncher.Result.Completed -> {
                confirmPaymentIntent(
                    ConfirmPaymentIntentParams.createWithPaymentMethodId(
                        paymentMethodId = requireNotNull(result.paymentMethod.id),
                        clientSecret = clientSecret,
                    )
                )
                snackbarController.show("Successfully collected payment method. Confirming PaymentIntent.")
            }
            GooglePayPaymentMethodLauncher.Result.Canceled -> {
                snackbarController.show("Customer cancelled Google Pay.")
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                snackbarController.show("Google Pay failed. ${result.error.message}")
            }
        }

        if (result !is GooglePayPaymentMethodLauncher.Result.Completed) {
            googlePayButton.isEnabled = false
        }
    }

    private fun createDynamicCallbacksConfig(): GooglePayDynamicCallbacksConfig {
        return GooglePayDynamicCallbacksConfig.create(
            shippingAddressConfig = GooglePayPaymentMethodLauncher.ShippingAddressConfig(
                isRequired = true,
                allowedCountryCodes = setOf("US", "CA"),
            ),
            shippingOptionsConfig = usShippingOptionsConfig,
            onPaymentDataChanged = { intermediatePaymentData ->
                createPaymentDataUpdate(intermediatePaymentData)
            },
        )
    }

    private companion object {
        private const val COUNTRY_CODE = "US"
        private const val SAVED_CLIENT_SECRET = "client_secret"

        private const val BASE_PRICE_CENTS = 2000L
        private const val CANADA_DUTIES_CENTS = 500L
        private const val CURRENCY_CODE = "USD"

        private const val STANDARD_US_SHIPPING_ID = "standard-us"
        private const val EXPRESS_US_SHIPPING_ID = "express-us"
        private const val STANDARD_CA_SHIPPING_ID = "standard-ca"
        private const val EXPRESS_CA_SHIPPING_ID = "express-ca"

        private val usShippingOptionsConfig = GooglePayPaymentMethodLauncher.ShippingOptionsConfig(
            defaultSelectedOptionId = STANDARD_US_SHIPPING_ID,
            shippingOptions = listOf(
                GooglePayPaymentMethodLauncher.ShippingOption(
                    id = STANDARD_US_SHIPPING_ID,
                    label = "Free standard shipping",
                    description = "Delivered in 5-7 business days.",
                ),
                GooglePayPaymentMethodLauncher.ShippingOption(
                    id = EXPRESS_US_SHIPPING_ID,
                    label = "Express shipping (\$5.00)",
                    description = "Delivered in 2-3 business days.",
                ),
            ),
        )

        private val canadaShippingOptions = GooglePayJsonFactory.ShippingOptionParameters(
            defaultSelectedOptionId = STANDARD_CA_SHIPPING_ID,
            shippingOptions = listOf(
                GooglePayJsonFactory.ShippingOption(
                    id = STANDARD_CA_SHIPPING_ID,
                    label = "Standard shipping (\$8.00)",
                    description = "Delivered in 7-10 business days.",
                ),
                GooglePayJsonFactory.ShippingOption(
                    id = EXPRESS_CA_SHIPPING_ID,
                    label = "Express shipping (\$15.00)",
                    description = "Delivered in 3-5 business days.",
                ),
            ),
        )

        private val usShippingOptions = GooglePayJsonFactory.ShippingOptionParameters(
            defaultSelectedOptionId = STANDARD_US_SHIPPING_ID,
            shippingOptions = listOf(
                GooglePayJsonFactory.ShippingOption(
                    id = STANDARD_US_SHIPPING_ID,
                    label = "Free standard shipping",
                    description = "Delivered in 5-7 business days.",
                ),
                GooglePayJsonFactory.ShippingOption(
                    id = EXPRESS_US_SHIPPING_ID,
                    label = "Express shipping (\$5.00)",
                    description = "Delivered in 2-3 business days.",
                ),
            ),
        )

        private fun createPaymentDataUpdate(
            intermediatePaymentData: GooglePayIntermediatePaymentData,
        ): GooglePayPaymentDataRequestUpdate {
            val countryCode = intermediatePaymentData.shippingAddress?.countryCode.orEmpty()
            val isCanada = countryCode.equals("CA", ignoreCase = true)
            val shippingOptions = if (isCanada) canadaShippingOptions else usShippingOptions

            val selectedShippingOptionId = when (intermediatePaymentData.callbackTrigger) {
                GooglePayIntermediatePaymentData.CallbackTrigger.SHIPPING_OPTION -> {
                    intermediatePaymentData.shippingOption?.id
                }
                else -> shippingOptions.defaultSelectedOptionId
            } ?: shippingOptions.defaultSelectedOptionId.orEmpty()

            val shippingCostCents = shippingCostCents(selectedShippingOptionId)
            val dutiesCents = if (isCanada) CANADA_DUTIES_CENTS else 0L
            val totalCents = BASE_PRICE_CENTS + shippingCostCents + dutiesCents

            val shouldUpdateShippingOptions = intermediatePaymentData.callbackTrigger !=
                GooglePayIntermediatePaymentData.CallbackTrigger.SHIPPING_OPTION

            return GooglePayPaymentDataRequestUpdate(
                newTransactionInfo = GooglePayJsonFactory.TransactionInfo(
                    currencyCode = CURRENCY_CODE,
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                    totalPrice = totalCents.toInt(),
                    totalPriceLabel = "Total",
                ),
                newShippingOptionParameters = if (shouldUpdateShippingOptions) {
                    shippingOptions
                } else {
                    null
                },
            )
        }

        private fun shippingCostCents(shippingOptionId: String): Long {
            return when (shippingOptionId) {
                EXPRESS_US_SHIPPING_ID -> 500L
                STANDARD_CA_SHIPPING_ID -> 800L
                EXPRESS_CA_SHIPPING_ID -> 1500L
                else -> 0L
            }
        }
    }
}
