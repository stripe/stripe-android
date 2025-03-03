package com.stripe.form.fields

import android.os.Parcelable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stripe.form.ContentBox
import com.stripe.form.ContentSpec
import com.stripe.form.FormFieldSpec
import com.stripe.form.FormFieldState
import com.stripe.form.Key
import com.stripe.form.ValidationResult
import com.stripe.form.ValueChange

@Stable
data class CheckBoxSpec(
    override val state: CheckBoxState
) : FormFieldSpec<Boolean> {

    @Composable
    override fun Content(modifier: Modifier) {
        var checked by rememberSaveable(state.key) { mutableStateOf(state.checked) }
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    state.onValueChange(
                        ValueChange(
                            key = state.key,
                            value = it,
                            isComplete = state.validator(it).isValid
                        )
                    )
                }
            )

            state.label?.let {
                ContentBox(
                    modifier = Modifier.weight(1f),
                    spec = it
                )
            }
        }
    }

    @Immutable
    data class CheckBoxState(
        override val key: Key<Boolean>,
        val checked: Boolean = false,
        val label: ContentSpec? = null,
        override val onValueChange: (ValueChange<Boolean>) -> Unit,
        override val validator: (Boolean) -> ValidationResult = { ValidationResult.Valid },
    ) : FormFieldState<Boolean>
}