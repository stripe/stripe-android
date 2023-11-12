package com.stripe.android.paymentsheet.prototype.uielement

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.prototype.UiElementDefinition
import com.stripe.android.paymentsheet.prototype.UiRenderer
import com.stripe.android.paymentsheet.prototype.UiState
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class TextFieldUiElementDefinition private constructor(
    private val label: ResolvableString,
    private val propertyFetcher: (UiState.Snapshot) -> String,
    private val propertyUpdater: (UiState, String) -> Unit,
    private val flowFetcher: (UiState) -> Flow<String>,
) : UiElementDefinition {
    override fun isComplete(uiState: UiState.Snapshot): Boolean {
        return propertyFetcher(uiState).isNotEmpty()
    }

    override fun renderer(uiState: UiState): UiRenderer {
        return TextFieldUiRenderer(label, flowFetcher(uiState)) { updatedValue ->
            propertyUpdater(uiState, updatedValue)
        }
    }

    companion object {
        fun <V : UiState.Value> createTextField(
            label: ResolvableString,
            key: UiState.Key<V>,
            mapper: (state: V) -> String,
            updater: (state: V, updatedValue: String) -> V,
        ): TextFieldUiElementDefinition {
            return TextFieldUiElementDefinition(
                label = label,
                propertyFetcher = { uiState -> mapper(uiState[key]) },
                propertyUpdater = { uiState, updatedValue ->
                    uiState.update(key) { state ->
                        updater(state, updatedValue)
                    }
                },
                flowFetcher = { uiState ->
                    uiState[key].map(mapper)
                }
            )
        }
    }
}

private class TextFieldUiRenderer(
    private val label: ResolvableString,
    private val valueFlow: Flow<String>,
    private val onValueUpdated: (String) -> Unit,
) : UiRenderer {
    @Composable
    override fun Content(
        enabled: Boolean,
        modifier: Modifier
    ) {
        val value by valueFlow.collectAsState("")

        TextField(
            value = value,
            onValueChange = onValueUpdated,
            enabled = enabled,
            label = {
                val color = MaterialTheme.stripeColors.placeholderText
                Text(
                    text = label.resolve(),
                    modifier = modifier,
                    color = if (enabled) color else color.copy(alpha = ContentAlpha.disabled),
                    style = MaterialTheme.typography.subtitle1
                )
            },
            singleLine = true,
            modifier = modifier.fillMaxWidth(),
        )
    }
}
