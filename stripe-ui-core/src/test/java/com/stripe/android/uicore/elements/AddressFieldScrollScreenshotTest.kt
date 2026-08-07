package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.FormScrollProvider
import com.stripe.android.uicore.R
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

internal class AddressFieldScrollScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.fillMaxSize(),
    )

    @Test
    fun scrollsAddressFieldToTopWhenPredictionsAppear() {
        paparazziRule.gif(end = 1_400L) {
            var showPredictions by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(500L)
                showPredictions = true
            }

            AddressFormPage(showPredictions = showPredictions)
        }
    }
}

@Composable
private fun AddressFormPage(showPredictions: Boolean) {
    Box(
        modifier = Modifier
            .height(360.dp)
            .clipToBounds(),
    ) {
        val scrollState = rememberScrollState()
        FormScrollProvider(scrollState) { viewportModifier ->
            Column(
                modifier = Modifier
                    .then(viewportModifier)
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Card information",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TextField(
                    value = "4242 4242 4242 4242",
                    onValueChange = {},
                    label = { Text("Card number") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = "12/28",
                    onValueChange = {},
                    label = { Text("Expiry") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Billing address",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                val predictionsState = if (showPredictions) {
                    AutocompleteAddressInteractor.InlinePredictionsState.Results(
                        query = "4567",
                        predictions = listOf(
                            AutocompleteAddressInteractor.InlineAddressPrediction(
                                id = "1",
                                primaryText = "4567 Broadway",
                                secondaryText = "New York, NY, United States",
                            ),
                            AutocompleteAddressInteractor.InlineAddressPrediction(
                                id = "2",
                                primaryText = "4567 Beekman Place",
                                secondaryText = "New York, NY, United States",
                            ),
                            AutocompleteAddressInteractor.InlineAddressPrediction(
                                id = "3",
                                primaryText = "4567 Broad Street",
                                secondaryText = "New York, NY, United States",
                            ),
                        ),
                    )
                } else {
                    AutocompleteAddressInteractor.InlinePredictionsState.Idle
                }

                InlineAddressPredictionsUI(
                    state = predictionsState,
                    attributionDrawable = R.drawable.stripe_google_maps_logo,
                    onPredictionSelected = {},
                    onClear = {},
                    onEnterManually = {},
                )
                Spacer(modifier = Modifier.height(200.dp))
            }
        }
    }
}
