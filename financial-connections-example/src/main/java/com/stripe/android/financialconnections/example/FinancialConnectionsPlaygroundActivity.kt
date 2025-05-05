package com.stripe.android.financialconnections.example

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.PaymentConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.example.Experience.FinancialConnections
import com.stripe.android.financialconnections.example.Experience.InstantDebits
import com.stripe.android.financialconnections.example.Experience.LinkCardBrand
import com.stripe.android.financialconnections.example.FinancialConnectionsPlaygroundViewEffect.OpenForData
import com.stripe.android.financialconnections.example.FinancialConnectionsPlaygroundViewEffect.OpenForPaymentIntent
import com.stripe.android.financialconnections.example.FinancialConnectionsPlaygroundViewEffect.OpenForToken
import com.stripe.android.financialconnections.example.settings.EmailSetting
import com.stripe.android.financialconnections.example.settings.PlaygroundSettings
import com.stripe.android.financialconnections.example.settings.SettingsUi
import com.stripe.android.financialconnections.rememberFinancialConnectionsSheet
import com.stripe.android.financialconnections.rememberFinancialConnectionsSheetForToken
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountForInstantDebitsLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher.Companion.HOSTED_SURFACE_PAYMENT_ELEMENT
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet

class FinancialConnectionsPlaygroundActivity : AppCompatActivity() {

    private lateinit var collectBankAccountForAchLauncher: CollectBankAccountLauncher
    private lateinit var collectBankAccountForInstantDebitsLauncher: CollectBankAccountLauncher

    private val viewModel by viewModels<FinancialConnectionsPlaygroundViewModel> {
        FinancialConnectionsPlaygroundViewModel.Factory(
            applicationSupplier = { application },
            uriSupplier = { intent.data },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collectBankAccountForAchLauncher = CollectBankAccountLauncher.create(
            activity = this,
            callback = viewModel::onCollectBankAccountLauncherResult,
        )

        collectBankAccountForInstantDebitsLauncher = CollectBankAccountForInstantDebitsLauncher.createForPaymentSheet(
            // Pretending this is PaymentSheet for now…
            hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
            activityResultRegistryOwner = this,
            callback = viewModel::onCollectBankAccountForInstantDebitsLauncherResult,
            financialConnectionsAvailability = FinancialConnectionsAvailability.Full
        )

        setContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            FinancialConnectionsExampleTheme {
                FinancialConnectionsScreen()
            }
        }
    }

    @Composable
    private fun FinancialConnectionsScreen() {
        val state by viewModel.state.collectAsState()
        val viewEffect by viewModel.viewEffect.collectAsState(null)

        val paymentSheet = rememberPaymentSheet {
            viewModel.onPaymentSheetResult(it)
        }
        val financialConnectionsSheetForData = rememberFinancialConnectionsSheet(
            viewModel::onFinancialConnectionsSheetResult
        )

        val financialConnectionsSheetForToken = rememberFinancialConnectionsSheetForToken(
            viewModel::onFinancialConnectionsSheetForTokenResult
        )

        LaunchedEffect(viewEffect) {
            viewEffect?.let { effect ->
                when (effect) {
                    is OpenForData -> effect.launch(financialConnectionsSheetForData)
                    is OpenForToken -> effect.launch(financialConnectionsSheetForToken)
                    is OpenForPaymentIntent -> effect.launch(state, paymentSheet)
                }
            }
        }

        FinancialConnectionsContent(
            state = state,
            onSettingsChanged = viewModel::onSettingsChanged,
            onButtonClick = viewModel::connectAccounts
        )
    }

    private fun OpenForData.launch(
        financialConnectionsSheet: FinancialConnectionsSheet
    ) {
        financialConnectionsSheet.present(configuration)
    }

    private fun OpenForToken.launch(
        financialConnectionsSheet: FinancialConnectionsSheet
    ) {
        financialConnectionsSheet.present(configuration)
    }

    private fun OpenForPaymentIntent.launch(
        state: FinancialConnectionsPlaygroundState,
        paymentSheet: PaymentSheet
    ) {
        val email = state.settings.get<EmailSetting>().selectedOption

        when (integrationType) {
            IntegrationType.Standalone -> {
                launchStandaloneIntegration(email)
            }
            IntegrationType.PaymentElement -> {
                launchPaymentSheet(paymentSheet, email)
            }
        }
    }

    private fun OpenForPaymentIntent.launchStandaloneIntegration(email: String) {
        when (experience) {
            FinancialConnections -> collectBankAccountForAchLauncher.presentWithPaymentIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                clientSecret = paymentIntentSecret,
                configuration = CollectBankAccountConfiguration.USBankAccount(
                    name = "Sample name",
                    email = email,
                )
            )
            InstantDebits,
            LinkCardBrand -> collectBankAccountForInstantDebitsLauncher.presentWithPaymentIntent(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                clientSecret = paymentIntentSecret,
                configuration = CollectBankAccountConfiguration.InstantDebits(
                    email = email,
                    elementsSessionContext = elementsSessionContext,
                )
            )
        }
    }

    private fun OpenForPaymentIntent.launchPaymentSheet(
        paymentSheet: PaymentSheet,
        email: String,
    ) {
        PaymentConfiguration.init(
            context = this@FinancialConnectionsPlaygroundActivity,
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
        )

        val config = PaymentSheet.Configuration(
            allowsDelayedPaymentMethods = true,
            merchantDisplayName = "Example, Inc.",
            customer = PaymentSheet.CustomerConfiguration(
                id = requireNotNull(customerId),
                ephemeralKeySecret = requireNotNull(ephemeralKey),
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(
                email = email.takeIf { it.isNotBlank() },
            ),
        )

        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret = paymentIntentSecret,
            configuration = config,
        )
    }

    @Composable
    private fun FinancialConnectionsContent(
        state: FinancialConnectionsPlaygroundState,
        onSettingsChanged: (PlaygroundSettings) -> Unit,
        onButtonClick: () -> Unit
    ) {
        val (showEventsDialog, setShowEventsDialog) = remember { mutableStateOf(false) }

        if (showEventsDialog) {
            EventsDialog(setShowEventsDialog, state)
        }

        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                PlaygroundTopBar(
                    settings = state.settings,
                    setShowEventsDialog = setShowEventsDialog
                )
            },
            content = {
                PlaygroundContent(
                    state = state,
                    onSettingsChanged = onSettingsChanged,
                    onButtonClick = onButtonClick,
                    modifier = Modifier.padding(it),
                )
            }
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    @Suppress("LongMethod")
    private fun PlaygroundContent(
        state: FinancialConnectionsPlaygroundState,
        onSettingsChanged: (PlaygroundSettings) -> Unit,
        onButtonClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val focusManager = LocalFocusManager.current
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = modifier,
        ) {
            item {
                SettingsUi(
                    playgroundSettings = state.settings,
                    onSettingsChanged = onSettingsChanged
                )
            }

            if (state.loading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Divider(Modifier.padding(vertical = 8.dp))
            }

            item {
                Text(
                    text = "backend: ${state.backendUrl}",
                    color = MaterialTheme.colors.secondaryVariant
                )
            }

            item {
                Text(
                    text = "env: ${BuildConfig.TEST_ENVIRONMENT}",
                    color = MaterialTheme.colors.secondaryVariant
                )
            }

            item {
                Button(
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("connect_accounts"),
                    onClick = {
                        focusManager.clearFocus()
                        onButtonClick()
                    },
                ) {
                    Text("Connect Accounts")
                }
            }

            items(state.status) { item ->
                Row(Modifier.padding(4.dp), verticalAlignment = Alignment.Top) {
                    val primary = MaterialTheme.colors.primary
                    Canvas(
                        modifier = Modifier
                            .padding(end = 8.dp, top = 6.dp)
                            .size(6.dp)
                    ) {
                        drawCircle(primary)
                    }
                    SelectionContainer {
                        Text(text = item, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    @Composable
    private fun PlaygroundTopBar(
        settings: PlaygroundSettings,
        setShowEventsDialog: (Boolean) -> Unit,
    ) {
        val (showMenu, setShowMenu) = remember { mutableStateOf(false) }
        val context = LocalContext.current
        TopAppBar(
            windowInsets = WindowInsets.statusBars,
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
                        setShowEventsDialog(true)
                    }) {
                        Text("See live events")
                    }
                    DropdownMenuItem(onClick = {
                        setShowMenu(false)
                        context.startActivity(
                            FinancialConnectionsQrCodeActivity.create(
                                context = context,
                                settingsUri = settings.asDeeplinkUri().toString(),
                            )
                        )
                    }) {
                        Text("QR code")
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
