package com.stripe.android.paymentsheet.example.playground.embedded

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.reactnative.ReactNativeSdkInternal
import com.stripe.android.paymentelement.ApiConfigurationPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElementIntegration
import com.stripe.android.paymentelement.rememberEmbeddedPaymentElement
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.UseApiConfigurationSettingsDefinition
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.serialization.json.Json

@OptIn(ReactNativeSdkInternal::class)
internal class EmbeddedExampleActivity : AppCompatActivity() {
    private var mountedInstances by mutableStateOf(0)
    private val integrationViewModel: EmbeddedExampleIntegrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContent {
            val useApiConfiguration = remember {
                val snapshot = PlaygroundSettings.createFromSharedPreferences(applicationContext).snapshot()
                UseApiConfigurationSettingsDefinition.isEnabled(snapshot)
            }
            var checkoutVisible by rememberSaveable { mutableStateOf(false) }
            var useRestrictedIntegration by rememberSaveable { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues()),
            ) {
                Text(getString(R.string.embedded_example_title), modifier = Modifier.padding(16.dp))
                Button(
                    enabled = !checkoutVisible,
                    onClick = { useRestrictedIntegration = !useRestrictedIntegration },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(if (useRestrictedIntegration) "Restricted RN integration" else "Native Compose control")
                }
                Button(
                    onClick = {
                        if (checkoutVisible && useRestrictedIntegration) {
                            integrationViewModel.destroyIntegration()
                        }
                        if (!checkoutVisible) mountedInstances += 1
                        checkoutVisible = !checkoutVisible
                    },
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(if (checkoutVisible) "Hide checkout" else "Show checkout")
                }
                Text("Created instances: $mountedInstances", modifier = Modifier.padding(horizontal = 16.dp))
                if (checkoutVisible) {
                    Box(modifier = Modifier.weight(1f)) {
                        CheckoutScreen(
                            useApiConfiguration = useApiConfiguration,
                            integration = if (useRestrictedIntegration) {
                                integrationViewModel.getIntegration(this@EmbeddedExampleActivity)
                            } else {
                                null
                            },
                            activity = this@EmbeddedExampleActivity,
                        )
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val parcel = Parcel.obtain()
        try {
            outState.writeToParcel(parcel, 0)
            Log.i(
                "EmbeddedRetention",
                "mounted_instances=$mountedInstances saved_state_bytes=${parcel.dataSize()}",
            )
        } finally {
            parcel.recycle()
        }
    }
}

@Composable
@OptIn(ApiConfigurationPreview::class, ReactNativeSdkInternal::class)
private fun CheckoutScreen(
    useApiConfiguration: Boolean,
    integration: EmbeddedPaymentElementIntegration?,
    activity: ComponentActivity,
) {
    val context = LocalContext.current.applicationContext
    var prefetchedCheckout by remember { mutableStateOf<CheckoutResult?>(null) }
    val embeddedBuilder = remember {
        EmbeddedPaymentElement.Builder(
            createIntentCallback = { _, _ ->
                (prefetchedCheckout ?: checkout(context, initializePaymentConfiguration = true)).createIntentResult
            },
            resultCallback = { result -> handlePaymentResult(context, result) },
        )
    }

    val embeddedPaymentElement = if (integration == null) {
        rememberEmbeddedPaymentElement(embeddedBuilder)
    } else {
        remember(integration, activity, embeddedBuilder) {
            integration.createElement(activity, embeddedBuilder)
        }
    }

    LaunchedEffect(embeddedPaymentElement) {
        if (integration != null && embeddedPaymentElement.state != null) return@LaunchedEffect
        val checkoutResult = if (useApiConfiguration) {
            checkout(context, initializePaymentConfiguration = false).also {
                prefetchedCheckout = it
            }
        } else {
            null
        }
        val configurationBuilder = EmbeddedPaymentElement.Configuration.Builder("Powdur")
        checkoutResult?.apiConfiguration?.let(configurationBuilder::apiConfiguration)
        val configureResult = embeddedPaymentElement.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1099,
                    currency = "EUR",
                ),
                // Optional intent configuration options...
            ),
            configuration = configurationBuilder.build()
        )
        if (configureResult is EmbeddedPaymentElement.ConfigureResult.Failed) {
            Toast.makeText(context, configureResult.error.message, Toast.LENGTH_LONG).show()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                paddingValues = WindowInsets.systemBars.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                ).asPaddingValues()
            )
            .padding(16.dp)
    ) {
        embeddedPaymentElement.Content()
        Button(
            onClick = {
                embeddedPaymentElement.confirm()
            }
        ) {
            Text("Confirm payment")
        }
    }
}

@OptIn(ReactNativeSdkInternal::class)
internal class EmbeddedExampleIntegrationViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private var integration: EmbeddedPaymentElementIntegration? = null

    fun getIntegration(activity: ComponentActivity): EmbeddedPaymentElementIntegration {
        return integration ?: EmbeddedPaymentElementIntegration.create(
            activity = activity,
            savedStateHandle = savedStateHandle,
            integrationName = "embedded_example",
        ).also { integration = it }
    }

    fun destroyIntegration() {
        integration?.destroy()
        integration = null
    }

    override fun onCleared() {
        destroyIntegration()
    }
}

private suspend fun checkout(
    context: Context,
    initializePaymentConfiguration: Boolean,
): CheckoutResult {
    val request = ExampleCheckoutRequest(
        hotDogCount = 1,
        saladCount = 1,
        isSubscribing = false
    )
    val requestBody = Json.encodeToString(ExampleCheckoutRequest.serializer(), request)
    val apiResult = Fuel
        .post("https://stripe-mobile-payment-sheet.stripedemos.com/checkout")
        .jsonBody(requestBody)
        .suspendable()
        .awaitModel(ExampleCheckoutResponse.serializer())
    return when (apiResult) {
        is Result.Success -> {
            if (initializePaymentConfiguration) {
                PaymentConfiguration.init(
                    context = context,
                    publishableKey = apiResult.value.publishableKey,
                )
            }
            CheckoutResult(
                createIntentResult = CreateIntentResult.Success(apiResult.value.paymentIntent),
                apiConfiguration = ApiConfiguration(apiResult.value.publishableKey),
            )
        }
        is Result.Failure -> {
            CheckoutResult(
                createIntentResult = CreateIntentResult.Failure(apiResult.error),
                apiConfiguration = null,
            )
        }
    }
}

private data class CheckoutResult(
    val createIntentResult: CreateIntentResult,
    val apiConfiguration: ApiConfiguration?,
)

private fun handlePaymentResult(context: Context, result: EmbeddedPaymentElement.Result) {
    when (result) {
        is EmbeddedPaymentElement.Result.Completed -> {
            // Payment completed - show a confirmation screen.
            Toast.makeText(
                context,
                "Payment completed successfully!",
                Toast.LENGTH_LONG
            ).show()
        }
        is EmbeddedPaymentElement.Result.Failed -> {
            // Encountered an unrecoverable error. You can display the error to the user, log it, etc.
            Toast.makeText(
                context,
                "Payment failed: ${result.error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
        is EmbeddedPaymentElement.Result.Canceled -> {
            // Customer canceled - you should probably do nothing.
            Toast.makeText(
                context,
                "Payment canceled",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
