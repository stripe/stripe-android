package com.stripe.android.paymentsheet.example.samples.ui.messagingelement

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentmethodmessaging.view.messagingelement.PaymentMethodMessagingElement
import com.stripe.android.paymentsheet.example.playground.activity.IncrementDecrementItem
import kotlinx.coroutines.flow.collectLatest

internal class MessagingElementActivity : AppCompatActivity() {

    private val viewModel by viewModels<MessagingElementViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {

            val font = PaymentMethodMessagingElement.Appearance.Font()
            var theme = PaymentMethodMessagingElement.Appearance.Theme.LIGHT
            val colors = PaymentMethodMessagingElement.Appearance.Colors()
            val amount = remember { mutableStateOf(0f) }
            var currency = remember {  mutableStateOf("usd") }
            var locale = remember {  mutableStateOf("en") }
            var countryCode = remember {  mutableStateOf("US") }
            var paymentMethods = remember { mutableStateOf("affirm,klarna,afterpay_clearpay") }
            val appearance = PaymentMethodMessagingElement.Appearance()

            // Element
            Column {

                Spacer(Modifier.height(400.dp))

                Box(Modifier.padding(16.dp)) {
                    viewModel.paymentMethodMessagingElement.Content(appearance)
                }

                val result by viewModel.result.collectAsState()
                ResultToast(result)

                // Config
                IncrementDecrementItem(
                    label = "amount",
                    value = amount.value,
                    incrementDecrementAmount = 1000f
                ) {
                    amount.value = it
                }

                TextField(
                    value = currency.value,
                    onValueChange = {
                        currency.value = it
                    },
                    label = { Text("currency") }
                )

                TextField(
                    value = countryCode.value,
                    onValueChange = {
                        countryCode.value = it
                    },
                    label = { Text("countryCode") }
                )

                TextField(
                    value = locale.value,
                    onValueChange = {
                        locale.value = it
                    },
                    label = { Text("locale") }
                )

                TextField(
                    value = paymentMethods.value,
                    onValueChange = {
                        paymentMethods.value = it
                    },
                    label = { Text("Payment Methods") }
                )

                Button(
                    onClick = {
                        viewModel.configurePaymentMethodMessagingElement(
                            amount = amount.value.toLong(),
                            currency = currency.value,
                            locale = locale.value,
                            countryCode = countryCode.value,
                            paymentMethods = paymentMethods.value.split(",")
                        )
                    }
                ) {
                    Text("Configure")
                }

//                // Appearance
//                var currentFontSize = 16f
//                IncrementDecrementItem(
//                    label = "fontSizeSp",
//                    value = currentFontSize
//                ) {
//                    font.fontSizeSp(it)
//                    currentFontSize = it
//                }
//
//                var currentFontWeight = 200f
//                IncrementDecrementItem(
//                    label = "fontWeight",
//                    value = currentFontWeight,
//                    incrementDecrementAmount = 100f
//                ) {
//                    currentFontWeight = it
//                    font.fontWeight(it.toInt())
//                }
//
//                var currentLetterSpacingSp = 16f
//                IncrementDecrementItem(
//                    label = "letterSpacingSp",
//                    value = currentLetterSpacingSp
//                ) {
//                    font.letterSpacingSp(it)
//                    currentLetterSpacingSp = it
//                }
//
//                var currentFontFamily: Int? = R.font.opensans
//                FontDropDown(
//                    fontResId = currentFontFamily
//                ) {
//                    font.fontFamily(it)
//                    currentFontFamily = it
//                }
//
//                ColorItem(
//                    label = "textColor",
//                    currentColor = StripeThemeDefaults.colorsLight.onComponent,
//                    onColorPicked = { colors.textColor(it.toArgb()) }
//                ) { }
//
//                var currentColor = StripeThemeDefaults.colorsLight.subtitle
//                ColorItem(
//                    label = "infoIconColor",
//                    currentColor = currentColor,
//                    onColorPicked = {
//                        colors.infoIconColor(it.toArgb())
//                        currentColor = it
//                    }
//                ) { }
//
//                ThemeDropDown(theme) {
//                    theme = it
//                }
//
//                Button(
//                    onClick = {
//                        appearance
//                            .theme(theme)
//                            .font(font)
//                            .colors(colors)
//                    }
//                ) {
//                    Text("Update Appearance")
//                }
            }
        }
    }

    @Composable
    private fun ResultToast(result: PaymentMethodMessagingElement.Result?) {
        val context = LocalContext.current
        result?.let { Toast.makeText(context, it.toString(), Toast.LENGTH_LONG).show() }
    }

    @Composable
    private fun ThemeDropDown(
        theme: PaymentMethodMessagingElement.Appearance.Theme,
        themeSelectedCallback: (PaymentMethodMessagingElement.Appearance.Theme) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.TopStart)
        ) {
            Text(
                text = "Theme: $theme",
                fontSize = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { expanded = true })
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                PaymentMethodMessagingElement.Appearance.Theme.entries.forEach {
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            themeSelectedCallback(it)
                        }
                    ) {
                        Text(it.name)
                    }
                }
            }
        }
    }
}
