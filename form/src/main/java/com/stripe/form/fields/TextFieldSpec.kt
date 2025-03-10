package com.stripe.form.fields

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.form.ContentBox
import com.stripe.form.ContentSpec
import com.stripe.form.FormFieldSpec
import com.stripe.form.FormFieldState
import com.stripe.form.Key
import com.stripe.form.ValidationResult
import com.stripe.form.ValueChange
import java.util.UUID

data class TextFieldSpec(
    override val state: TextFieldState,
) : FormFieldSpec<TextFieldValue> {

    @Composable
    override fun Content(modifier: Modifier) {
        var content by rememberSaveable(
            inputs = arrayOf(state.key),
            stateSaver = TextFieldValue.Saver
        ) { mutableStateOf(state.initialValue) }
        val validationResult by remember(content) {
            mutableStateOf(state.validator(content))
        }

        Column(
            modifier = modifier
        ) {
            TextField(
                modifier = Modifier
                    .testTag(state.testTag)
                    .fillMaxWidth(),
                value = content,
                onValueChange = {
                    if (it.text.length > state.maxLength) return@TextField
                    content = it
                    state.onValueChange(
                        ValueChange(
                            key = state.key,
                            value = it,
                            isComplete = validationResult.isValid
                        )
                    )
                },
                label = {
                    state.label?.let {
                        ContentBox(it)
                    }
                },
                trailingIcon = {
                    state.trailing?.let {
                        ContentBox(it)
                    }
                },
                visualTransformation = state.visualTransformation,
                isError = validationResult.isValid.not(),
                keyboardOptions = state.keyboardOptions,
                readOnly = state.readOnly,
                enabled = state.enabled
            )

            when (val result = validationResult) {
                is ValidationResult.Invalid -> {
                    result.message?.let {
                        ContentBox(
                            modifier = Modifier.fillMaxWidth(),
                            spec = it
                        )
                    }
                }
                ValidationResult.Valid -> Unit
            }
        }
    }


    @Immutable
    data class TextFieldState(
        override val key: Key<TextFieldValue>,
        val label: ContentSpec?,
        val trailing: ContentSpec? = null,
        val initialValue: TextFieldValue = TextFieldValue(),
        val visualTransformation: VisualTransformation = VisualTransformation.None,
        val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        val maxLength: Int = Int.MAX_VALUE,
        val readOnly: Boolean = false,
        val enabled: Boolean = true,
        val testTag: String = UUID.randomUUID().toString(),
        override val onValueChange: (ValueChange<TextFieldValue>) -> Unit,
        override val validator: (TextFieldValue) -> ValidationResult = { ValidationResult.Valid },
    ) : FormFieldState<TextFieldValue>
}