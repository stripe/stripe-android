package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.compose.material.Text
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessaging
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingState
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingView
import com.stripe.android.paymentmethodmessaging.view.rememberMessagingState
import com.stripe.example.databinding.PaymentMethodMessagingActivityBinding

class PaymentMethodMessagingExampleActivity : StripeIntentActivity() {
    private val viewBinding: PaymentMethodMessagingActivityBinding by lazy {
        PaymentMethodMessagingActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val config = PaymentMethodMessagingView.Configuration(
            publishableKey = PaymentConfiguration.getInstance(this).publishableKey,
            paymentMethods = setOf(
                PaymentMethodMessagingView.Configuration.PaymentMethod.Klarna,
                PaymentMethodMessagingView.Configuration.PaymentMethod.AfterpayClearpay
            ),
            currency = "USD",
            amount = 2999,
            fontFamily = null
        )

        /**
         * View Example
         */
        viewBinding.messageViewLoading.visibility = View.VISIBLE
        viewBinding.messageView.load(
            config = config,
            onSuccess = {
                viewBinding.messageViewLoading.visibility = View.GONE
                viewBinding.messageView.visibility = View.VISIBLE
            },
            onFailure = {
                viewBinding.messageViewLoading.visibility = View.GONE
                viewBinding.errorView.visibility = View.VISIBLE
            }
        )

        /**
         * Compose Example
         */
        viewBinding.messageComposeView.setContent {
            when (val messageState = rememberMessagingState(config).value) {
                is PaymentMethodMessagingState.Loading -> {
                    Text(text = "Loading...")
                }
                is PaymentMethodMessagingState.Failure -> {
                    Text(text = "Error")
                }
                is PaymentMethodMessagingState.Success -> {
                    PaymentMethodMessaging(
                        data = messageState.data
                    )
                }
            }
        }
    }
}