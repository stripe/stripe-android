package com.stripe.form.example.ui.theme.customform

import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stripe.form.FormFieldSpec
import com.stripe.form.FormFieldState
import com.stripe.form.Key
import com.stripe.form.ValidationResult
import com.stripe.form.ValueChange

class CustomSliderSpec(
    override val state: State
): FormFieldSpec<Float> {

    @Composable
    override fun Content(modifier: Modifier) {
        var value by rememberSaveable(state.key) { mutableFloatStateOf(state.initialValue) }
        Slider(
            value = value,
            onValueChange = {
                value = it
                state.onValueChange(
                    ValueChange(
                        key = state.key,
                        value = it,
                        isComplete = state.validator(it).isValid
                    )
                )
            }
        )
    }

    data class State(
        override val key: Key<Float>,
        val initialValue: Float = .5f,
        override val onValueChange: (ValueChange<Float>) -> Unit,
        override val validator: (Float) -> ValidationResult = { ValidationResult.Valid }
    ): FormFieldState<Float>
}