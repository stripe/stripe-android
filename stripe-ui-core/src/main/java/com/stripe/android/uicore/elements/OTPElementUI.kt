@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.uicore.elements

import android.view.KeyEvent
import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBorderStrokeWidth
import com.stripe.android.uicore.stripeColors

@Composable
@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
internal fun OTPElementUIPreview() {
    StripeTheme {
        OTPElementUI(
            enabled = true,
            element = OTPElement(
                identifier = IdentifierSpec.Generic("otp"),
                controller = OTPController()
            )
        )
    }
}

@Composable
@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
internal fun OTPElementUIDisabledPreview() {
    StripeTheme {
        OTPElementUI(
            enabled = false,
            element = OTPElement(
                identifier = IdentifierSpec.Generic("otp"),
                controller = OTPController()
            )
        )
    }
}

@Composable
@Suppress("LongMethod")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun OTPElementUI(
    enabled: Boolean,
    element: OTPElement,
    modifier: Modifier = Modifier,
    colors: OTPElementColors = OTPElementColors(
        selectedBorder = MaterialTheme.colors.primary,
        placeholder = MaterialTheme.stripeColors.placeholderText
    ),
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        var focusedElementIndex by remember { mutableStateOf(-1) }

        (0 until element.controller.otpLength).map { index ->
            val isSelected = focusedElementIndex == index

            // Add extra spacing in the middle
            if (index == element.controller.otpLength / 2) {
                Spacer(modifier = Modifier.width(12.dp))
            }

            SectionCard(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                backgroundColor = if (enabled) {
                    MaterialTheme.stripeColors.component
                } else {
                    MaterialTheme.stripeColors.placeholderText.copy(alpha = 0.1f)
                },
                border = BorderStroke(
                    width = MaterialTheme.getBorderStrokeWidth(isSelected),
                    color = if (isSelected) {
                        colors.selectedBorder
                    } else {
                        MaterialTheme.stripeColors.componentBorder
                    }
                )
            ) {
                val value by element.controller.fieldValues[index].collectAsState("")
                var textFieldModifier = Modifier
                    .height(56.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            focusedElementIndex = index
                        } else if (!focusState.isFocused && isSelected) {
                            focusedElementIndex = -1
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        if (index != 0 &&
                            event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL &&
                            value.isEmpty()
                        ) {
                            // If the current field is empty, move to the previous one and delete
                            focusManager.moveFocus(FocusDirection.Previous)
                            element.controller.onValueChanged(index - 1, "")
                            return@onPreviewKeyEvent true
                        }
                        false
                    }
                    .semantics {
                        testTag = "OTP-$index"
                    }

                if (index == 0) {
                    textFieldModifier = textFieldModifier.focusRequester(focusRequester)
                }

                OTPInputBox(
                    value = value,
                    isSelected = isSelected,
                    element = element,
                    index = index,
                    focusManager = focusManager,
                    modifier = textFieldModifier,
                    enabled = enabled,
                    colors = colors
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun OTPInputBox(
    value: String,
    isSelected: Boolean,
    element: OTPElement,
    index: Int,
    focusManager: FocusManager,
    modifier: Modifier,
    enabled: Boolean,
    colors: OTPElementColors
) {
    // Need to use BasicTextField instead of TextField to be able to customize the
    // internal contentPadding
    BasicTextField(
        value = TextFieldValue(
            text = value,
            selection = if (isSelected) { TextRange(value.length) } else { TextRange.Zero }
        ),
        onValueChange = {
            // If the OTPInputBox already has a value, it would be the first character of it.text
            // remove it before passing it to the controller.
            val newValue =
                if (value.isNotBlank() && it.text.isNotBlank()) { it.text.substring(1) } else { it.text }
            val inputLength = element.controller.onValueChanged(index, newValue)
            (0 until inputLength).forEach { _ -> focusManager.moveFocus(FocusDirection.Next) }
        },
        modifier = modifier,
        enabled = enabled,
        textStyle = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            color = MaterialTheme.stripeColors.onComponent,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(MaterialTheme.stripeColors.textCursor),
        keyboardOptions = KeyboardOptions(
            keyboardType = element.controller.keyboardType
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus(true) }
        ),
        singleLine = true,
        decorationBox = @Composable { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = value,
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                placeholder = {
                    Text(
                        text = if (!isSelected) "●" else "",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                singleLine = true,
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = MaterialTheme.stripeColors.onComponent,
                    backgroundColor = Color.Transparent,
                    cursorColor = MaterialTheme.stripeColors.textCursor,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    placeholderColor = colors.placeholder,
                    disabledPlaceholderColor = colors.placeholder
                ),
                // TextField has a default padding, here we are specifying 0.dp padding
                contentPadding = PaddingValues()
            )
        }
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class OTPElementColors(
    val selectedBorder: Color,
    val placeholder: Color
)
