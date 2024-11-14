@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.uicore.elements

import android.view.KeyEvent
import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBorderStrokeWidth
import com.stripe.android.uicore.moveFocusSafely
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.autofill
import com.stripe.android.uicore.utils.collectAsState

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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun OTPElementUI(
    enabled: Boolean,
    element: OTPElement,
    modifier: Modifier = Modifier,
    boxShape: Shape = MaterialTheme.shapes.medium,
    boxTextStyle: TextStyle = OTPElementUI.defaultTextStyle(),
    boxSpacing: Dp = 8.dp,
    middleSpacing: Dp = 20.dp,
    otpInputPlaceholder: String = "●",
    colors: OTPElementColors = OTPElementColors(
        selectedBorder = MaterialTheme.colors.primary,
        placeholder = MaterialTheme.stripeColors.placeholderText
    ),
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        var focusedElementIndex by remember { mutableIntStateOf(-1) }

        (0 until element.controller.otpLength).map { index ->
            val isSelected = focusedElementIndex == index

            when (index) {
                0 -> Unit
                element.controller.otpLength / 2 -> Spacer(modifier = Modifier.width(middleSpacing))
                else -> Spacer(modifier = Modifier.width(boxSpacing))
            }

            SectionCard(
                modifier = Modifier
                    .alpha(if (enabled) 1f else ContentAlpha.disabled)
                    .weight(1f),
                shape = boxShape,
                backgroundColor = MaterialTheme.stripeColors.component,
                border = BorderStroke(
                    width = MaterialTheme.getBorderStrokeWidth(isSelected),
                    color = if (isSelected) {
                        colors.selectedBorder
                    } else {
                        MaterialTheme.stripeColors.componentBorder
                    }
                )
            ) {
                val value by element.controller.fieldValues[index].collectAsState()

                var textFieldModifier = Modifier
                    .height(56.dp)
                    .autofill(
                        types = listOf(AutofillType.SmsOtpCode),
                        onFill = element.controller::onAutofillDigit,
                    )
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
                            focusManager.moveFocusSafely(FocusDirection.Previous)
                            element.controller.onValueChanged(index - 1, "")
                            return@onPreviewKeyEvent true
                        }
                        false
                    }
                    .testTag("OTP-$index")
                    .semantics { testTagsAsResourceId = true }

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
                    placeholder = otpInputPlaceholder,
                    textStyle = boxTextStyle,
                    enabled = enabled,
                    colors = colors
                )
            }
        }
    }
}

@Composable
private fun OTPInputBox(
    value: String,
    isSelected: Boolean,
    textStyle: TextStyle,
    element: OTPElement,
    index: Int,
    focusManager: FocusManager,
    modifier: Modifier,
    enabled: Boolean,
    colors: OTPElementColors,
    placeholder: String
) {
    // Need to use BasicTextField instead of TextField to be able to customize the
    // internal contentPadding
    BasicTextField(
        value = TextFieldValue(
            text = value,
            selection = if (isSelected) {
                TextRange(value.length)
            } else {
                TextRange.Zero
            }
        ),
        onValueChange = {
            // If the OTPInputBox already has a value, it would be the first character of it.text
            // remove it before passing it to the controller.
            val newValue =
                if (value.isNotBlank() && it.text.isNotBlank()) {
                    it.text.substring(1)
                } else {
                    it.text
                }
            val inputLength = element.controller.onValueChanged(index, newValue)
            (0 until inputLength).forEach { _ -> focusManager.moveFocusSafely(FocusDirection.Next) }
        },
        modifier = modifier,
        enabled = enabled,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.stripeColors.textCursor),
        keyboardOptions = KeyboardOptions(
            keyboardType = element.controller.keyboardType
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocusSafely(FocusDirection.Next) },
            onDone = { focusManager.clearFocus(true) }
        ),
        singleLine = true,
        decorationBox = OTPInputDecorationBox(value, isSelected, placeholder, enabled, colors)
    )
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun OTPInputDecorationBox(
    value: String,
    isSelected: Boolean,
    placeholder: String,
    enabled: Boolean,
    colors: OTPElementColors
) = @Composable { innerTextField: @Composable () -> Unit ->
    TextFieldDefaults.TextFieldDecorationBox(
        value = value,
        visualTransformation = VisualTransformation.None,
        innerTextField = innerTextField,
        placeholder = {
            Text(
                text = if (!isSelected) placeholder else "",
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

internal object OTPElementUI {

    @Composable
    fun defaultTextStyle() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        color = MaterialTheme.stripeColors.onComponent,
        textAlign = TextAlign.Center
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class OTPElementColors(
    val selectedBorder: Color,
    val placeholder: Color
)
