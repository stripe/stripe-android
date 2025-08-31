package com.stripe.android.crypto.onramp.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.crypto.onramp.example.network.OnrampSessionResponse
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.DateOfBirth
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.PaymentMethodDisplayData
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.launch

internal class OnrampActivity : ComponentActivity() {

    private lateinit var onrampPresenter: OnrampCoordinator.Presenter

    private val viewModel: OnrampViewModel by viewModels {
        OnrampViewModel.Factory()
    }

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        FeatureFlags.nativeLinkEnabled.setEnabled(true)

        val callbacks = OnrampCallbacks(
            authenticateUserCallback = viewModel::onAuthenticateUserResult,
            verifyIdentityCallback = viewModel::onVerifyIdentityResult,
            checkoutCallback = viewModel::onCheckoutResult,
            collectPaymentCallback = viewModel::onCollectPaymentResult,
            authorizeCallback = viewModel::onAuthorizeResult
        )

        onrampPresenter = viewModel.onrampCoordinator
            .createPresenter(this, callbacks)

        // ViewModel notifies UI to launch checkout flow.
        // Note checkout requires an Activity context since it might launch UI to handle next actions (e.g. 3DS2).
        lifecycleScope.launch {
            viewModel.checkoutEvent.collect { event ->
                event?.let {
                    onrampPresenter.performCheckout(
                        onrampSessionId = event.sessionId,
                        checkoutHandler = { viewModel.checkoutWithBackend(event.sessionId) }
                    )
                    viewModel.clearCheckoutEvent()
                }
            }
        }

        setContent {
            OnrampExampleTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            windowInsets = AppBarDefaults.topAppBarWindowInsets,
                            title = { Text("Onramp Coordinator") },
                        )
                    },
                ) { innerPadding ->
                    OnrampScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel,
                        onAuthenticateUser = { oauthScopes ->
                            if (oauthScopes.isNullOrBlank()) {
                                onrampPresenter.authenticateUser()
                            } else {
                                // Not the cleanest approach, but good enough for an example.
                                lifecycleScope.launch {
                                    val linkAuthIntentId = viewModel.createLinkAuthIntent(oauthScopes)
                                        ?: return@launch
                                    viewModel.onAuthorize(linkAuthIntentId)
                                    onrampPresenter.authorize(linkAuthIntentId)
                                }
                            }
                        },
                        onAuthorize = { linkAuthIntentId ->
                            viewModel.onAuthorize(linkAuthIntentId)
                            onrampPresenter.authorize(linkAuthIntentId)
                        },
                        onRegisterWalletAddress = { address, network ->
                            viewModel.registerWalletAddress(address, network)
                        },
                        onStartVerification = {
                            onrampPresenter.verifyIdentity()
                        },
                        onCollectPayment = { type ->
                            onrampPresenter.collectPaymentMethod(type)
                        },
                        onCreatePaymentToken = {
                            viewModel.createCryptoPaymentToken()
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
internal fun OnrampScreen(
    viewModel: OnrampViewModel,
    modifier: Modifier = Modifier,
    onAuthenticateUser: (oauthScopes: String?) -> Unit,
    onAuthorize: (linkAuthIntentId: String) -> Unit,
    onRegisterWalletAddress: (String, CryptoNetwork) -> Unit,
    onStartVerification: () -> Unit,
    onCollectPayment: (type: PaymentMethodType) -> Unit,
    onCreatePaymentToken: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = uiState.screen != Screen.EmailInput) {
        viewModel.onBackToEmailInput()
    }

    // Show toast messages
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            Log.d("OnrampExample", it)
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (uiState.screen) {
            Screen.EmailInput -> {
                EmailInputScreen(
                    onCheckUser = { email ->
                        viewModel.checkIfLinkUser(email)
                    },
                    onAuthorize = onAuthorize
                )
            }
            Screen.Loading -> {
                LoadingScreen(message = uiState.loadingMessage ?: "Loading...")
            }
            Screen.Registration -> {
                RegistrationScreen(
                    initialEmail = uiState.email,
                    onRegister = { email, phone, country, fullName ->
                        val userInfo = LinkUserInfo(
                            email = email.trim(),
                            fullName = fullName?.trim()?.takeIf { it.isNotEmpty() },
                            phone = phone.trim(),
                            country = country.trim(),
                        )
                        viewModel.registerNewLinkUser(userInfo)
                    },
                    onBack = {
                        viewModel.onBackToEmailInput()
                    }
                )
            }
            Screen.Authentication -> {
                AuthenticationScreen(
                    email = uiState.email,
                    onAuthenticate = onAuthenticateUser,
                    onUpdatePhoneNumber = { phoneNumber ->
                        viewModel.updatePhoneNumber(phoneNumber)
                    },
                    onBack = {
                        viewModel.onBackToEmailInput()
                    }
                )
            }
            Screen.AuthenticatedOperations -> {
                AuthenticatedOperationsScreen(
                    email = uiState.email,
                    customerId = uiState.customerId ?: "",
                    consentedLinkAuthIntentIds = uiState.consentedLinkAuthIntentIds,
                    onrampSessionResponse = uiState.onrampSession,
                    selectedPaymentData = uiState.selectedPaymentData,
                    onAuthenticate = onAuthenticateUser,
                    onRegisterWalletAddress = onRegisterWalletAddress,
                    onCollectKYC = { kycInfo -> viewModel.collectKycInfo(kycInfo) },
                    onStartVerification = onStartVerification,
                    onCollectPayment = onCollectPayment,
                    onCreatePaymentToken = onCreatePaymentToken,
                    onCreateSession = { viewModel.createSession() },
                    onPerformCheckout = { viewModel.performCheckout() },
                    onLogOut = { viewModel.logOut() },
                    onBack = {
                        viewModel.onBackToEmailInput()
                    }
                )
            }
        }
    }
}

@Composable
private fun EmailInputScreen(
    onCheckUser: (String) -> Unit,
    onAuthorize: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = { onCheckUser(email) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check if Link User Exists")
        }

        Spacer(modifier = Modifier.height(24.dp))

        AuthorizeSection(onAuthorize = onAuthorize)
    }
}

@Composable
private fun LoadingScreen(message: String = "Loading...") {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(message)
        }
    }
}

@Composable
private fun RegistrationScreen(
    initialEmail: String,
    onRegister: (String, String, String, String?) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    Column {
        Text(
            text = "Register New Link User",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        RegistrationFields(
            email = email,
            onEmailChange = { email = it },
            phone = phone,
            onPhoneChange = { phone = it },
            country = country,
            onCountryChange = { country = it },
            fullName = fullName,
            onFullNameChange = { fullName = it }
        )

        RegistrationButtons(
            onBack = onBack,
            onRegister = { onRegister(email, phone, country, fullName) }
        )
    }
}

@Composable
private fun RegistrationFields(
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    country: String,
    onCountryChange: (String) -> Unit,
    fullName: String,
    onFullNameChange: (String) -> Unit
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email Address *") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Phone Number (E.164 format) *") },
        placeholder = { Text("+1234567890") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    OutlinedTextField(
        value = country,
        onValueChange = onCountryChange,
        label = { Text("Country Code *") },
        placeholder = { Text("US") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    OutlinedTextField(
        value = fullName,
        onValueChange = onFullNameChange,
        label = { Text("Full Name (optional)") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    )
}

@Composable
private fun RegistrationButtons(
    onBack: () -> Unit,
    onRegister: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.weight(1f)
        ) {
            Text("Back")
        }

        Button(
            onClick = onRegister,
            modifier = Modifier.weight(1f)
        ) {
            Text("Register")
        }
    }
}

@Composable
private fun AuthenticationScreen(
    email: String,
    onAuthenticate: (oauthScopes: String?) -> Unit,
    onUpdatePhoneNumber: (String) -> Unit,
    onBack: () -> Unit
) {
    Column {
        Text(
            text = "Authenticate Link User",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { /* Read-only for now */ },
            label = { Text("Email Address") },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        AuthenticateSection(onAuthenticate = onAuthenticate)

        Spacer(modifier = Modifier.height(24.dp))

        UpdatePhoneNumberSection(onUpdatePhoneNumber = onUpdatePhoneNumber)

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Email Input")
        }
    }
}

@Composable
fun AuthenticateSection(
    onAuthenticate: (oauthScopes: String?) -> Unit,
) {
    var oauthScopes by remember { mutableStateOf("kyc.status:read,crypto:ramp") }
    OutlinedTextField(
        value = oauthScopes,
        onValueChange = { oauthScopes = it },
        label = { Text("Request OAuth scopes (optional)") },
        placeholder = { Text("userinfo:read,kyc:share") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )

    Button(
        onClick = { onAuthenticate(oauthScopes) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text("Authenticate")
    }
}

@Composable
@Suppress("LongMethod")
private fun AuthenticatedOperationsScreen(
    email: String,
    customerId: String,
    consentedLinkAuthIntentIds: List<String>,
    onrampSessionResponse: OnrampSessionResponse?,
    selectedPaymentData: PaymentMethodDisplayData?,
    onAuthenticate: (oauthScopes: String?) -> Unit,
    onRegisterWalletAddress: (String, CryptoNetwork) -> Unit,
    onCollectKYC: (KycInfo) -> Unit,
    onStartVerification: () -> Unit,
    onCollectPayment: (type: PaymentMethodType) -> Unit,
    onCreatePaymentToken: () -> Unit,
    onCreateSession: () -> Unit,
    onPerformCheckout: () -> Unit,
    onLogOut: () -> Unit,
    onBack: () -> Unit
) {
    // hardcoded sample ETH wallet
    var walletAddressInput by remember { mutableStateOf("0x742d35Cc6634C0532925a3b844Bc454e4438f44e") }
    var selectedNetwork by remember { mutableStateOf(CryptoNetwork.Ethereum) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Authenticated Operations",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Email: $email",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Customer ID: $customerId",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Consented LAIs:\n${consentedLinkAuthIntentIds.joinToString("\n")}",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        onrampSessionResponse?.let { response ->
            Text(
                text = "Onramp Session ID: ${response.id}",
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Text(
                text = "Session Status: ${response.status}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Total Amount: ${response.sourceTotalAmount}",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Payment Method: ${response.paymentMethod}",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            response.transactionDetails.let { details ->
                Text(
                    text = buildString {
                        append("Exchange Amount: ${details.sourceExchangeAmount}")
                        append(" ${details.sourceCurrency} → ${details.destinationExchangeAmount}")
                        append(" ${details.destinationCurrency}")
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Network Fee: ${details.fees.networkFeeAmount}",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Transaction Fee: ${details.fees.transactionFeeAmount}",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        } ?: run {
            Spacer(modifier = Modifier.height(16.dp))
        }

        selectedPaymentData?.let {
            Image(
                painter = painterResource(selectedPaymentData.iconRes),
                contentDescription = selectedPaymentData.label,
                modifier = Modifier
                    .height(24.dp)
                    .padding(end = 8.dp)
            )

            Text(
                text = "Selected Payment Type: ${selectedPaymentData.label}",
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "Selected Payment Value: ${selectedPaymentData.sublabel}",
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        Text(
            text = "Request scopes",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AuthenticateSection(
            onAuthenticate = onAuthenticate
        )

        Text(
            text = "Register Wallet Address",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Network Selection
        Box {
            OutlinedTextField(
                value = selectedNetwork.value.replaceFirstChar { it.uppercase() },
                onValueChange = { },
                label = { Text("Network") },
                readOnly = true,
                trailingIcon = {
                    TextButton(onClick = { isDropdownExpanded = true }) {
                        Text("▼")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                CryptoNetwork.entries.forEach { network ->
                    DropdownMenuItem(
                        onClick = {
                            selectedNetwork = network
                            isDropdownExpanded = false
                        }
                    ) {
                        Text(network.value.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }

        OutlinedTextField(
            value = walletAddressInput,
            onValueChange = { walletAddressInput = it },
            label = { Text("Wallet Address") },
            placeholder = { Text("0x1234567890abcdef...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = { onRegisterWalletAddress(walletAddressInput, selectedNetwork) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text("Register Wallet Address")
        }

        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var ssn by remember { mutableStateOf("000000000") }

        KYCScreen(
            firstName = firstName,
            onFirstNameChange = { firstName = it },
            lastName = lastName,
            onLastNameChange = { lastName = it },
            ssn = ssn,
            onSsnChange = { ssn = it },
            onCollectKYC = { kycInfo -> onCollectKYC(kycInfo) }
        )

        StartVerificationScreen {
            onStartVerification()
        }

        Text(
            text = "Payment",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { onCollectPayment(PaymentMethodType.Card) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Collect Card")
        }

        Button(
            onClick = { onCollectPayment(PaymentMethodType.BankAccount) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Collect Bank Account")
        }

        Button(
            onClick = onCreatePaymentToken,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Create Crypto Payment Token")
        }

        Button(
            onClick = onCreateSession,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("📋 Create Session")
        }

        Button(
            onClick = onPerformCheckout,
            enabled = onrampSessionResponse != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                if (onrampSessionResponse != null) {
                    "🚀 Checkout"
                } else {
                    "🚀 Checkout (Create session first)"
                }
            )
        }

        Button(
            onClick = onLogOut,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Log Out")
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Email Input")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun KYCScreen(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    ssn: String,
    onSsnChange: (String) -> Unit,
    onCollectKYC: (KycInfo) -> Unit
) {
    Column {
        Text(
            text = "Collect KYC Info",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        KYCTextField(firstName, "First Name", onFirstNameChange)
        KYCTextField(lastName, "Last Name", onLastNameChange)
        KYCTextField(ssn, "SSN", onSsnChange)

        Button(
            onClick = {
                onCollectKYC(
                    KycInfo(
                        firstName = firstName,
                        lastName = lastName,
                        idNumber = ssn,
                        dateOfBirth = DateOfBirth(1, month = 1, year = 1990),
                        address = PaymentSheet.Address(
                            city = "New York",
                            country = "US",
                            line1 = "1234 Fake Street",
                            postalCode = "10108",
                            state = "NY"
                        )
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text("Collect KYC Info")
        }
    }
}

@Composable
private fun KYCTextField(
    value: String,
    label: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Composable
private fun StartVerificationScreen(
    startVerification: () -> Unit
) {
    Column {
        Text(
            text = "Verification",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                startVerification()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text("Start Identity Verification")
        }
    }
}

@Composable
private fun AuthorizeSection(
    onAuthorize: (String) -> Unit
) {
    var linkAuthIntentId by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = linkAuthIntentId,
            onValueChange = { linkAuthIntentId = it },
            label = { Text("LinkAuthIntent ID") },
            placeholder = { Text("lai_...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = { onAuthorize(linkAuthIntentId) },
            enabled = linkAuthIntentId.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Authorize")
        }
    }
}

@Composable
private fun UpdatePhoneNumberSection(
    onUpdatePhoneNumber: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    Column {
        Text(
            text = "Update Phone Number",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number (E.164 format)") },
            placeholder = { Text("+1234567890") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = { onUpdatePhoneNumber(phoneNumber) },
            enabled = phoneNumber.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Phone Number")
        }
    }
}

@Composable
fun OnrampExampleTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) darkColors() else lightColors(),
        content = content,
    )
}
