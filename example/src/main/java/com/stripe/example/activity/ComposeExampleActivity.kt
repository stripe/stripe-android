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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
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

        createPaymentLauncher().let { paymentLauncher ->
            Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                if (inProgress) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    stringResource(R.string.payment_auth_intro),
                    modifier = Modifier.padding(vertical = 5.dp),
                )
                ConfirmButton(
                    params = confirmParams3ds1,
                    buttonName = R.string.confirm_with_3ds1_button,
                    paymentLauncher = paymentLauncher,
                    inProgress = inProgress
                )
                ConfirmButton(
                    params = confirmParams3ds2,
                    buttonName = R.string.confirm_with_3ds2_button,
                    paymentLauncher = paymentLauncher,
                    inProgress = inProgress
                )
                Divider(modifier = Modifier.padding(vertical = 5.dp))
                Text(text = status)
            }
        }
    }

    /**
     * Create [PaymentLauncher] in a [Composable]
     */
    @Composable
    fun createPaymentLauncher(): PaymentLauncher {
        val settings = Settings(LocalContext.current)
        return PaymentLauncher.createForCompose(
            publishableKey = settings.publishableKey,
            stripeAccountId = settings.stripeAccountId
        ) {
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
    }

    @Composable
    fun ConfirmButton(
        params: PaymentMethodCreateParams,
        @StringRes buttonName: Int,
        paymentLauncher: PaymentLauncher,
        inProgress: Boolean
    ) {
        Button(
            onClick = { createAndConfirmPaymentIntent(params, paymentLauncher) },
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
        paymentLauncher: PaymentLauncher,
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
                paymentLauncher.confirm(confirmPaymentIntentParams)
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
