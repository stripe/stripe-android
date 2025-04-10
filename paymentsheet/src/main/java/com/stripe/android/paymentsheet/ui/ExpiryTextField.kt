package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.stripe.android.uicore.elements.ExpiryDateVisualTransformation
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.elements.compat.errorSemanticsWithDefault
import com.stripe.android.uicore.strings.resolve

@SuppressWarnings("SpreadOperator")
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ExpiryTextField(
    modifier: Modifier = Modifier,
    state: ExpiryDateState,
    onValueChange: (String) -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isError = state.shouldShowError()
    val colors = TextFieldColors(
        shouldShowError = isError,
    )
    CommonTextField(
        modifier = modifier
            .indicatorLine(
                enabled = state.enabled,
                isError = isError && state.enabled,
                interactionSource = interactionSource,
                colors = colors
            )
            .errorSemanticsWithDefault(
                isError = isError,
                errorMessage = state.sectionError()?.resolve()
            ),
        value = state.text,
        onValueChange = onValueChange,
        enabled = state.enabled,
        label = stringResource(id = com.stripe.android.uicore.R.string.stripe_expiration_date_hint),
        shape = MaterialTheme.shapes.small.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize,
            bottomEnd = ZeroCornerSize,
        ),
        shouldShowError = isError,
        visualTransformation = ExpiryDateVisualTransformation(CARD_EDIT_UI_FALLBACK_EXPIRY_DATE),
        colors = colors
    )
}
