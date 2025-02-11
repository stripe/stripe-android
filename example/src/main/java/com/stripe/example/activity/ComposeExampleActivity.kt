package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.module.StripeIntentViewModel

/**
 * An Activity to demonstrate [PaymentLauncher] with Jetpack Compose.
 */
class ComposeExampleActivity : AppCompatActivity() {
    private val viewModel: StripeIntentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeScreen()
        }
    }

    @Composable
    fun ComposeScreen() {
        val inProgress by viewModel.inProgress.observeAsState(false)
        val status by viewModel.status.observeAsState("")

        val context = LocalContext.current
        val settings = remember { Settings(context) }

        val paymentLauncher = rememberPaymentLauncher(
            publishableKey = settings.publishableKey,
            stripeAccountId = settings.stripeAccountId,
            callback = ::onPaymentResult
        )

        ComposeScreen(
            inProgress = inProgress,
            status = status,
            onConfirm = { paymentLauncher.confirm(it) }
        )
    }

    @Composable
    private fun ComposeScreen(
        inProgress: Boolean,
        status: String,
        onConfirm: (ConfirmPaymentIntentParams) -> Unit
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            if (inProgress) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                stringResource(R.string.payment_auth_intro),
                modifier = Modifier.padding(vertical = 5.dp)
            )
            ConfirmButton(
                params = confirmParams3ds1,
                buttonName = R.string.confirm_with_3ds1_button,
                onConfirm = onConfirm,
                inProgress = inProgress
            )
            ConfirmButton(
                params = confirmParams3ds2,
                buttonName = R.string.confirm_with_3ds2_button,
                onConfirm = onConfirm,
                inProgress = inProgress
            )
            Divider(modifier = Modifier.padding(vertical = 5.dp))
            Text(text = status)
        }
    }

    private fun onPaymentResult(paymentResult: PaymentResult) {
        when (paymentResult) {
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
                    "throwable ${paymentResult.throwable} \n\n"
                viewModel.inProgress.value = false
            }
        }
    }

    @Composable
    private fun ConfirmButton(
        params: PaymentMethodCreateParams,
        @StringRes buttonName: Int,
        inProgress: Boolean,
        onConfirm: (ConfirmPaymentIntentParams) -> Unit
    ) {
        Button(
            onClick = { createAndConfirmPaymentIntent(params, onConfirm) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            enabled = !inProgress
        ) {
            Text(stringResource(buttonName))
        }
    }

    private fun createAndConfirmPaymentIntent(
        params: PaymentMethodCreateParams,
        onConfirm: (ConfirmPaymentIntentParams) -> Unit
    ) {
        viewModel.createPaymentIntent("us").observe(
            this
        ) {
            it.onSuccess { responseData ->
                val confirmPaymentIntentParams =
                    ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                        paymentMethodCreateParams = params,
                        clientSecret = responseData.getString("secret"),
                        shipping = SHIPPING
                    )
                onConfirm(confirmPaymentIntentParams)
            }
        }
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
                    .setExpiryYear(2045)
                    .setCvc("123")
                    .build()
            )

        private val confirmParams3ds1 =
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.Builder()
                    .setNumber("4000000000003063")
                    .setExpiryMonth(1)
                    .setExpiryYear(2045)
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
