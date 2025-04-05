package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.Stripe
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.Settings
import com.stripe.example.databinding.PaymentAuthActivityBinding

/**
 * An example of creating a PaymentIntent, then confirming it with [Stripe.confirmPayment]
 */
class PaymentAuthActivity : StripeIntentActivity() {

    private val viewBinding: PaymentAuthActivityBinding by lazy {
        PaymentAuthActivityBinding.inflate(layoutInflater)
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    private var usePaymentLauncher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))
        viewModel.requiresActionSecret.observe(this, {
            println("3ds2 intent secret: $it")
            if (usePaymentLauncher) {
                paymentLauncher.handleNextActionForPaymentIntent(it)
            } else {
                viewModel.stripe.handleNextActionForPayment(this, it)
            }

        })

        val stripeAccountId = Settings(this).stripeAccountId

        val uiCustomization =
            PaymentAuthConfig.Stripe3ds2UiCustomization.Builder().build()
        PaymentAuthConfig.init(
            PaymentAuthConfig.Builder()
                .set3ds2Config(
                    PaymentAuthConfig.Stripe3ds2Config.Builder()
                        .setTimeout(6)
                        .setUiCustomization(uiCustomization)
                        .build()
                )
                .build()
        )

        viewBinding.confirmWith3ds1Button.setOnClickListener {
            createAndConfirmPaymentIntent(
                "us",
                confirmParams3ds1,
                stripeAccountId = stripeAccountId
            )
        }
        viewBinding.confirmWith3ds2Button.setOnClickListener {
            createAndConfirmPaymentIntent(
                "us",
                confirmParams3ds2,
                shippingDetails = SHIPPING,
                stripeAccountId = stripeAccountId
            )
        }

        viewBinding.confirmWithNewCardButton.setOnClickListener {
            keyboardController.hide()
            viewBinding.cardInputWidget.paymentMethodCreateParams?.let {
                createPaymentMethod(it)
            }
        }

        viewBinding.confirmWithPaymentLauncher.setOnClickListener {
            keyboardController.hide()
            usePaymentLauncher = true
            viewBinding.cardInputWidget.paymentMethodCreateParams?.let {
                createPaymentMethod(it)
            }
        }

        viewBinding.confirmWithStripeKt.setOnClickListener {
            keyboardController.hide()
            usePaymentLauncher = false
            viewBinding.cardInputWidget.paymentMethodCreateParams?.let {
                createPaymentMethod(it)
            }
        }

        viewBinding.setupButton.setOnClickListener {
            createAndConfirmSetupIntent(
                "us",
                confirmParams3ds2,
                stripeAccountId = stripeAccountId
            )
        }
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmWith3ds2Button.isEnabled = enable
        viewBinding.confirmWith3ds1Button.isEnabled = enable
        viewBinding.confirmWithNewCardButton.isEnabled = enable
        viewBinding.setupButton.isEnabled = enable
    }

    private companion object {

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
    }
}
