package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.stripe.android.uicore.elements.ExpiryDateVisualTransformation
import com.stripe.android.uicore.elements.TextFieldState

@Composable
fun ExpiryTextField(
    modifier: Modifier = Modifier,
    validator: (String) -> TextFieldState,
    onValueChange: (String) -> Unit
) {
    var date by rememberSaveable("date") {
        mutableStateOf("")
    }
    CommonTextField(
        modifier = modifier,
        value = date,
        onValueChange = onValueChange,
        label = stringResource(id = com.stripe.android.uicore.R.string.stripe_expiration_date_hint),
        shape = MaterialTheme.shapes.small.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize,
            bottomEnd = ZeroCornerSize,
        ),
        shouldShowError = validator(date).shouldShowError(true),
        enabled = true,
        visualTransformation = ExpiryDateVisualTransformation()
    )
}
