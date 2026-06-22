package com.stripe.android.uicore.elements

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

class InlineAddressPredictionsScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.padding(PaddingValues(vertical = 16.dp))
            .fillMaxWidth()
    )

    @Test
    fun testLoading() {
        paparazziRule.snapshot {
            InlineAddressPredictionsContent(
                state = AutocompleteAddressInteractor.InlinePredictionsState.Loading,
                attributionDrawable = R.drawable.stripe_google_maps_logo,
                onPredictionSelected = {},
                onClear = {},
                onEnterManually = {},
            )
        }
    }

    @Test
    fun testResultsWithPredictions() {
        paparazziRule.snapshot {
            InlineAddressPredictionsContent(
                state = AutocompleteAddressInteractor.InlinePredictionsState.Results(
                    query = "123 Main",
                    predictions = listOf(
                        AutocompleteAddressInteractor.InlineAddressPrediction(
                            id = "1",
                            primaryText = "123 Main Street",
                            secondaryText = "San Francisco, CA, USA",
                        ),
                        AutocompleteAddressInteractor.InlineAddressPrediction(
                            id = "2",
                            primaryText = "123 Main Avenue",
                            secondaryText = "Los Angeles, CA, USA",
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

    @Test
    fun testResultsWithEnterManually() {
        paparazziRule.snapshot {
            InlineAddressPredictionsContent(
                state = AutocompleteAddressInteractor.InlinePredictionsState.Results(
                    query = "456 Oak",
                    predictions = listOf(
                        AutocompleteAddressInteractor.InlineAddressPrediction(
                            id = "1",
                            primaryText = "456 Oak Boulevard",
                            secondaryText = "Seattle, WA, USA",
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

    @Test
    fun testResultsWithoutAttribution() {
        paparazziRule.snapshot {
            InlineAddressPredictionsContent(
                state = AutocompleteAddressInteractor.InlinePredictionsState.Results(
                    query = "789 Elm",
                    predictions = listOf(
                        AutocompleteAddressInteractor.InlineAddressPrediction(
                            id = "1",
                            primaryText = "789 Elm Drive",
                            secondaryText = "Portland, OR, USA",
                        ),
                    ),
                ),
                attributionDrawable = null,
                onPredictionSelected = {},
                onClear = {},
                onEnterManually = null,
            )
        }
    }
}
