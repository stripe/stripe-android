package com.stripe.android.financialconnections.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.financialconnections.example.settings.PlaygroundSettings
import com.stripe.android.financialconnections.example.settings.SettingsUi
import com.stripe.android.financialconnections.rememberFinancialConnectionsSheet
import com.stripe.android.financialconnections.rememberFinancialConnectionsSheetForToken
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher

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
    private fun FinancialConnectionsContent(
        state: FinancialConnectionsPlaygroundState,
        onSettingsChanged: (PlaygroundSettings) -> Unit,
        onButtonClick: () -> Unit
    ) {
        val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

        if (showDialog) {
            EventsDialog(setShowDialog, state)
        }

        Scaffold(
            topBar = { PlaygroundTopBar(setShowDialog) },
            content = {
                PlaygroundContent(
                    padding = it,
                    state = state,
                    onSettingsChanged = onSettingsChanged,
                    onButtonClick = onButtonClick
                )
            }
        )
    }

    @Composable
    @Suppress("LongMethod")
    private fun PlaygroundContent(
        padding: PaddingValues,
        state: FinancialConnectionsPlaygroundState,
        onSettingsChanged: (PlaygroundSettings) -> Unit,
        onButtonClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            SettingsUi(
                playgroundSettings = state.settings,
                onSettingsChanged = onSettingsChanged
            )
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
                onClick = onButtonClick,
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

    @Composable
    private fun PlaygroundTopBar(
        setShowDialog: (Boolean) -> Unit,
    ) {
        val (showMenu, setShowMenu) = remember { mutableStateOf(false) }
        TopAppBar(
            title = { Text("Connections Playground") },
            actions = {
                IconButton(onClick = { setShowMenu(true) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { setShowMenu(false) }
                ) {
                    DropdownMenuItem(onClick = {
                        setShowMenu(false)
                        setShowDialog(true)
                    }) {
                        Text("See live events")
                    }
                }
            }
        )
    }

    @Composable
    private fun EventsDialog(
        setShowDialog: (Boolean) -> Unit,
        state: FinancialConnectionsPlaygroundState
    ) {
        AlertDialog(
            onDismissRequest = { setShowDialog(false) },
            title = { Text(text = "Emitted events") },
            text = {
                LazyColumn {
                    items(state.emittedEvents) { item ->
                        Text(text = "- $item")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { setShowDialog(false) }) {
                    Text("Close")
                }
            }
        )
    }

    @Preview
    @Composable
    fun ContentPreview() {
        FinancialConnectionsContent(
            state = FinancialConnectionsPlaygroundState(
                settings = PlaygroundSettings.createFromDefaults(),
                backendUrl = "http://backend.url",
                loading = false,
                publishableKey = "pk",
                status = listOf("Result: Pending")
            ),
            onButtonClick = {},
            onSettingsChanged = {}
        )
    }
}
