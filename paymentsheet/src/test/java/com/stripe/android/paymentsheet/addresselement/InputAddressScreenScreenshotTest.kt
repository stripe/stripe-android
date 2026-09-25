package com.stripe.android.paymentsheet.addresselement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class InputAddressScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
    )

    @Test
    fun saveError() {
        paparazziRule.snapshot {
            val formController = remember { createFormController() }

            InputAddressTestScreen(
                formController = formController,
                saveError = SAVE_ERROR,
            )
        }
    }

    @Test
    fun saveErrorClearedByEditThenValidationError() {
        paparazziRule.gif(end = 3000L) {
            val formController = remember { createFormController() }
            var saveError by remember { mutableStateOf<ResolvableString?>(SAVE_ERROR) }

            LaunchedEffect(Unit) {
                delay(1000L)
                formController.setRawValues(INITIAL_VALUES + (FormFieldId.Line1 to ""))
                saveError = null
                delay(1000L)
                formController.elements.forEach { it.onValidationStateChanged(true) }
            }

            InputAddressTestScreen(
                formController = formController,
                saveError = saveError,
            )
        }
    }

    @Composable
    private fun InputAddressTestScreen(
        formController: AddressFormController,
        saveError: ResolvableString?,
    ) {
        val completeValues by formController.completeFormValues.collectAsState()

        InputAddressScreen(
            primaryButtonEnabled = completeValues != null,
            primaryButtonText = "Save address",
            title = "Shipping address",
            onPrimaryButtonClick = {},
            onDisabledButtonClick = {},
            onCloseClick = {},
            topContent = {},
            formContent = {
                FormUI(
                    hiddenIdentifiersFlow = stateFlowOf(emptySet()),
                    enabledFlow = stateFlowOf(true),
                    elementsFlow = stateFlowOf(formController.elements),
                    lastTextFieldIdentifierFlow = formController.lastTextFieldIdentifier,
                )
            },
            bottomContent = {},
            saveError = saveError,
        )
    }

    private fun createFormController(): AddressFormController {
        return AddressFormController(
            initialValues = INITIAL_VALUES,
            config = null,
            interactor = NoOpAutocompleteAddressInteractor,
        )
    }

    private object NoOpAutocompleteAddressInteractor : AutocompleteAddressInteractor {
        override val autocompleteConfig = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = null,
            autocompleteCountries = emptySet(),
        )

        override val inlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
            stateFlowOf(AutocompleteAddressInteractor.InlinePredictionsState.Idle)

        override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) = Unit

        override fun onAutocomplete(country: String) = Unit
    }

    private companion object {
        val SAVE_ERROR = "Taxes can't be calculated for this address.".resolvableString
        val INITIAL_VALUES = mapOf(
            FormFieldId.Name to "Jenny Rosen",
            FormFieldId.Country to "US",
            FormFieldId.Line1 to "510 Townsend St",
            FormFieldId.City to "San Francisco",
            FormFieldId.State to "CA",
            FormFieldId.PostalCode to "94103",
        )
    }
}
