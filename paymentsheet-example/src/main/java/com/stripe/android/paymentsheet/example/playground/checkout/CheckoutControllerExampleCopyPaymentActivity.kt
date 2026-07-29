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
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import kotlinx.coroutines.launch

/** The payment step for the copied Checkout Controller integration. */
internal class CheckoutControllerExampleCopyPaymentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = requireNotNull(CheckoutControllerExampleCopyControllerStore.controller) {
            "Checkout Controller must be configured before opening the payment activity."
        }
        val presenter = controller.createPresenter(this)
        val paymentElement = presenter.paymentElement()

        lifecycleScope.launch {
            controller.checkoutSession.collect { session ->
                if (session?.status == CheckoutSession.Status.Complete) {
                    Toast.makeText(this@CheckoutControllerExampleCopyPaymentActivity, "Payment complete!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        setContent {
            val session by controller.checkoutSession.collectAsState()
            PlaygroundTheme(
                content = {
                    session?.let { checkoutSession ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CopyExpressCheckoutSection(
                                session = checkoutSession,
                                content = { presenter.expressCheckoutElement().Content() },
                            )
                            paymentElement.PaymentOptionsContent()
                        }
                    }
                },
                bottomBarContent = {
                    CopyPaymentOptionRow(session?.paymentOptionDisplayData)
                    Button(
                        onClick = { paymentElement.presentPaymentOptions() },
                        enabled = session != null,
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
