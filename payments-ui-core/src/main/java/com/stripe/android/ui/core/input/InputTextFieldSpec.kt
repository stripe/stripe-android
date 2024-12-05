package com.stripe.android.ui.core.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.elements.compat.CompatTextField

@Immutable
data class InputTextFieldSpec(
    val text: String = "",
    val label: Spec? = null,
    val trailing: Spec? = null,
    val readOnly: Boolean = false,
    val onValueChanged: (String) -> Unit
) : InputSpec {

    override val valid = true

    @Composable
    override fun Content(modifier: Modifier) {
        val colors = TextFieldColors(shouldShowError = valid.not())

        CompatTextField(
            value = TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            ),
            onValueChange = { newValue ->
                onValueChanged(newValue.text)
            },
            modifier = modifier,
            enabled = true,
            label = {
                if (label != null) {
                    SpecBox(
                        spec = label
                    )
                }
            },
            isError = valid.not(),
            singleLine = true,
            colors = colors
        )
    }
}
