package com.stripe.example.activity

import android.os.Bundle
import com.stripe.android.Stripe
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.example.Settings
import com.stripe.example.databinding.CardBrandChoiceExampleActivityBinding

/**
 * An example of creating a PaymentIntent, then confirming it with [Stripe.confirmPayment]
 */
class CardBrandChoiceExampleActivity : StripeIntentActivity() {

    private val viewBinding: CardBrandChoiceExampleActivityBinding by lazy {
        CardBrandChoiceExampleActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, this::enableUi)
        viewModel.status.observe(this, viewBinding.status::setText)

        val stripeAccountId = Settings(this).stripeAccountId

        viewBinding.confirmWithNewCardButton.setOnClickListener {
            viewBinding.root.clearFocus()

            viewBinding.cardInputWidget.paymentMethodCreateParams?.let { params ->
                createAndConfirmPaymentIntent(
                    country = "fr",
                    paymentMethodCreateParams = params,
                    shippingDetails = SHIPPING,
                    stripeAccountId = stripeAccountId,
                )
            }
        }
    }

    private fun enableUi(inProgress: Boolean) {
        viewBinding.confirmWithNewCardButton.isEnabled = !inProgress
    }

    private companion object {

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
