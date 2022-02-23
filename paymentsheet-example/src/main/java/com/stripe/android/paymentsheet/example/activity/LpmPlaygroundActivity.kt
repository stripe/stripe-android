package com.stripe.android.paymentsheet.example.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.databinding.ActivityLpmPlaygroundBinding
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.viewmodel.LpmPlaygroundViewModel
import kotlinx.coroutines.launch

internal open class LpmPlaygroundActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityLpmPlaygroundBinding.inflate(layoutInflater)
    }

    val viewModel: LpmPlaygroundViewModel by lazy {
        LpmPlaygroundViewModel(application)
    }

    private val customer: CheckoutCustomer
        get() = when (viewBinding.customerRadioGroup.checkedRadioButtonId) {
            R.id.guest_customer_button -> CheckoutCustomer.Guest
            R.id.new_customer_button -> {
                viewModel.temporaryCustomerId?.let {
                    CheckoutCustomer.WithId(it)
                } ?: CheckoutCustomer.New
            }
            else -> CheckoutCustomer.Returning
        }

    private val currency: CheckoutCurrency
        get() = when (viewBinding.currencyRadioGroup.checkedRadioButtonId) {
            R.id.currency_usd_button -> CheckoutCurrency.USD
            else -> CheckoutCurrency.EUR
        }

    private val mode: CheckoutMode
        get() = when (viewBinding.modeRadioGroup.checkedRadioButtonId) {
            R.id.mode_payment_button -> CheckoutMode.Payment
            R.id.mode_payment_with_setup_button -> CheckoutMode.PaymentWithSetup
            else -> CheckoutMode.Setup
        }

    private val setShippingAddress: Boolean
        get() = viewBinding.shippingRadioGroup.checkedRadioButtonId == R.id.shipping_on_button

    private lateinit var paymentLauncher: PaymentLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val publishableKey =
            "pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C"

        paymentLauncher = PaymentLauncher.create(this as ComponentActivity, publishableKey, null) {
            when (it) {
                is PaymentResult.Completed -> {
                    viewModel.status.value += "\n\nPaymentIntent confirmation succeeded\n\n"
                    viewModel.inProgress.value = false
                }
                is PaymentResult.Canceled -> {
                    viewModel.status.value += "\n\nPaymentIntent confirmation cancelled\n\n"
                    viewModel.inProgress.value = false
                }
                is PaymentResult.Failed -> {
                    viewModel.status.value += "\n\nPaymentIntent confirmation failed with " +
                        "throwable ${it.throwable} \n\n"
                    viewModel.inProgress.value = false
                }
            }
        }

        viewBinding.reloadButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.prepareCheckout(
                    customer,
                    currency,
                    mode,
                    setShippingAddress,
                    false,
                    "https://stripe-mobile-payment-sheet-test-playground-v6.glitch.me/"
                )
            }
        }

        viewBinding.confirmButton.setOnClickListener {
            startConfirm(
                items.indexOf("afterPayClearPay"),
                paymentLauncher
            )
        }

        viewModel.status.observe(this) {
            viewBinding.result.text = it
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.isInvisible = !it
        }

        viewModel.readyToCheckout.observe(this) { isReady ->
            if (isReady) {
                viewBinding.confirmButton.isEnabled = true
            } else {
                disableViews()
            }
        }

        disableViews()
    }

    private fun disableViews() {
        viewBinding.confirmButton.isEnabled = false
    }

    private fun startConfirm(
        selectedIndex: Int,
        paymentLauncher: PaymentLauncher
    ) {
        val paramMap = Companion.params[items[selectedIndex]]?.toParamMap()
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
        if (viewModel.checkoutMode == CheckoutMode.Setup) {
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

    companion object {
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
