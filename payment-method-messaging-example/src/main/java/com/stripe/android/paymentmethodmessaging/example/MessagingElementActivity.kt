package com.stripe.android.paymentmethodmessaging.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElement
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElementPreview

@OptIn(PaymentMethodMessagingElementPreview::class)
internal class MessagingElementActivity : AppCompatActivity() {

    private val viewModel by viewModels<MessagingElementViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(
                Modifier.padding(8.dp)
            ) {
                Text(
                    text = "PaymentMethodMessagingElement",
                    style = MaterialTheme.typography.h6
                )

                Box(Modifier.padding(vertical = 8.dp)) {
                    viewModel.paymentMethodMessagingElement.Content()
                }

                val config by viewModel.config.collectAsState()
                ConfigurationSettings(config)

                Button(
                    onClick = {
                        viewModel.configurePaymentMethodMessagingElement()
                    },
                    Modifier.fillMaxWidth()
                ) {
                    Text("Configure")
                }

                val result by viewModel.configureResult.collectAsState()
                ResultToast(result)
            }
        }
    }

    @Composable
    fun ConfigurationSettings(config: MessagingElementViewModel.Config) {
        TextField(
            value = config.amount.toString(),
            onValueChange = {
                if (it.isDigitsOnly()) {
                    viewModel.updateConfigState(config.copy(amount = it.toLong()))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("amount") }
        )

        TextField(
            value = config.currency,
            onValueChange = {
                viewModel.updateConfigState(config.copy(currency = it))
            },
            label = { Text("currency") }
        )

        TextField(
            value = config.countryCode,
            onValueChange = {
                viewModel.updateConfigState(config.copy(countryCode = it))
            },
            label = { Text("countryCode") }
        )

        TextField(
            value = config.locale,
            onValueChange = {
                viewModel.updateConfigState(config.copy(locale = it))
            },
            label = { Text("locale") }
        )

        TextField(
            value = config.paymentMethods.joinToString(","),
            onValueChange = {
                viewModel.updateConfigState(config.copy(paymentMethods = it.split(",")))
            },
            label = { Text("Payment Methods") }
        )
    }

    @Composable
    private fun ResultToast(configureResult: PaymentMethodMessagingElement.ConfigureResult?) {
        val context = LocalContext.current
        val message = when (configureResult) {
            is PaymentMethodMessagingElement.ConfigureResult.Failed -> "Failed ${configureResult.error}"
            is PaymentMethodMessagingElement.ConfigureResult.NoContent -> "NoContent"
            is PaymentMethodMessagingElement.ConfigureResult.Succeeded -> "Succeeded"
            null -> null
        }
        message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
}
