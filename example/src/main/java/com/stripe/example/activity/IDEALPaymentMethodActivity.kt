package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams

class IDEALPaymentMethodActivity : StripeIntentActivity() {
    private val bankNameMap = mapOf(
        "abn_amro" to "ABN AMRO",
        "asn_bank" to "ASN Bank",
        "bunq" to "Bunq",
        "handelsbanken" to "Handelsbanken",
        "ing" to "ING",
        "knab" to "Knab",
        "moneyou" to "Moneyou",
        "rabobank" to "Rabobank",
        "revolut" to "Revolut",
        "regiobank" to "RegioBank",
        "sns_bank" to "SNS Bank (De Volksbank)",
        "triodos_bank" to "Triodos Bank",
        "van_lanschot" to "Van Lanschot"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            setContent {
                IDEALScreen()
            }
        }
    }

    @Composable
    private fun IDEALScreen() {
        val inProgress by viewModel.inProgress.observeAsState(false)
        val status by viewModel.status.observeAsState("")
        val scrollState = rememberScrollState()
        val name = remember { mutableStateOf("Johnny Lawrence") }
        var selectedBank by remember { mutableStateOf(bankNameMap.firstNotNullOf { it.key }) }
        var expandedBankDropdown by remember { mutableStateOf(false) }

        if (inProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (status.isNotEmpty()) {
            Text(text = status)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .verticalScroll(scrollState)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    value = name.value,
                    maxLines = 1,
                    onValueChange = { name.value = it }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = bankNameMap[selectedBank] ?: "",
                        onValueChange = {},
                        enabled = false,
                        trailingIcon = {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colors.onBackground,
                                modifier = Modifier.height(24.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { expandedBankDropdown = true })
                    )
                    DropdownMenu(
                        expanded = expandedBankDropdown,
                        onDismissRequest = { expandedBankDropdown = false }
                    ) {
                        bankNameMap.forEach {
                            DropdownMenuItem(onClick = {
                                selectedBank = it.key
                                expandedBankDropdown = false
                            }) {
                                Text(it.value)
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        createAndConfirmPaymentIntent(
                            country = "NL",
                            paymentMethodCreateParams = PaymentMethodCreateParams.create(
                                ideal = PaymentMethodCreateParams.Ideal(selectedBank),
                                billingDetails = PaymentMethod.BillingDetails(
                                    name = name.value,
                                    phone = "1-800-555-1234",
                                    email = "jrosen@example.com",
                                    address = Address.Builder()
                                        .setCity("San Francisco")
                                        .setCountry("US")
                                        .setLine1("123 Market St")
                                        .setLine2("#345")
                                        .setPostalCode("94107")
                                        .setState("CA")
                                        .build()
                                )
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm with iDEAL")
                }
            }
        }
    }
}
