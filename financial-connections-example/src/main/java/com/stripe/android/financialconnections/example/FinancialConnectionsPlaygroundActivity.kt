package com.stripe.android.financialconnections.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.financialconnections.example.settings.PlaygroundSettings
import com.stripe.android.financialconnections.example.settings.SettingsUi
import com.stripe.android.financialconnections.rememberFinancialConnectionsSheet
import com.stripe.android.financialconnections.rememberFinancialConnectionsSheetForToken
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher

@OptIn(ExperimentalComposeUiApi::class)
class FinancialConnectionsPlaygroundActivity : AppCompatActivity() {

    private val viewModel by viewModels<FinancialConnectionsPlaygroundViewModel>()

    private lateinit var collectBankAccountLauncher: CollectBankAccountLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectBankAccountLauncher = CollectBankAccountLauncher.create(
            this,
            viewModel::onCollectBankAccountLauncherResult
        )
        setContent {
            FinancialConnectionsScreen()
        }
    }

    @Composable
    private fun FinancialConnectionsScreen() {
        val state: FinancialConnectionsPlaygroundState by viewModel.state.collectAsState()
        val viewEffect: FinancialConnectionsPlaygroundViewEffect? by viewModel.viewEffect.collectAsState(
            null
        )
        val financialConnectionsSheetForData = rememberFinancialConnectionsSheet(
            viewModel::onFinancialConnectionsSheetResult
        )

        val financialConnectionsSheetForToken = rememberFinancialConnectionsSheetForToken(
            viewModel::onFinancialConnectionsSheetForTokenResult
        )

        LaunchedEffect(viewEffect) {
            viewEffect?.let {
                when (it) {
                    is FinancialConnectionsPlaygroundViewEffect.OpenForData -> {
                        financialConnectionsSheetForData.present(it.configuration)
                    }

                    is FinancialConnectionsPlaygroundViewEffect.OpenForToken -> {
                        financialConnectionsSheetForToken.present(it.configuration)
                    }

                    is FinancialConnectionsPlaygroundViewEffect.OpenForPaymentIntent -> {
                        collectBankAccountLauncher.presentWithPaymentIntent(
                            publishableKey = it.publishableKey,
                            stripeAccountId = null,
                            clientSecret = it.paymentIntentSecret,
                            configuration = CollectBankAccountConfiguration.USBankAccount(
                                name = "Sample name",
                                email = "sampleEmail@test.com"
                            )
                        )
                    }
                }
            }
        }

        FinancialConnectionsContent(
            state = state,
            onSettingsChanged = viewModel::onSettingsChanged,
            onButtonClick = viewModel::startFinancialConnectionsSession
        )
    }

    @Composable
    @Suppress("LongMethod")
    private fun FinancialConnectionsContent(
        state: FinancialConnectionsPlaygroundState,
        onSettingsChanged: (PlaygroundSettings) -> Unit,
        onButtonClick: (Flow, Pair<String, String>, String) -> Unit
    ) {
        val (selectedFlow, onFlowSelected) = remember { mutableStateOf(Flow.values()[0]) }
        val (publicKey, onPublicKeyChanged) = remember { mutableStateOf("") }
        val (secretKey, onSecretKeyChanged) = remember { mutableStateOf("") }
        val (email, onEmailChange) = remember { mutableStateOf("") }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Connections Playground") }) },
            content = {
                Column(
                    modifier = Modifier
                        .padding(it)
                        .padding(16.dp)
                ) {
                    // TODO custom keys
//                    if (selectedMode == Merchant.Other) {
//                        OutlinedTextField(
//                            value = publicKey,
//                            onValueChange = onPublicKeyChanged,
//                            placeholder = { Text("pk_...") },
//                            label = { Text("Public key") },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                        )
//                        OutlinedTextField(
//                            value = secretKey,
//                            onValueChange = onSecretKeyChanged,
//                            placeholder = { Text("sk_...") },
//                            label = { Text("Secret key") },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                        )
//                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowSection(selectedFlow, onFlowSelected)
                    EmailInputSection(email, onEmailChange)
                    SettingsUi(playgroundSettings = state.settings)
                    if (state.loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Divider(Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "backend: ${state.backendUrl}",
                        color = Color.Gray
                    )
                    Text(
                        text = "env: ${BuildConfig.TEST_ENVIRONMENT}",
                        color = Color.Gray
                    )
                    Button(
                        onClick = {
                            onButtonClick(
                                selectedFlow,
                                publicKey to secretKey,
                                email
                            )
                        },
                    ) {
                        Text("Connect Accounts!")
                    }
                    LazyColumn {
                        items(state.status) { item ->
                            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.Top) {
                                Canvas(
                                    modifier = Modifier
                                        .padding(end = 8.dp, top = 6.dp)
                                        .size(6.dp)
                                ) {
                                    drawCircle(Color.Black)
                                }
                                SelectionContainer {
                                    Text(text = item, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun EmailInputSection(
        email: String,
        onEmailChange: (String) -> Unit
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTagsAsResourceId = true }
                .testTag("email_input"),
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Customer email (optional)") }
        )
    }

    @Composable
    private fun FlowSection(
        selectedOption: Flow,
        onOptionSelected: (Flow) -> Unit
    ) {
        Text(
            text = "Flow",
            style = MaterialTheme.typography.h6.merge(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Flow.values().forEach { text ->
                RadioButton(
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("${text.name}_checkbox"),
                    selected = (text == selectedOption),
                    onClick = { onOptionSelected(text) }
                )
                Text(
                    text = text.name,
                    style = MaterialTheme.typography.body1.merge(),
                )
            }
        }
    }

    @Preview
    @Composable
    fun ContentPreview() {
        FinancialConnectionsContent(
            state = FinancialConnectionsPlaygroundState(
                backendUrl = "http://backend.url",
                loading = false,
                publishableKey = "pk",
                status = listOf("Result: Pending")
            ),
            onSettingsChanged = {},
            onButtonClick = { _, _, _ -> }
        )
    }
}
