package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.stripe.android.ApiResultCallback
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.example.StripeFactory
import com.stripe.example.theme.DefaultExampleTheme
import kotlinx.coroutines.flow.MutableStateFlow

class ManualUSBankAccountPaymentMethodActivity : StripeIntentActivity() {
    private val stripe by lazy {
        StripeFactory(this).create()
    }

    private var paymentIntentSecret: String? = null
    private var setupIntentSecret: String? = null

    private val verifyCallback = object : ApiResultCallback<StripeIntent> {
        override fun onSuccess(result: StripeIntent) {
            viewModel.inProgress.value = false
            viewModel.status.value += "Attempted to verify with \n\n$result\n"
        }

        override fun onError(e: Exception) {
            error(
                "Can't verify ${if (paymentIntentSecret != null) "Payment" else "Setup"}Intent, $e"
            )
        }
    }

    private val screenState = MutableStateFlow<ScreenState>(ScreenState.CustomerCollectionScreen)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            screenState.collect {
                when (it) {
                    is ScreenState.CustomerCollectionScreen -> {
                        setContent { DefaultExampleTheme { CollectBankScreen() } }
                    }
                    is ScreenState.VerificationNeededScreen -> {
                        viewModel.inProgress.value = false
                        viewModel.status.value = ""
                        setContent { DefaultExampleTheme { VerificationNeededScreen() } }
                    }
                    is ScreenState.VerifyWithMicrodepositsScreen -> {
                        viewModel.inProgress.value = false
                        viewModel.status.value = ""
                        setContent { DefaultExampleTheme { VerifyWithMicrodepositScreen() } }
                    }
                    is ScreenState.Error -> {
                        viewModel.inProgress.value = false
                        viewModel.status.value += it.message
                    }
                }
            }
        }
    }

    override fun onConfirmSuccess() {
        super.onConfirmSuccess()

        // onConfirmSuccess gets called when we use PaymentLauncher to perform confirmation
        // In this case we want to go to the next screen state
        next()
    }

    @Composable
    private fun CollectBankScreen() {
        val inProgress by viewModel.inProgress.observeAsState(false)
        val status by viewModel.status.observeAsState("")
        val scrollState = rememberScrollState()
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
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    value = email.value,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    onValueChange = { email.value = it }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Account number") },
                    value = accountNumber.value,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    onValueChange = { accountNumber.value = it }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Routing number") },
                    value = routingNumber.value,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    onValueChange = { routingNumber.value = it }
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
                        onCheckedChange = { isSaveForFutureUsage.value = it }
                    )
                    Text(
                        text = "Save for future usage",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val paymentMethodCreateParams = PaymentMethodCreateParams.create(
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
                            createAndConfirmSetupIntent(
                                "us",
                                paymentMethodCreateParams
                            ) { secret ->
                                setupIntentSecret = secret
                            }
                        } else {
                            createAndConfirmPaymentIntent(
                                "us",
                                paymentMethodCreateParams
                            ) { secret ->
                                paymentIntentSecret = secret
                            }
                        }
                    }
                ) { Text(text = "Pay with US Bank Account") }
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
                    fontSize = 8.sp
                )
            }
        }
    }

    @Composable
    private fun VerificationNeededScreen() {
        val status by viewModel.status.observeAsState("")
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "${if (paymentIntentSecret != null) "Payment" else "Setup"}Intent " +
                    "awaiting verification.\n\nStripe will send a descriptor code microdeposit " +
                    "and may fall back to an amount microdeposit. These deposits take 1-2 " +
                    "business days to appear on the customer's online statement.\n\nIf you " +
                    "supplied a billing email, Stripe notifies your customer via this email when " +
                    "the deposits are expected to arrive.\n\nOnce the customer is ready to " +
                    "verify their account, you can manually collect the descriptor code or " +
                    "amounts to verify their account with this SDK."
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                content = { Text(text = "Continue to verification") },
                onClick = { next() }
            )
            Text(
                text = status,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
    }

    @Composable
    private fun VerifyWithMicrodepositScreen() {
        val inProgress by viewModel.inProgress.observeAsState(false)
        val status by viewModel.status.observeAsState("")
        val scrollState = rememberScrollState()
        val descriptorCode = remember { mutableStateOf("") }
        val firstAmount = remember { mutableStateOf("") }
        val secondAmount = remember { mutableStateOf("") }

        if (inProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .verticalScroll(scrollState)
        ) {
            if (status.isNotEmpty()) {
                Text(text = status)
            } else {
                Text(text = "Enter verification details from your bank account.")
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descriptor code") },
                    value = descriptorCode.value,
                    onValueChange = { descriptorCode.value = it }
                )
                Text(
                    text = "--- OR ---",
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("First amount") },
                    value = firstAmount.value,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    onValueChange = { firstAmount.value = it }
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Second amount") },
                    value = secondAmount.value,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    onValueChange = { secondAmount.value = it }
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    content = { Text(text = "Verify") },
                    onClick = {
                        verifyWithMicrodeposits(
                            descriptorCode = descriptorCode.value,
                            firstAmount = firstAmount.value.toIntOrNull() ?: 0,
                            secondAmount = secondAmount.value.toIntOrNull() ?: 0
                        )
                    }
                )
                Text(
                    text = status,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }
        }
    }

    private fun verifyWithMicrodeposits(
        descriptorCode: String,
        firstAmount: Int,
        secondAmount: Int
    ) {
        if (paymentIntentSecret != null) {
            verifyPaymentIntent(paymentIntentSecret!!, descriptorCode, firstAmount, secondAmount)
        } else {
            verifySetupIntent(setupIntentSecret!!, descriptorCode, firstAmount, secondAmount)
        }
    }

    private fun verifyPaymentIntent(
        clientSecret: String,
        descriptorCode: String,
        firstAmount: Int,
        secondAmount: Int
    ) {
        viewModel.inProgress.value = true
        viewModel.status.value += "Retreiving PaymentIntent...\n"
        stripe.retrievePaymentIntent(
            clientSecret,
            callback = object : ApiResultCallback<PaymentIntent> {
                override fun onSuccess(result: PaymentIntent) {
                    if (descriptorCode.isNotEmpty()) {
                        viewModel.status.value += "Verifying PaymentIntent with descriptorCode...\n"
                        stripe.verifyPaymentIntentWithMicrodeposits(
                            clientSecret = clientSecret,
                            descriptorCode = descriptorCode,
                            callback = verifyCallback
                        )
                    } else {
                        viewModel.status.value += "Verifying PaymentIntent with amounts...\n"
                        stripe.verifyPaymentIntentWithMicrodeposits(
                            clientSecret = clientSecret,
                            firstAmount = firstAmount,
                            secondAmount = secondAmount,
                            callback = verifyCallback
                        )
                    }
                }
                override fun onError(e: Exception) {
                    error("Could not retrieve PaymentIntent, $e\n")
                }
            }
        )
    }

    private fun verifySetupIntent(
        clientSecret: String,
        descriptorCode: String,
        firstAmount: Int,
        secondAmount: Int
    ) {
        viewModel.inProgress.value = true
        viewModel.status.value += "Retreiving SetupIntent...\n"
        stripe.retrieveSetupIntent(
            clientSecret,
            callback = object : ApiResultCallback<SetupIntent> {
                override fun onSuccess(result: SetupIntent) {
                    if (descriptorCode.isNotEmpty()) {
                        viewModel.status.value += "Verifying SetupIntent with descriptorCode...\n"
                        stripe.verifySetupIntentWithMicrodeposits(
                            clientSecret = clientSecret,
                            descriptorCode = descriptorCode,
                            callback = verifyCallback
                        )
                    } else {
                        viewModel.status.value += "Verifying SetupIntent with amounts...\n"
                        stripe.verifySetupIntentWithMicrodeposits(
                            clientSecret = clientSecret,
                            firstAmount = firstAmount,
                            secondAmount = secondAmount,
                            callback = verifyCallback
                        )
                    }
                }
                override fun onError(e: Exception) {
                    error("Could not retrieve SetupIntent, $e\n")
                }
            }
        )
    }

    private fun next() {
        val current = screenState.value
        screenState.tryEmit(
            when (current) {
                is ScreenState.CustomerCollectionScreen ->
                    ScreenState.VerificationNeededScreen
                is ScreenState.VerificationNeededScreen ->
                    ScreenState.VerifyWithMicrodepositsScreen
                ScreenState.VerifyWithMicrodepositsScreen ->
                    ScreenState.VerifyWithMicrodepositsScreen
                is ScreenState.Error -> ScreenState.Error(current.message)
            }
        )
    }

    private fun error(message: String) {
        screenState.tryEmit(
            ScreenState.Error(message)
        )
    }

    private sealed class ScreenState {
        object CustomerCollectionScreen : ScreenState()
        object VerificationNeededScreen : ScreenState()
        object VerifyWithMicrodepositsScreen : ScreenState()
        data class Error(val message: String) : ScreenState()
    }
}
