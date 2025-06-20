package com.stripe.android.paymentsheet.example.playground.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentIntent
import com.stripe.android.paymentsheet.LinkCoordinator
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RidesharingAppActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_PLAYGROUND_STATE = "playground_state"
        
        internal fun createIntent(context: Context, playgroundState: PlaygroundState.Payment): Intent {
            return Intent(context, RidesharingAppActivity::class.java).apply {
                putExtra(EXTRA_PLAYGROUND_STATE, playgroundState)
            }
        }
    }
    
    private lateinit var playgroundState: PlaygroundState.Payment
    private var linkCoordinator: LinkCoordinator? = null

    private val _paymentOption = MutableStateFlow<PaymentOption?>(null)
    private val paymentOption: StateFlow<PaymentOption?> = _paymentOption.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    private val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        playgroundState = intent.getParcelableExtra(EXTRA_PLAYGROUND_STATE)
            ?: throw IllegalStateException("PlaygroundState not provided")

        setupLinkCoordinator()

        setContent {
            RidesharingTheme {
                RidesharingAppContent()
            }
        }
        }

    private fun setupLinkCoordinator() {
        linkCoordinator = LinkCoordinator.Builder { paymentOption ->
            _paymentOption.value = paymentOption
        }.build(this)
        
        configureLinkCoordinator()
    }

    private fun handleLinkSuccess() {
        _isLoading.value = false
        setResult(RESULT_OK)
        finish()
    }

    private fun handleLinkFailure(error: Throwable?) {
        _isLoading.value = false
        // In a real app, you'd show an error message to the user
        error?.printStackTrace()
    }

    private fun configureLinkCoordinator() {
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

        val configuration = LinkCoordinator.Configuration(
            stripeIntent = mockPaymentIntent,
            merchantName = playgroundState.paymentSheetConfiguration().merchantDisplayName,
            customerEmail = "demo@example.com" // Mock email for demo
        )

        linkCoordinator?.configure(configuration) { success, error ->
            if (success) {
                _paymentOption.value = linkCoordinator?.getPaymentOption()
            } else {
                // Handle configuration error
                error?.printStackTrace()
            }
        }
    }
    
    @Composable
    private fun RidesharingAppContent() {
        val currentPaymentOption by paymentOption.collectAsState()
        val loading by isLoading.collectAsState()
        var selectedRideType by remember { mutableStateOf("Economy") }

        Surface(color = MaterialTheme.colors.background) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(id = R.drawable.map),
                        contentDescription = "Map",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.surface)
                            .padding(16.dp)
                    ) {
                        Text(
                            "Choose a ride - Link Demo",
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        RideTypeSelector(selectedRideType) { selectedRideType = it }
                        Spacer(modifier = Modifier.height(16.dp))
                        PaymentMethodSelector(
                            isEnabled = !loading,
                            paymentMethodLabel = currentPaymentOption?.label ?: "Select a payment method",
                            paymentMethodPainter = currentPaymentOption?.iconPainter,
                            onClick = { 
                                // This will eventually launch the Link payment flow
                                linkCoordinator?.present()
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                _isLoading.value = true
                                // For demo purposes, simulate a successful Link confirmation
                                // In real implementation, linkCoordinator?.confirm() would handle this
                                linkCoordinator?.confirm()
                                
                                // Simulate success after a short delay for demo
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    handleLinkSuccess()
                                }, 1000)
                            },
                            enabled = currentPaymentOption != null && !loading,
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
                                Text("Confirm Ride with Link")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RideTypeSelector(
        selectedType: String,
        onTypeSelected: (String) -> Unit
    ) {
        val rideTypes = listOf(
            RideType("Economy", "1-4", "$10.50", R.drawable.car_economy),
            RideType("Comfort", "1-4", "$15.00", R.drawable.car_comfort),
            RideType("XL", "1-6", "$20.25", R.drawable.car_xl)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            rideTypes.forEach { rideType ->
                RideTypeView(
                    rideType = rideType,
                    isSelected = selectedType == rideType.name,
                    onSelected = { onTypeSelected(rideType.name) }
                )
            }
        }
    }

    @Composable
    fun RideTypeView(
        rideType: RideType,
        isSelected: Boolean,
        onSelected: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .selectable(
                    selected = isSelected,
                    onClick = onSelected
                )
                .padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = rideType.iconRes),
                contentDescription = rideType.name,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        if (isSelected) MaterialTheme.colors.secondary.copy(alpha = 0.2f)
                        else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(rideType.name, style = MaterialTheme.typography.body2)
            Text(
                rideType.price,
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold)
            )
        }
    }

    data class RideType(
        val name: String,
        val capacity: String,
        val price: String,
        val iconRes: Int
    )

    @Composable
    fun RidesharingTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colors = lightColors(
                primary = Color.Black,
                secondary = Color(0xFF007AFF),
                background = Color(0xFFF5F5F5),
                surface = Color.White
            ),
            content = content
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        RidesharingTheme {
            RidesharingAppContent()
        }
    }
} 