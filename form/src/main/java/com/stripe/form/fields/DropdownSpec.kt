package com.stripe.form.fields

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stripe.form.FormFieldSpec
import com.stripe.form.FormFieldState
import com.stripe.form.Key
import com.stripe.form.ValidationResult
import com.stripe.form.ValueChange
import kotlinx.collections.immutable.ImmutableList

data class DropdownSpec(
    override val state: State
) : FormFieldSpec<String> {

    @Composable
    override fun Content(modifier: Modifier) {
        var expanded by rememberSaveable(state.key) { mutableStateOf(false) }
        var selectedIndex by rememberSaveable("${state.key}_index") { mutableIntStateOf(state.initialIndex) }
        ExposedDropdownMenuBox(
            modifier = modifier,
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = state.options[selectedIndex].displayValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )

            ExposedDropdownMenu(
                modifier = Modifier
                    .fillMaxWidth(),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                state.options.forEachIndexed { index, selectionOption ->
                    DropdownMenuItem(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            selectedIndex = index
                            expanded = false
                            state.onValueChange(
                                ValueChange(
                                    key = state.key,
                                    value = selectionOption.rawValue,
                                    isComplete = state.validator.invoke(selectionOption.rawValue).isValid
                                )
                            )
                        }
                    ) {
                        Text(text = selectionOption.displayValue)
                    }
                }
            }

        }
    }

    data class State(
        override val key: Key<String>,
        val options: ImmutableList<Option>,
        val initialIndex: Int = 0,
        override val onValueChange: (ValueChange<String>) -> Unit,
        override val validator: (String) -> ValidationResult = { ValidationResult.Valid }
    ) : FormFieldState<String>

    data class Option(
        val rawValue: String,
        val displayValue: String
    )
}
