package com.stripe.android.paymentsheet.example.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.idling.CountingIdlingResource
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.databinding.ActivityPaymentSheetPlaygroundBinding
import com.stripe.android.paymentsheet.example.repository.Repository
import com.stripe.android.paymentsheet.example.viewmodel.PaymentSheetPlaygroundViewModel
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.launch

internal open class PaymentSheetPlaygroundActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetPlaygroundBinding.inflate(layoutInflater)
    }

    val viewModel: PaymentSheetPlaygroundViewModel by viewModels {
        PaymentSheetPlaygroundViewModel.Factory(
            application
        )
    }

    private val customer: Repository.CheckoutCustomer
        get() = when (viewBinding.customerRadioGroup.checkedRadioButtonId) {
            R.id.guest_customer_button -> Repository.CheckoutCustomer.Guest
            R.id.new_customer_button -> {
                viewModel.temporaryCustomerId?.let {
                    Repository.CheckoutCustomer.WithId(it)
                } ?: Repository.CheckoutCustomer.New
            }
            else -> Repository.CheckoutCustomer.Returning
        }

    private val googlePayConfig: PaymentSheet.GooglePayConfiguration?
        get() = when (viewBinding.googlePayRadioGroup.checkedRadioButtonId) {
            R.id.google_pay_on_button -> {
                PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US",
                    currencyCode = currency.value
                )
            }
            else -> null
        }

    private val currency: Repository.CheckoutCurrency
        get() = when (viewBinding.currencyRadioGroup.checkedRadioButtonId) {
            R.id.currency_usd_button -> Repository.CheckoutCurrency.USD
            else -> Repository.CheckoutCurrency.EUR
        }

    private val mode: Repository.CheckoutMode
        get() = when (viewBinding.modeRadioGroup.checkedRadioButtonId) {
            R.id.mode_payment_button -> Repository.CheckoutMode.Payment
            R.id.mode_payment_with_setup_button -> Repository.CheckoutMode.PaymentWithSetup
            else -> Repository.CheckoutMode.Setup
        }

    private val setShippingAddress: Boolean
        get() = viewBinding.shippingRadioGroup.checkedRadioButtonId == R.id.shipping_on_button

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var flowController: PaymentSheet.FlowController
    private lateinit var paymentLauncher: PaymentLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        flowController = PaymentSheet.FlowController.create(
            this,
            ::onPaymentOption,
            ::onPaymentSheetResult
        )

        val publishableKey =
            "pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C"
        paymentLauncher = PaymentLauncher.create(
            this as ComponentActivity,
            publishableKey,
            null,
            ::onPaymentResult
        )

        ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            items
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            viewBinding.confirmLpmSelector.adapter = adapter
        }

        viewBinding.reloadButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.prepareCheckout(customer, currency, mode, setShippingAddress)
            }
        }

        viewBinding.completeCheckoutButton.setOnClickListener {
            startCompleteCheckout()
        }

        viewBinding.customCheckoutButton.setOnClickListener {
            flowController.confirm()
        }

        viewBinding.paymentMethod.setOnClickListener {
            flowController.presentPaymentOptions()
        }

        viewBinding.confirmButton.setOnClickListener {
            startConfirm(
                viewBinding.confirmLpmSelector.selectedItem as String,
                paymentLauncher
            )
        }

        viewModel.status.observe(this) {
            viewBinding.result.text = it
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.isInvisible = !it

            if (it) {
                singleStepUIIdlingResource.increment()
                multiStepUIIdlingResource.increment()
            } else {
                singleStepUIIdlingResource.decrement()
                multiStepUIIdlingResource.decrement()
            }
        }

        viewModel.readyToCheckout.observe(this) { isReady ->
            if (isReady) {
                viewBinding.completeCheckoutButton.isEnabled = true
                viewBinding.confirmButton.isEnabled = true
                configureCustomCheckout()
            } else {
                disableViews()
            }
        }

        viewModel.paymentMethods.observe(this) { paymentMethods ->

        }

        disableViews()
    }

    private fun disableViews() {
        viewBinding.completeCheckoutButton.isEnabled = false
        viewBinding.customCheckoutButton.isEnabled = false
        viewBinding.confirmButton.isEnabled = false
        viewBinding.paymentMethod.isClickable = false
    }

    private fun startCompleteCheckout() {
        val clientSecret = viewModel.clientSecret.value ?: return

        if (viewModel.checkoutMode == Repository.CheckoutMode.Setup) {
            paymentSheet.presentWithSetupIntent(
                clientSecret,
                makeConfiguration()
            )
        } else {
            paymentSheet.presentWithPaymentIntent(
                clientSecret,
                makeConfiguration()
            )
        }
    }

    private fun configureCustomCheckout() {
        val clientSecret = viewModel.clientSecret.value ?: return

        if (viewModel.checkoutMode == Repository.CheckoutMode.Setup) {
            flowController.configureWithSetupIntent(
                clientSecret,
                makeConfiguration(),
                ::onConfigured
            )
        } else {
            flowController.configureWithPaymentIntent(
                clientSecret,
                makeConfiguration(),
                ::onConfigured
            )
        }
    }

    private fun makeConfiguration(): PaymentSheet.Configuration {
        val defaultBilling = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(
                line1 = "123 Main Street",
                line2 = null,
                city = "Blackrock",
                state = "Co. Dublin",
                postalCode = "T37 F8HK",
                country = "IE",
            ),
            email = "email@email.com",
            name = "Jenny Rosen",
            phone = "+18008675309"
        ).takeIf { viewBinding.defaultBillingOnButton.isChecked }

        return PaymentSheet.Configuration(
            merchantDisplayName = merchantName,
            customer = viewModel.customerConfig.value,
            googlePay = googlePayConfig,
            defaultBillingDetails = defaultBilling
        )
    }

    private fun startConfirm(
        selectedLpm: String,
        paymentLauncher: PaymentLauncher
    ) {
        val paramMap =
            params[selectedLpm]?.toParamMap()
        PaymentMethod.Type.fromCode(paramMap?.get("type") as String)?.let {
            createAndConfirmPaymentIntent(
                PaymentMethodCreateParams.createWithOverride(it, paramMap, setOf("test")),
                paymentLauncher
            )
        }
    }

    private fun createAndConfirmPaymentIntent(
        params: PaymentMethodCreateParams,
        paymentLauncher: PaymentLauncher
    ) {
        if (viewModel.checkoutMode == Repository.CheckoutMode.Setup) {
            val confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                    paymentMethodCreateParams = params,
                    clientSecret = viewModel.clientSecret.value ?: return,
                )
            paymentLauncher.confirm(confirmSetupIntentParams)
        } else {
            val confirmPaymentIntentParams =
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    paymentMethodCreateParams = params,
                    clientSecret = viewModel.clientSecret.value ?: return
                )
            paymentLauncher.confirm(confirmPaymentIntentParams)
        }
    }

    private fun onPaymentResult(paymentResult: PaymentResult) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                viewModel.status.value = "PaymentIntent confirmation succeeded"
            }
            is PaymentResult.Canceled -> {
                viewModel.status.value = "PaymentIntent confirmation cancelled"
            }
            is PaymentResult.Failed -> {
                viewModel.status.value = "PaymentIntent confirmation failed with " +
                    "throwable ${paymentResult.throwable}"
            }
        }
    }

    private fun onConfigured(success: Boolean, error: Throwable?) {
        if (success) {
            viewBinding.paymentMethod.isClickable = true
            onPaymentOption(flowController.getPaymentOption())
        } else {
            viewModel.status.value =
                "Failed to configure PaymentSheetFlowController: ${error?.message}"
        }
    }

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        if (paymentOption != null) {
            viewBinding.paymentMethod.text = paymentOption.label
            viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
                paymentOption.drawableResourceId,
                0,
                0,
                0
            )
            viewBinding.customCheckoutButton.isEnabled = true
        } else {
            viewBinding.paymentMethod.setText(R.string.select)
            viewBinding.paymentMethod.setCompoundDrawables(null, null, null, null)
            viewBinding.customCheckoutButton.isEnabled = false
        }
    }

    internal open fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        if (paymentResult !is PaymentSheetResult.Canceled) {
            disableViews()
        }

        viewModel.status.value = when (paymentResult) {
            PaymentSheetResult.Canceled -> "Cancelled"
            PaymentSheetResult.Completed -> "Completed"
            is PaymentSheetResult.Failed -> paymentResult.error.toString()
        }
    }

    companion object {
        private const val merchantName = "Example, Inc."
        val singleStepUIIdlingResource = CountingIdlingResource("singleStepUI")
        val multiStepUIIdlingResource = CountingIdlingResource("multiStepUI")

        /**
         * See https://stripe.com/docs/payments/3d-secure#three-ds-cards for more options.
         */
        private val confirmParams3ds2 =
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.Builder()
                    .setNumber("4000000000003238")
                    .setExpiryMonth(1)
                    .setExpiryYear(2025)
                    .setCvc("123")
                    .build()
            )

        private val confirmParams3ds1 =
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.Builder()
                    .setNumber("4000000000003063")
                    .setExpiryMonth(1)
                    .setExpiryYear(2025)
                    .setCvc("123")
                    .build()
            )

        private val SHIPPING = ConfirmPaymentIntentParams.Shipping(
            address = Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build(),
            name = "Jenny Rosen",
            carrier = "Fedex",
            trackingNumber = "12345"
        )

        private val billingDetails = PaymentMethod.BillingDetails(
            address = Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build(),
            name = "Jenny Rosen",
            email = "jrosen@rosen.com",
            phone = "555-5555"
        )


        val params = mapOf(
            "3DS1" to confirmParams3ds1,
            "3DS2" to confirmParams3ds2,
            "sofort" to PaymentMethodCreateParams.Companion.create(
                PaymentMethodCreateParams.Sofort("AT"),
                billingDetails
            ),
            "bancontact" to PaymentMethodCreateParams.createBancontact(billingDetails),

            // TODO: Merchant country needs to be one of a set
            "bacsDebit" to PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.BacsDebit("00012345", "108800"),
                billingDetails
            ),
            // payment_method_data[type] not supported
            "blik" to PaymentMethodCreateParams.createBlik(
                billingDetails
            ),
            "sepa" to PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.SepaDebit("DE89370400440532013000"),
                billingDetails
            ),
            "eps" to PaymentMethodCreateParams.createEps(
                billingDetails,
            ),
            "giropay" to PaymentMethodCreateParams.createGiropay(
                billingDetails,
            ),
//            "fpx" to null,
            "iDeal" to PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Ideal("abn_amro"),
                billingDetails,
            ),
            "p24" to PaymentMethodCreateParams.createP24(
                billingDetails,
            ),
            // unkown parameter payment_method_data[netbanking]
//            "netbanking" to PaymentMethodCreateParams.create(
//                PaymentMethodCreateParams.Netbanking("NETBANKING_BANK"), // Field type?
//                billingDetails
//            ),
            // unkown parameter payment_method_data[netbanking]
//            "upi" to PaymentMethodCreateParams.create(
//                PaymentMethodCreateParams.Upi("VPA"), // Field type?
//                billingDetails
//            ),

            // Not a valid bsb number
//            "auBecsDebit" to PaymentMethodCreateParams.create(
//                PaymentMethodCreateParams.AuBecsDebit(
//                    "000-000",
//                    "000123456"
//                ),
//                billingDetails
//            ),
            "afterPayClearPay" to PaymentMethodCreateParams.createAfterpayClearpay(billingDetails),
            "Alipay" to PaymentMethodCreateParams.createAlipay(),
            "grabPay" to PaymentMethodCreateParams.createGrabPay(
                billingDetails
            ),
            "oxxo" to PaymentMethodCreateParams.createOxxo(
                billingDetails
            ),
            "paypal" to PaymentMethodCreateParams.createPayPal(),
            "wechat" to PaymentMethodCreateParams.createWeChatPay(billingDetails),
        )

        val items = params.keys.toList()

    }
}
