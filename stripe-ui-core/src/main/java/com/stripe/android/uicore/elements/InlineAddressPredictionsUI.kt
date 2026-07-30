package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.R
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.annotatedStringResource

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun InlineAddressPredictionsUI(
    state: AutocompleteAddressInteractor.InlinePredictionsState,
    attributionDrawable: Int?,
    onPredictionSelected: (String) -> Unit,
    onClear: () -> Unit,
    onEnterManually: (() -> Unit)?,
) {
    if (!shouldShowPredictionsDropdown(state)) {
        return
    }

    Card(
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        InlineAddressPredictionsContent(
            state = state,
            attributionDrawable = attributionDrawable,
            onPredictionSelected = onPredictionSelected,
            onClear = onClear,
            onEnterManually = onEnterManually,
        )
    }
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun InlineAddressPredictionsContent(
    state: AutocompleteAddressInteractor.InlinePredictionsState,
    attributionDrawable: Int?,
    onPredictionSelected: (String) -> Unit,
    onClear: () -> Unit,
    onEnterManually: (() -> Unit)?,
) {
    val results = state as? AutocompleteAddressInteractor.InlinePredictionsState.Results

    Column(modifier = Modifier.fillMaxWidth()) {
        PredictionsHeader(
            onEnterManually = onEnterManually,
            onClear = onClear,
        )
        if (results != null) {
            Divider()
            PredictionsListItems(
                results = results,
                onPredictionSelected = onPredictionSelected,
            )
        }
        PredictionsFooter(attributionDrawable = attributionDrawable)
    }
}

@Composable
private fun PredictionsHeader(
    onEnterManually: (() -> Unit)?,
    onClear: () -> Unit,
) {
    val closeIcon = remember(onClear) {
        TextFieldIcon.Trailing(
            idRes = R.drawable.stripe_ic_material_close,
            isTintable = true,
            onClick = onClear,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        if (onEnterManually != null) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onEnterManually),
            ) {
                Text(
                    text = stringResource(R.string.stripe_address_enter_manually),
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.body1,
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f))
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.stripeColors.onComponent) {
            TrailingIcon(
                trailingIcon = closeIcon,
                loading = false,
                modifier = Modifier
                    .height(24.dp)
                    .width(16.dp),
            )
        }
    }
}

@Composable
private fun PredictionsFooter(attributionDrawable: Int?) {
    if (attributionDrawable == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.stripe_address_suggestions),
            color = MaterialTheme.stripeColors.subtitle,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(end = 4.dp),
        )
        Image(
            painter = painterResource(id = attributionDrawable),
            contentDescription = stringResource(R.string.stripe_address_google_maps),
            modifier = Modifier.height(18.dp).padding(top = 3.dp),
        )
    }
}

@Composable
private fun PredictionsListItems(
    results: AutocompleteAddressInteractor.InlinePredictionsState.Results,
    onPredictionSelected: (String) -> Unit,
) {
    val queryRegex = remember(results.query) { buildQueryRegex(results.query) }
    results.predictions.forEach { prediction ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPredictionSelected(prediction.id) }
                .padding(vertical = 8.dp, horizontal = 16.dp),
        ) {
            val boldText = remember(prediction.primaryText, queryRegex) {
                applyBoldMatches(prediction.primaryText, queryRegex)
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
}

private fun buildQueryRegex(query: String): Regex? {
    if (query.isBlank()) return null
    val pattern = query.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString("|") { Regex.escape(it) }
    if (pattern.isEmpty()) return null
    return pattern.toRegex(RegexOption.IGNORE_CASE)
}

private fun applyBoldMatches(primaryText: String, queryRegex: Regex?): String {
    if (queryRegex == null) return primaryText
    return queryRegex.replace(primaryText) { "<b>${it.value}</b>" }
}

internal fun shouldShowPredictionsDropdown(
    state: AutocompleteAddressInteractor.InlinePredictionsState
): Boolean {
    return when (state) {
        AutocompleteAddressInteractor.InlinePredictionsState.Idle -> false
        AutocompleteAddressInteractor.InlinePredictionsState.Loading -> false
        is AutocompleteAddressInteractor.InlinePredictionsState.Results -> true
    }
}
