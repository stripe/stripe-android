package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.stripe.android.uicore.R
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.annotatedStringResource

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun InlineAddressPredictionsUI(
    state: AutocompleteAddressInteractor.InlinePredictionsState,
    attributionDrawable: Int?,
    fieldWidthDp: Dp,
    onPredictionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onEnterManually: (() -> Unit)? = null,
) {
    val loading = state is AutocompleteAddressInteractor.InlinePredictionsState.Loading
    val results = state as? AutocompleteAddressInteractor.InlinePredictionsState.Results
    val expanded = loading || (results != null && results.predictions.isNotEmpty())

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = if (fieldWidthDp > 0.dp) Modifier.width(fieldWidthDp) else Modifier.fillMaxWidth(),
        properties = PopupProperties(focusable = false),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .size(24.dp),
                        color = MaterialTheme.colors.primary,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            IconButton(onClick = onClear) {
                Icon(
                    painter = painterResource(id = R.drawable.stripe_ic_material_close),
                    contentDescription = null,
                    tint = MaterialTheme.stripeColors.onComponent,
                )
            }
        }

        if (!loading && results != null) {
            Divider()
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

            onEnterManually?.let {
                Divider()
                Text(
                    text = stringResource(R.string.stripe_address_enter_manually),
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { it() }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
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
