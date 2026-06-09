package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.annotatedStringResource

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun InlineAddressPredictionsUI(
    state: AutocompleteAddressInteractor.InlinePredictionsState,
    attributionDrawable: Int?,
    onPredictionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val loading = state is AutocompleteAddressInteractor.InlinePredictionsState.Loading
    val results = state as? AutocompleteAddressInteractor.InlinePredictionsState.Results
    val expanded = loading || (results != null && results.predictions.isNotEmpty())

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        properties = PopupProperties(focusable = false),
    ) {
        if (loading) {
            Box(modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 8.dp),
                )
            }
        } else if (results != null) {
            results.predictions.forEach { prediction ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPredictionSelected(prediction.id) }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                ) {
                    val boldText = remember(prediction.primaryText, results.query) {
                        buildBoldMatchText(prediction.primaryText, results.query)
                    }
                    Text(
                        text = annotatedStringResource(text = boldText),
                        color = MaterialTheme.stripeColors.onComponent,
                        style = MaterialTheme.typography.body1,
                    )
                    Text(
                        text = prediction.secondaryText,
                        color = MaterialTheme.stripeColors.onComponent,
                        style = MaterialTheme.typography.body1,
                    )
                }
                Divider()
            }
            attributionDrawable?.let { drawable ->
                Image(
                    painter = painterResource(id = drawable),
                    contentDescription = null,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                )
            }
        }
    }
}

private fun buildBoldMatchText(primaryText: String, query: String): String {
    if (query.isBlank()) return primaryText
    val pattern = query.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString("|") { Regex.escape(it) }
    if (pattern.isEmpty()) return primaryText
    return pattern.toRegex(RegexOption.IGNORE_CASE).replace(primaryText) { "<b>${it.value}</b>" }
}
