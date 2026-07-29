@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import kotlinx.coroutines.launch

/** The payment step for the copied Checkout Controller integration. */
internal class CheckoutControllerExampleCopyPaymentActivity : AppCompatActivity() {
    private val viewModel: CheckoutControllerExampleCopyViewModel by viewModels {
        CheckoutControllerExampleCopyViewModel.factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = viewModel.controller.createPresenter(this)
        val paymentElement = presenter.paymentElement()

        lifecycleScope.launch {
            viewModel.sessionComplete.collect {
                Toast.makeText(this@CheckoutControllerExampleCopyPaymentActivity, "Payment complete!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        setContent {
            val status by viewModel.status.collectAsState()
            PlaygroundTheme(
                content = {
                    when (val currentStatus = status) {
                        is CheckoutControllerExampleCopyViewModel.Status.Loading -> CopyLoadingContent()
                        is CheckoutControllerExampleCopyViewModel.Status.Error -> CopyErrorContent(currentStatus.message)
                        is CheckoutControllerExampleCopyViewModel.Status.Configured -> {
                            currentStatus.checkoutSession?.let { session ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    CopyExpressCheckoutSection(
                                        session = session,
                                        content = { presenter.expressCheckoutElement().Content() },
                                    )
                                    paymentElement.PaymentOptionsContent()
                                }
                            }
                        }
                    }
                },
                bottomBarContent = {
                    val configured = status as? CheckoutControllerExampleCopyViewModel.Status.Configured
                    CopyPaymentOptionRow(configured?.checkoutSession?.paymentOptionDisplayData)
                    Button(
                        onClick = { paymentElement.presentPaymentOptions() },
                        enabled = configured != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Select Payment Method") }
                    Button(
                        onClick = { presenter.confirm() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Confirm") }
                },
            )
        }
    }
}
