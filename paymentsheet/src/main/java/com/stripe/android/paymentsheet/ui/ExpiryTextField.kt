package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.elements.ExpiryDateVisualTransformation
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.canAcceptInput
import com.stripe.android.uicore.elements.compat.errorSemanticsWithDefault

@SuppressWarnings("SpreadOperator")
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ExpiryTextField(
    modifier: Modifier = Modifier,
    expDate: String,
    enabled: Boolean,
    validator: (String) -> TextFieldState,
    onValueChange: (String) -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onErrorChanged: (String?) -> Unit
) {
    var date by rememberSaveable("date") {
        mutableStateOf(expDate)
    }
    val textFieldState = remember(date) {
        validator(date)
    }
    val isError = enabled && textFieldState.shouldShowError(hasFocus = true)
    val sectionErrorString = textFieldState.getError()?.takeIf {
        isError
    }?.let {
        it.formatArgs?.let { args ->
            stringResource(
                it.errorMessage,
                *args
            )
        } ?: stringResource(it.errorMessage)
    }
    val colors = TextFieldColors(
        shouldShowError = isError,
    )
    CommonTextField(
        modifier = modifier
            .indicatorLine(
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors
            )
            .errorSemanticsWithDefault(isError, sectionErrorString),
        value = date,
        onValueChange = { newValue ->
            val canAcceptInput = textFieldState.canAcceptInput(
                currentValue = date,
                proposedValue = newValue
            )
            if (canAcceptInput.not()) return@CommonTextField
            date = newValue
            onValueChange(newValue)
        },
        enabled = enabled,
        label = stringResource(id = com.stripe.android.uicore.R.string.stripe_expiration_date_hint),
        shape = MaterialTheme.shapes.small.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize,
            bottomEnd = ZeroCornerSize,
        ),
        shouldShowError = textFieldState.shouldShowError(true),
        visualTransformation = if (enabled) {
            ExpiryDateVisualTransformation()
        } else {
            VisualTransformation.None
        }
    )

    LaunchedEffect(sectionErrorString) {
        onErrorChanged(sectionErrorString)
    }
}
