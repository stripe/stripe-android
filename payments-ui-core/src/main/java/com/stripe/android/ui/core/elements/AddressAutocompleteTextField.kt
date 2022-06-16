package com.stripe.android.ui.core.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stripe.android.model.Address
import com.stripe.android.ui.core.address.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.address.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.paymentsColors

@Composable
fun AddressAutocompleteTextField(
    controller: AddressAutocompleteTextFieldController,
    onResult: (Address?) -> Unit
) {
    val predictions by controller.predictions.collectAsState()
    val loading by controller.loading.collectAsState(initial = false)

    LaunchedEffect(controller.addressResult) {
        controller.addressResult.collect { result ->
            result?.fold(
                onSuccess = {
                    onResult(it)
                },
                onFailure = {
                    onResult(null)
                }
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        AutocompleteTextField(
            controller = controller.autocompleteController,
            loading = loading,
            predictions = predictions,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            controller.selectPrediction(it)
        }
    }
}


@Composable
internal fun AutocompleteTextField(
    modifier: Modifier = Modifier,
    controller: TextFieldController,
    loading: Boolean,
    predictions: List<AutocompletePrediction>,
    onPredictionSelection: (AutocompletePrediction) -> Unit
) {
    val query = controller.fieldValue.collectAsState(initial = "")
    val attributionDrawable =
        PlacesClientProxy.getPlacesPoweredByGoogleDrawable(LocalContext.current)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = modifier.fillMaxWidth()) {
            TextFieldSection(
                textFieldController = controller,
                imeAction = ImeAction.Done,
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator()
            }
        } else if (predictions.isNotEmpty()) {
            Divider(
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                predictions.forEach { prediction ->
                    val primaryText = prediction.primaryText
                    val secondaryText = prediction.secondaryText
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPredictionSelection(prediction)
                            }
                    ) {
                        val regex = query.value
                            .replace(" ", "|")
                            .toRegex(RegexOption.IGNORE_CASE)
                        val matches = regex.findAll(primaryText).toList()
                        val values = matches.map {
                            it.value
                        }.filter { it.isNotBlank() }
                        var text = primaryText
                        values.forEach {
                            text = text.replace(it, "<b>$it</b>")
                        }
                        Text(
                            text = annotatedStringResource(text = text),
                            color = MaterialTheme.paymentsColors.onComponent,
                            style = MaterialTheme.typography.body1,
                        )
                        Text(
                            text = secondaryText,
                            color = MaterialTheme.paymentsColors.onComponent,
                            style = MaterialTheme.typography.body1,
                        )
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            attributionDrawable?.let { drawable ->
                Image(
                    painter = painterResource(
                        id = drawable
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}