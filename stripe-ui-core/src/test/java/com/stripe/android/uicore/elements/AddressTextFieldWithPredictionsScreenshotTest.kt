package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.R
import org.junit.Rule
import org.junit.Test

class AddressTextFieldWithPredictionsScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier
            .padding(PaddingValues(vertical = 16.dp, horizontal = 16.dp))
            .fillMaxWidth(),
    )

    @Test
    fun testAddressFieldWithPredictionsDropdown() {
        paparazziRule.snapshot {
            Column(modifier = Modifier.fillMaxWidth()) {
                AddressTextFieldUI(
                    controller = createTestController(query = "4567"),
                    enabled = true,
                    onSearchActivated = null,
                )
                InlineAddressPredictionsUI(
                    state = AutocompleteAddressInteractor.InlinePredictionsState.Results(
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
                            AutocompleteAddressInteractor.InlineAddressPrediction(
                                id = "4",
                                primaryText = "4567 Bleecker St",
                                secondaryText = "New York, NY, United States",
                            ),
                        ),
                    ),
                    attributionDrawable = R.drawable.stripe_google_maps_logo,
                    onPredictionSelected = {},
                    onClear = {},
                    onEnterManually = {},
                )
            }
        }
    }

    @Test
    fun testAddressFieldWithNoPredictions() {
        paparazziRule.snapshot {
            Column(modifier = Modifier.fillMaxWidth()) {
                AddressTextFieldUI(
                    controller = createTestController(query = ""),
                    enabled = true,
                    onSearchActivated = null,
                )
                InlineAddressPredictionsUI(
                    state = AutocompleteAddressInteractor.InlinePredictionsState.Idle,
                    attributionDrawable = null,
                    onPredictionSelected = {},
                    onClear = {},
                    onEnterManually = null,
                )
            }
        }
    }

    @Test
    fun testAddressFieldWithSinglePrediction() {
        paparazziRule.snapshot {
            Column(modifier = Modifier.fillMaxWidth()) {
                AddressTextFieldUI(
                    controller = createTestController(query = "123 Main"),
                    enabled = true,
                    onSearchActivated = null,
                )
                InlineAddressPredictionsUI(
                    state = AutocompleteAddressInteractor.InlinePredictionsState.Results(
                        query = "123 Main",
                        predictions = listOf(
                            AutocompleteAddressInteractor.InlineAddressPrediction(
                                id = "1",
                                primaryText = "123 Main Street",
                                secondaryText = "San Francisco, CA, USA",
                            ),
                        ),
                    ),
                    attributionDrawable = R.drawable.stripe_google_maps_logo,
                    onPredictionSelected = {},
                    onClear = {},
                    onEnterManually = {},
                )
            }
        }
    }

    private fun createTestController(query: String): AddressTextFieldController {
        val controller = AddressTextFieldController(
            label = com.stripe.android.core.strings.resolvableString(value = "Address"),
            addressInputMode = AddressInputMode.NoAutocomplete(),
            inlineAutocompleteHandler = null,
            reportsFormValue = false,
            initialQuery = query,
            showEnterManually = true,
        )
        return controller
    }
}
