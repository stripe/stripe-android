package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.stripe.android.Stripe
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
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

        viewBinding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewBinding.cardInputWidget.isVisible = position == 0
                viewBinding.cardMultilineWidget.isVisible = position == 1
                viewBinding.cardFormView.isVisible = position == 2
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        viewBinding.spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayListOf("CardInputWidget", "CardMultilineWidget", "CardFormView")
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        viewBinding.preferredNetworkSwitch.setOnCheckedChangeListener { _, isChecked ->
            val networks = if (isChecked) {
                listOf(CardBrand.CartesBancaires)
            } else {
                emptyList()
            }

            viewBinding.cardInputWidget.setPreferredNetworks(networks)
            viewBinding.cardMultilineWidget.setPreferredNetworks(networks)
            viewBinding.cardFormView.setPreferredNetworks(networks)
        }

        val stripeAccountId = Settings(this).stripeAccountId

        viewBinding.confirmWithNewCardButton.setOnClickListener {
            viewBinding.root.clearFocus()

            val params = when (viewBinding.spinner.selectedItemPosition) {
                0 -> viewBinding.cardInputWidget.paymentMethodCreateParams
                1 -> viewBinding.cardMultilineWidget.paymentMethodCreateParams
                2 -> viewBinding.cardFormView.paymentMethodCreateParams
                else -> error("bla")
            }

            if (params != null) {
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
