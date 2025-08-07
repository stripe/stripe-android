package com.stripe.android.crypto.onramp.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.crypto.onramp.model.DateOfBirth
import com.stripe.android.crypto.onramp.model.IdType
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.paymentsheet.PaymentSheet

internal class OnrampActivity : ComponentActivity() {

    private lateinit var onrampPresenter: OnrampCoordinator.Presenter

    private val viewModel: OnrampViewModel by viewModels {
        OnrampViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        FeatureFlags.nativeLinkEnabled.setEnabled(true)

        val callbacks = OnrampCallbacks(
            authenticationCallback = viewModel::onAuthenticationResult,
        )

        onrampPresenter = viewModel.onrampCoordinator
            .createPresenter(this, callbacks)

        setContent {
            OnrampExampleTheme {
                OnrampScreen(
                    viewModel = viewModel,
                    onAuthenticateUser = { email ->
                        onrampPresenter.authenticateExistingLinkUser(email)
                    },
                    onStartVerification = {
                        onrampPresenter.promptForIdentityVerification()
                    }
                )
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
internal fun OnrampScreen(
    viewModel: OnrampViewModel,
    onAuthenticateUser: (String) -> Unit,
    onStartVerification: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Show toast messages
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Onramp Coordinator",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        when (val currentState = uiState) {
            is OnrampUiState.EmailInput -> {
                EmailInputScreen(
                    onCheckUser = { email ->
                        viewModel.checkIfLinkUser(email)
                    }
                )
            }
            is OnrampUiState.Loading -> {
                LoadingScreen()
            }
            is OnrampUiState.Registration -> {
                RegistrationScreen(
                    initialEmail = currentState.email,
                    onRegister = { email, phone, country, fullName ->
                        val userInfo = LinkUserInfo(
                            email = email.trim(),
                            phone = phone.trim(),
                            country = country.trim(),
                            fullName = fullName?.trim()?.takeIf { it.isNotEmpty() }
                        )
                        viewModel.registerNewLinkUser(userInfo)
                    },
                    onBack = {
                        viewModel.onBackToEmailInput()
                    }
                )
            }
            is OnrampUiState.Authentication -> {
                AuthenticationScreen(
                    email = currentState.email,
                    onAuthenticate = onAuthenticateUser,
                    onBack = {
                        viewModel.onBackToEmailInput()
                    }
                )
            }
            is OnrampUiState.PostAuthenticationScreen -> {
                var firstName by remember { mutableStateOf("") }
                var lastName by remember { mutableStateOf("") }

                KYCScreen(
                    firstName = firstName,
                    onFirstNameChange = { firstName = it },
                    lastName = lastName,
                    onLastNameChange = { lastName = it },
                    onCollectKYC = { kycInfo -> viewModel.collectKycInfo(kycInfo) }
                )

                StartVerificationScreen {
                    onStartVerification()
                }
            }
        }
    }
}

@Composable
private fun EmailInputScreen(
    onCheckUser: (String) -> Unit
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
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading...")
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
    onAuthenticate: (String) -> Unit,
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

        Button(
            onClick = { onAuthenticate(email) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text("Authenticate")
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Email Input")
        }
    }
}

@Composable
private fun KYCScreen(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
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

        Button(
            onClick = {
                onCollectKYC(
                    KycInfo(
                        firstName = firstName,
                        lastName = lastName,
                        idNumber = "123456789",
                        idType = IdType.SOCIAL_SECURITY_NUMBER,
                        dateOfBirth = DateOfBirth(1, month = 1, year = 1990),
                        address = PaymentSheet.Address(
                            city = "New York",
                            country = "US",
                            line1 = "1234 Fake Street",
                            postalCode = "10108",
                            state = "New York"
                        ),
                        nationalities = null,
                        birthCountry = "US",
                        birthCity = "Chicago"
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
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
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
fun OnrampExampleTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) darkColors() else lightColors(),
        content = content,
    )
}
