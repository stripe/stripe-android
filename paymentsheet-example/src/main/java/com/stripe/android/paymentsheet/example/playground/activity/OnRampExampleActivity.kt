package com.stripe.android.paymentsheet.example.playground.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.onramp.OnRampCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OnRampExampleActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_PLAYGROUND_STATE = "playground_state"

        internal fun createIntent(context: Context, playgroundState: PlaygroundState.Payment): Intent {
            return Intent(context, OnRampExampleActivity::class.java).apply {
                putExtra(EXTRA_PLAYGROUND_STATE, playgroundState)
            }
        }
    }

    private lateinit var playgroundState: PlaygroundState.Payment
    private var onRampCoordinator: OnRampCoordinator? = null

    private val _paymentOption = MutableStateFlow<PaymentOption?>(null)
    private val paymentOption: StateFlow<PaymentOption?> = _paymentOption.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    private val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasAccountResult = MutableStateFlow<String?>(null)
    private val hasAccountResult: StateFlow<String?> = _hasAccountResult.asStateFlow()

    private val _authenticatedUser = MutableStateFlow<OnRampCoordinator.User?>(null)
    private val authenticatedUser: StateFlow<OnRampCoordinator.User?> = _authenticatedUser.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playgroundState = intent.getParcelableExtra(EXTRA_PLAYGROUND_STATE)
            ?: throw IllegalStateException("PlaygroundState not provided")

        setupOnRampCoordinator()

        setContent {
            OnRampTheme {
                OnRampExampleContent()
            }
        }
    }

    private fun setupOnRampCoordinator() {
        onRampCoordinator = OnRampCoordinator.Builder(
            paymentOptionCallback = { paymentOption ->
                _paymentOption.value = paymentOption
            },
            userAuthenticatedCallback = { user ->
                _authenticatedUser.value = user
            }
        ).build(this)

        configureOnRampCoordinator()
    }

    private fun configureOnRampCoordinator() {
        // Create a mock PaymentIntent for demonstration purposes
        val mockPaymentIntent = PaymentIntent(
            id = "pi_mock_demo",
            paymentMethodTypes = listOf("card", "link"),
            amount = 1000L, // $10.00
            clientSecret = playgroundState.clientSecret,
            countryCode = "US",
            created = System.currentTimeMillis(),
            currency = "usd",
            isLiveMode = false,
            unactivatedPaymentMethods = emptyList()
        )

        val configuration = OnRampCoordinator.Configuration(
            stripeIntent = mockPaymentIntent,
            appearance = PaymentSheet.Appearance(),
            merchantName = playgroundState.paymentSheetConfiguration().merchantDisplayName,
        )

        onRampCoordinator?.configure(configuration) { success, error ->
            if (!success) {
                // Handle configuration error
                error?.printStackTrace()
                _hasAccountResult.value = "Configuration error: ${error?.message}"
            }
        }
    }

    private suspend fun checkAccount(email: String) {
        _isLoading.value = true
        _hasAccountResult.value = null

        onRampCoordinator?.let { coordinator ->
            val result = coordinator.hasAccount(email)
            result.fold(
                onSuccess = { hasAccount ->
                    _hasAccountResult.value = if (hasAccount) {
                        "✅ Account found for $email"
                    } else {
                        "❌ No account found for $email"
                    }
                },
                onFailure = { error ->
                    _hasAccountResult.value = "Error checking account: ${error.message}"
                }
            )
        } ?: run {
            _hasAccountResult.value = "OnRamp coordinator not initialized"
        }

        _isLoading.value = false
    }

    @Composable
    private fun OnRampExampleContent() {
        val loading by isLoading.collectAsState()
        val accountResult by hasAccountResult.collectAsState()
        val user by authenticatedUser.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        var email by remember { mutableStateOf("") }

        Surface(color = MaterialTheme.colors.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "OnRamp Account Checker",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Show authenticated user info if available
                        user?.let { authenticatedUser ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color.Green.copy(alpha = 0.1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        "✅ Authenticated User",
                                        style = MaterialTheme.typography.h6,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Green.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Email: ${authenticatedUser.email}")
                                    Text("Phone: ${authenticatedUser.phone ?: "Not provided"}")
                                    Text("Verified: ${if (authenticatedUser.isVerified) "Yes" else "No"}")
                                    Text("Signup Complete: ${if (authenticatedUser.completedSignup) "Yes" else "No"}")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("Enter email address") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (email.isNotBlank()) {
                                    coroutineScope.launch {
                                        checkAccount(email.trim())
                                    }
                                }
                            },
                            enabled = email.isNotBlank() && !loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Check Account")
                            }
                        }

                        accountResult?.let { result ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = if (result.startsWith("✅")) {
                                    Color.Green.copy(alpha = 0.1f)
                                } else if (result.startsWith("❌")) {
                                    Color(0xFFFF9500).copy(alpha = 0.1f)
                                } else {
                                    Color.Red.copy(alpha = 0.1f)
                                }
                            ) {
                                Text(
                                    text = result,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.body1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onRampCoordinator?.promptForLinkAuthentication()
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Authenticate with Link")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun OnRampTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colors = lightColors(
                primary = Color(0xFF6772E5),
                secondary = Color(0xFF00D924),
                background = Color(0xFFF5F5F5),
                surface = Color.White
            ),
            content = content
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        OnRampTheme {
            OnRampExampleContent()
        }
    }
}