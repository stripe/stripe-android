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
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
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
    private lateinit var flowController: PaymentSheet.FlowController

    private val _paymentOption = MutableStateFlow<PaymentOption?>(null)
    private val paymentOption: StateFlow<PaymentOption?> = _paymentOption.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    private val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        playgroundState = intent.getParcelableExtra(EXTRA_PLAYGROUND_STATE)
            ?: throw IllegalStateException("PlaygroundState not provided")

        flowController = PaymentSheet.FlowController.create(
            this,
            paymentResultCallback = { result ->
                _isLoading.value = false
                if (result is PaymentSheetResult.Completed) {
                    setResult(RESULT_OK)
                    finish()
                }
            },
            paymentOptionCallback = { option ->
                _paymentOption.value = option
            }
        )

        configureFlowController()

        setContent {
            RidesharingTheme {
                RidesharingAppContent()
            }
        }
    }
    
    private fun configureFlowController() {
        val callback: (Boolean, Throwable?) -> Unit = { success, _ ->
            if (success) {
                _paymentOption.value = flowController.getPaymentOption()
            }
        }

        if (playgroundState.checkoutMode == CheckoutMode.SETUP) {
            flowController.configureWithSetupIntent(
                setupIntentClientSecret = playgroundState.clientSecret,
                configuration = playgroundState.paymentSheetConfiguration(),
                callback = callback
            )
        } else {
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = playgroundState.clientSecret,
                configuration = playgroundState.paymentSheetConfiguration(),
                callback = callback
            )
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
                            "Choose a ride",
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        RideTypeSelector(selectedRideType) { selectedRideType = it }
                        Spacer(modifier = Modifier.height(16.dp))
                        PaymentMethodSelector(
                            isEnabled = !loading,
                            paymentMethodLabel = currentPaymentOption?.label ?: "Select a payment method",
                            paymentMethodPainter = currentPaymentOption?.iconPainter,
                            onClick = { flowController.presentPaymentOptions() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                _isLoading.value = true
                                flowController.confirm()
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
                                Text("Confirm Ride")
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