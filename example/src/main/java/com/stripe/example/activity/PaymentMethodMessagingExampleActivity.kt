package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.compose.material.Text
import com.stripe.android.PaymentConfiguration
import com.stripe.android.ui.core.elements.messaging.PaymentMethodMessage
import com.stripe.android.ui.core.elements.messaging.PaymentMethodMessagingView
import com.stripe.android.ui.core.elements.messaging.PaymentMethodMessageResult
import com.stripe.android.ui.core.elements.messaging.rememberMessagingState
import com.stripe.example.databinding.MessagingElementActivityBinding

class PaymentMethodMessagingExampleActivity : StripeIntentActivity() {
    private val viewBinding: MessagingElementActivityBinding by lazy {
        MessagingElementActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        /**
         * Try it yourself by using either the classic view or compose.
         * If you need inspiration, see the examples below.
         */

//        val config = PaymentMethodMessagingView.Configuration(
//            context = this@PaymentMethodMessagingExampleActivity,
//            publishableKey = PaymentConfiguration.getInstance(this@PaymentMethodMessagingExampleActivity).publishableKey,
//            stripeAccountId = PaymentConfiguration.getInstance(this@PaymentMethodMessagingExampleActivity).stripeAccountId,
//            paymentMethods = listOf(
//                PaymentMethodMessagingView.Configuration.PaymentMethod.Klarna,
//                PaymentMethodMessagingView.Configuration.PaymentMethod.AfterpayClearpay
//            ),
//            currency = "usd",
//            amount = 2499
//        )
//
//        // View
//        val messageView = viewBinding.messageView
//        val loadingView = viewBinding.messageViewLoading
//        loadingView.visibility = View.VISIBLE // Make sure loading view is showing
//
//        // Call load, don't forget to hide the loadingView and show the messageView
//        // messageView.load()
//
//        // Compose
//        viewBinding.messageComposeView.setContent {
//            // Call rememberMessagingState to get the message state
//            // val messageState = rememberMessageState()
//
//            // Switch on messageState to render the different states
//            // when (messageState) {
//            //    ...
//            //    PaymentMethodMessageResult.Loading -> show loading
//            //    ...
//            // }
//        }

        /**
         * View Example
         */
        viewBinding.messageViewLoading.visibility = View.VISIBLE
        viewBinding.messageView.load(
            config = PaymentMethodMessagingView.Configuration(
                context = this@PaymentMethodMessagingExampleActivity,
                publishableKey = PaymentConfiguration.getInstance(this@PaymentMethodMessagingExampleActivity).publishableKey,
                stripeAccountId = PaymentConfiguration.getInstance(this@PaymentMethodMessagingExampleActivity).stripeAccountId,
                paymentMethods = listOf(
                    PaymentMethodMessagingView.Configuration.PaymentMethod.Klarna,
                    PaymentMethodMessagingView.Configuration.PaymentMethod.AfterpayClearpay
                ),
                currency = "usd",
                amount = 2499
            ),
            onSuccess = {
                viewBinding.messageViewLoading.visibility = View.GONE
                viewBinding.messageView.visibility = View.VISIBLE
            },
            onFailure = {
                viewBinding.messageViewLoading.visibility = View.GONE
                // Error View
            }
        )

        /**
         * Compose Example
         */
        viewBinding.messageComposeView.setContent {
            val config = PaymentMethodMessagingView.Configuration(
                context = this@PaymentMethodMessagingExampleActivity,
                publishableKey = PaymentConfiguration.getInstance(this@PaymentMethodMessagingExampleActivity).publishableKey,
                stripeAccountId = PaymentConfiguration.getInstance(this@PaymentMethodMessagingExampleActivity).stripeAccountId,
                paymentMethods = listOf(
                    PaymentMethodMessagingView.Configuration.PaymentMethod.Klarna,
                    PaymentMethodMessagingView.Configuration.PaymentMethod.AfterpayClearpay
                ),
                currency = "usd",
                amount = 2499
            )
            when (val messageState = rememberMessagingState(config).value) {
                is PaymentMethodMessageResult.Loading -> {
                    Text(text = "Loading...")
                }
                is PaymentMethodMessageResult.Failure -> {
                    Text(text = "Error")
                }
                is PaymentMethodMessageResult.Success -> {
                    PaymentMethodMessage(
                        data = messageState.data
                    )
                }
            }
        }
    }
}
