package com.stripe.android.paymentmethodmessaging.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            MaterialTheme(
                colors = if (isSystemInDarkTheme()) darkColors() else lightColors()
            ) {
                Column(
                    Modifier
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "PaymentMethodMessagingElement",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )

                    val appearanceSettings by viewModel.appearanceSetting.collectAsState()
                    val appearance = appearanceSettings.toAppearance()

                    Box(Modifier.padding(vertical = 8.dp)) {
                        viewModel.paymentMethodMessagingElement.Content(appearance)
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
            label = { Text("paymentMethods") }
        )

        TextField(
            value = config.publishableKey,
            onValueChange = {
                viewModel.updateConfigState(config.copy(publishableKey = it))
            },
            label = { Text("publishableKey") }
        )

        TextField(
            value = config.stripeAccountId ?: "",
            onValueChange = {
                viewModel.updateConfigState(config.copy(stripeAccountId = it))
            },
            label = { Text("stripeAccountID") }
        )

        val appearanceSettings by viewModel.appearanceSetting.collectAsState()

        AppearanceDropDown(
            items = fontList(),
            selectedItem = appearanceSettings.fontSettings,
            label = "Font",
            itemToString = { it.label }
        ) {
            viewModel.updateFont(it)
        }

        AppearanceDropDown(
            items = colorList(),
            selectedItem = appearanceSettings.colorsSettings.textColor,
            label = "Text Color",
            itemToString = { it.name }
        ) {
            viewModel.updateColors(appearanceSettings.colorsSettings.copy(textColor = it))
        }

        AppearanceDropDown(
            items = colorList(),
            selectedItem = appearanceSettings.colorsSettings.iconColor,
            label = "Icon Color",
            itemToString = { it.name }
        ) {
            viewModel.updateColors(appearanceSettings.colorsSettings.copy(iconColor = it))
        }

        AppearanceDropDown(
            items = listOf(
                PaymentMethodMessagingElement.Appearance.Theme.LIGHT,
                PaymentMethodMessagingElement.Appearance.Theme.DARK,
                PaymentMethodMessagingElement.Appearance.Theme.FLAT,
            ),
            selectedItem = appearanceSettings.themeSettings,
            label = "Theme",
            itemToString = { it.toString() }
        ) {
            viewModel.updateTheme(it)
        }
    }


    @Composable
    private fun <T> AppearanceDropDown(
        items: List<T>,
        selectedItem: T,
        label: String,
        itemToString: (T) -> String,
        onItemSelected: (T) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }

        Text(
            text = "$label: " + itemToString(selectedItem),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(4.dp).clickable { expanded = true },
            color = MaterialTheme.colors.onSurface
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onItemSelected(it)
                    }
                ) {
                    Text(itemToString(it))
                }
            }
        }
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

    private fun fontList() = listOf(
        MessagingElementViewModel.FontSettings(
            fontFamily = R.font.cursive,
            fontSize = 20f,
            letterSpacing = 4f,
            fontWeight = 200,
            label = "Cursive"
        ),
        MessagingElementViewModel.FontSettings(
            fontFamily = R.font.opensans,
            fontSize = 32f,
            letterSpacing = 8f,
            fontWeight = 400,
            label = "Open Sans big"
        ),
        MessagingElementViewModel.FontSettings(),
    )

    private fun colorList() = listOf(
        MessagingElementViewModel.ColorInfo(Color.Red, "Red"),
        MessagingElementViewModel.ColorInfo(Color.Blue, "Blue"),
        MessagingElementViewModel.ColorInfo(Color.Black, "Black")
    )
}
