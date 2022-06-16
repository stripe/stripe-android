package com.stripe.android.ui.core.elements

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.ImeAction
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.autocomplete.AddressAutocompleteContract

@Composable
fun AddressAutocompleteTextFieldUI(
    controller: AutocompleteAddressTextFieldController,
) {
    val address = controller.address.collectAsState(null)
    val launcher = rememberLauncherForActivityResult(
        contract = AddressAutocompleteContract(),
        onResult = {
            it?.address?.let {
                controller.address.value = it
            }
        }
    )

    TextField(
        textFieldController = SimpleTextFieldController(
            SimpleTextFieldConfig(
                label = R.string.address_label_address_line1
            ),
            initialValue = address.value?.line1
        ),
        imeAction = ImeAction.Next,
        enabled = true,
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            launcher.launch(
                                AddressAutocompleteContract.Args(
                                    country = controller.country,
                                    googlePlacesApiKey = controller.googlePlacesApiKey,
                                )
                            )
                        }
                    }
                }
            }
    )
}
