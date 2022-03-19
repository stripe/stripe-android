package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult

class USBankAccountPaymentMethodActivity : AppCompatActivity() {
    private val viewModel: USBankAccountPaymentMethodViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observePaymentResults()

        setContent {
            USBankAccountScreen()
        }
    }

    @Composable
    private fun USBankAccountScreen() {
        val inProgress by viewModel.inProgress.observeAsState(false)
        val status by viewModel.status.observeAsState("")
        val name = remember { mutableStateOf("Johnny Lawrence") }
        val email = remember { mutableStateOf("johnny@lawrence.com") }
        val accountNumber = remember { mutableStateOf("000123456789") }
        val routingNumber = remember { mutableStateOf("110000000") }
        val isChecking = remember { mutableStateOf(true) }
        val isSavings = remember { mutableStateOf(false) }
        val isIndividual = remember { mutableStateOf(true) }
        val isCompany = remember { mutableStateOf(false) }
        val isSaveForFutureUsage = remember { mutableStateOf(false) }

        if (inProgress) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (status.isNotEmpty()) {
            Text(text = status)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    value = name.value,
                    maxLines = 1,
                    onValueChange = {
                        name.value = it
                    }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    value = email.value,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    onValueChange = {
                        email.value = it
                    }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Account number") },
                    value = accountNumber.value,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    onValueChange = {
                        accountNumber.value = it
                    }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Routing number") },
                    value = routingNumber.value,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    onValueChange = {
                        routingNumber.value = it
                    }
                )
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(text = "Account type")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isChecking.value,
                            onClick = {
                                isChecking.value = true
                                isSavings.value = false
                            }
                        )
                        Text(
                            text = "Checking",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSavings.value,
                            onClick = {
                                isChecking.value = false
                                isSavings.value = true
                            }
                        )
                        Text(
                            text = "Savings",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Column {
                    Text("Account holder type")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isIndividual.value,
                            onClick = {
                                isIndividual.value = true
                                isCompany.value = false
                            }
                        )
                        Text(
                            text = "Individual",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isCompany.value,
                            onClick = {
                                isIndividual.value = false
                                isCompany.value = true
                            }
                        )
                        Text(
                            text = "Company",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isSaveForFutureUsage.value,
                        onCheckedChange = {
                            isSaveForFutureUsage.value = it
                        }
                    )
                    Text(
                        text = "Save for future usage",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val params = PaymentMethodCreateParams.create(
                            PaymentMethodCreateParams.USBankAccount(
                                accountNumber = accountNumber.value,
                                routingNumber = routingNumber.value,
                                accountType = if (isChecking.value) {
                                    PaymentMethod.USBankAccount.USBankAccountType.CHECKING
                                } else {
                                    PaymentMethod.USBankAccount.USBankAccountType.SAVINGS
                                },
                                accountHolderType = if (isIndividual.value) {
                                    PaymentMethod.USBankAccount.USBankAccountHolderType.INDIVIDUAL
                                } else {
                                    PaymentMethod.USBankAccount.USBankAccountHolderType.COMPANY
                                }
                            ),
                            billingDetails = PaymentMethod.BillingDetails(
                                name = name.value,
                                email = email.value
                            )
                        )
                        if (isSaveForFutureUsage.value) {
                            createAndConfirmSetupIntent(params)
                        } else {
                            createAndConfirmPaymentIntent(params)
                        }
                    }
                ) {
                    Text(text = "Pay with US Bank Account")
                }
                Text(
                    text =
                    "By clicking Pay with US Bank Account, you authorize Non-Card Payment" +
                        "Examples to debit the bank account specified above for any amount owed " +
                        "for charges arising from your use of Non-Card Payment Examples’ " +
                        "services and/or purchase of products from Non-Card Payment Examples, " +
                        "pursuant to Non-Card Payment Examples’ website and terms, until this " +
                        "authorization is revoked. You may amend or cancel this authorization at " +
                        "any time by providing notice to Non-Card Payment Examples with 30 " +
                        "(thirty) days notice." +

                        "If you use Non-Card Payment Examples’ services or purchase additional" +
                        "products periodically pursuant to Non-Card Payment Examples’ terms, you" +
                        "authorize Non-Card Payment Examples to debit your bank account " +
                        "periodically. Payments that fall outside of the regular debits " +
                        "authorized above will only be debited after your authorization is " +
                        "obtained.",
                    color = Color.Gray,
                    fontSize = 8.sp
                )
            }
        }
    }

    private fun createAndConfirmSetupIntent(
        params: PaymentMethodCreateParams
    ) {
        viewModel.createSetupIntent(
            country = "us"
        ).observe(
            this
        ) {
            it.onSuccess { responseData ->
                viewModel.confirmSetupIntent(
                    params,
                    responseData.getString("secret")
                )
            }
        }
    }

    private fun createAndConfirmPaymentIntent(
        params: PaymentMethodCreateParams
    ) {
        viewModel.createPaymentIntent(
            country = "us",
            supportedPaymentMethods = "us_bank_account"
        ).observe(
            this
        ) {
            it.onSuccess { responseData ->
                viewModel.confirmPaymentIntent(
                    params,
                    responseData.getString("secret")
                )
            }
        }
    }

    private fun observePaymentResults() {
        viewModel.paymentResultLiveData.observe(this) { paymentResult ->
            when (paymentResult) {
                is PaymentResult.Completed -> {
                    viewModel.status.value += "\n\nPayment successfully initiated. Will fulfill " +
                        "after microdeposit verification\n\n"
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
    }
}
