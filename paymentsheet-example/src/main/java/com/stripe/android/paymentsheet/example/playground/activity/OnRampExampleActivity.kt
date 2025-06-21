package com.stripe.android.paymentsheet.example.playground.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.onramp.OnRampCoordinator
import com.stripe.onramp.model.OnRampConfiguration
import com.stripe.onramp.model.PrefillDetails
import com.stripe.onramp.result.OnRampKycResult
import com.stripe.onramp.result.OnRampVerificationResult

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
    private var onRampVerificationResult by mutableStateOf<OnRampVerificationResult?>(null)
    private var onRampKycResult by mutableStateOf<OnRampKycResult?>(null)

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
        val onRampCallbacks = OnRampCoordinator.OnRampCallbacks.Builder()
            .verificationResultCallback { result -> onRampVerificationResult = result }
            .kycResultCallback { result -> onRampKycResult = result }
            .build()

        onRampCoordinator = OnRampCoordinator.Builder(onRampCallbacks).build(this)

        configureOnRampCoordinator()
    }

    private fun configureOnRampCoordinator() {
        val onRampConfiguration = OnRampConfiguration(
            publishableKey = "pk_1234",
            appearance = PaymentSheet.Appearance(),
        )

        onRampCoordinator?.configure(onRampConfiguration) { success, error ->
            if (!success) {
                // Handle configuration error
                error?.printStackTrace()
            }
        }
    }

    @Composable
    private fun OnRampExampleContent() {
        var email by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }
        var country by remember { mutableStateOf("") }
        var fullName by remember { mutableStateOf("") }

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
                            "OnRamp Link Authentication",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Show verification result if available
                        onRampVerificationResult?.let { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = when (result) {
                                    is OnRampVerificationResult.Completed -> Color.Green.copy(alpha = 0.1f)
                                    is OnRampVerificationResult.Canceled -> Color.Red.copy(alpha = 0.1f)
                                    is OnRampVerificationResult.Failed -> Color.Red.copy(alpha = 0.2f)
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (result) {
                                                is OnRampVerificationResult.Completed -> "✅ Verification Completed"
                                                is OnRampVerificationResult.Canceled -> "❌ Verification Canceled"
                                                is OnRampVerificationResult.Failed -> "⚠️ Verification Failed"
                                            },
                                            style = MaterialTheme.typography.h6,
                                            fontWeight = FontWeight.Bold,
                                            color = when (result) {
                                                is OnRampVerificationResult.Completed -> Color.Green.copy(alpha = 0.8f)
                                                is OnRampVerificationResult.Canceled -> Color.Red.copy(alpha = 0.8f)
                                                is OnRampVerificationResult.Failed -> Color.Red.copy(alpha = 0.9f)
                                            }
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                onRampVerificationResult = null
                                            }
                                        ) {
                                            Text("Clear Result")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    when (result) {
                                        is OnRampVerificationResult.Completed -> {
                                            Text("Customer ID: ${result.customerId}")
                                            Text("Status: Ready for OnRamp operations")
                                        }
                                        is OnRampVerificationResult.Canceled -> {
                                            Text("User canceled the verification process")
                                        }
                                        is OnRampVerificationResult.Failed -> {
                                            Text("Error: ${result.error.message ?: "Unknown error"}")
                                            Text("Please try again or contact support")
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Only show authentication form if no verification result
                        if (onRampVerificationResult == null) {
                            // Email input
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                placeholder = { Text("Enter email address") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Prefill details section
                            Text(
                                "Prefill Details (Optional)",
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                placeholder = { Text("Enter full name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                placeholder = { Text("Enter phone number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = country,
                                onValueChange = { country = it },
                                label = { Text("Country") },
                                placeholder = { Text("Enter country") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (email.isNotBlank()) {
                                        val prefillDetails = if (fullName.isNotBlank() || phoneNumber.isNotBlank() || country.isNotBlank()) {
                                            PrefillDetails(
                                                fullName = fullName.takeIf { it.isNotBlank() },
                                                phoneNumber = phoneNumber.takeIf { it.isNotBlank() },
                                                country = country.takeIf { it.isNotBlank() }
                                            )
                                        } else {
                                            null
                                        }
                                        
                                        onRampCoordinator?.promptForLinkAuthentication(
                                            emailAddress = email.trim(),
                                            prefillDetails = prefillDetails
                                        )
                                    }
                                },
                                enabled = email.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Authenticate with Link")
                            }
                        } else {
                            // Show options based on verification result
                            when (val result = onRampVerificationResult) {
                                is OnRampVerificationResult.Completed -> {
                                    Text(
                                        "🎉 Verification successful! Customer ${result.customerId} is ready for OnRamp operations.",
                                        style = MaterialTheme.typography.body1,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                                is OnRampVerificationResult.Canceled -> {
                                    Text(
                                        "Verification was canceled. You can try again by clearing the result and entering an email address.",
                                        style = MaterialTheme.typography.body1,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                                is OnRampVerificationResult.Failed -> {
                                    Text(
                                        "⚠️ Verification failed: ${result.error.message ?: "Unknown error"}. Please try again by clearing the result.",
                                        style = MaterialTheme.typography.body1,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                                null -> {
                                    // This shouldn't happen since we check for null above, but keeping for safety
                                }
                            }
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