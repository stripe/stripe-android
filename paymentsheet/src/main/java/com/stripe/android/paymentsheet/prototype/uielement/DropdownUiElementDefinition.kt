package com.stripe.android.paymentsheet.prototype.uielement

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.prototype.UiElementDefinition
import com.stripe.android.paymentsheet.prototype.UiRenderer
import com.stripe.android.paymentsheet.prototype.UiState
import com.stripe.android.uicore.strings.resolve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DropdownItem(
    val identifier: String,
    val displayText: String,
)

internal class DropdownUiElementDefinition private constructor(
    private val label: ResolvableString,
    private val availableItems: List<DropdownItem>,
    private val identifierUpdater: (UiState, String) -> Unit,
    private val flowFetcher: (UiState) -> Flow<String>,
) : UiElementDefinition {
    override fun isComplete(uiState: UiState.Snapshot): Boolean {
        return true
    }

    override fun renderer(uiState: UiState): UiRenderer {
        return DropdownUiRenderer(
            label = label,
            availableItems = availableItems,
            selectedItemFlow = flowFetcher(uiState).map { identifier ->
                availableItems.first { it.identifier == identifier }
            },
            onItemSelected = { selectedItem ->
                identifierUpdater(uiState, selectedItem.identifier)
            }
        )
    }

    companion object {
        fun <V : UiState.Value> createDropdownField(
            label: ResolvableString,
            availableItems: List<DropdownItem>,
            key: UiState.Key<V>,
            mapper: (state: V) -> String,
            updater: (state: V, updatedValue: String) -> V,
        ): DropdownUiElementDefinition {
            return DropdownUiElementDefinition(
                label = label,
                availableItems = availableItems,
                identifierUpdater = { uiState, updatedValue ->
                    uiState.update(key) { state ->
                        updater(state, updatedValue)
                    }
                },
                flowFetcher = { uiState ->
                    uiState[key].map(mapper)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
internal class DropdownUiRenderer(
    private val label: ResolvableString,
    private val availableItems: List<DropdownItem>,
    private val selectedItemFlow: Flow<DropdownItem>,
    private val onItemSelected: (DropdownItem) -> Unit,
) : UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        val selectedItem by selectedItemFlow.collectAsState(initial = null)

        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextField(
                readOnly = true,
                value = selectedItem?.displayText.orEmpty(),
                onValueChange = {},
                label = { Text(label.resolve()) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                for (item in availableItems) {
                    DropdownMenuItem(
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        }
                    ) {
                        Text(
                            text = item.displayText,
                        )
                    }
                }
            }
        }
    }
}
