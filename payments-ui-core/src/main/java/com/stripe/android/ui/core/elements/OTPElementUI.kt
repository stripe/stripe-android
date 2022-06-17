package com.stripe.android.ui.core.elements

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.getBorderStrokeWidth
import com.stripe.android.ui.core.paymentsColors

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun OTPElementUI(
    enabled: Boolean,
    element: OTPElement,
    modifier: Modifier = Modifier,
    colors: OTPElementColors = OTPElementColors(
        selectedBorder = MaterialTheme.colors.primary,
        placeholder = MaterialTheme.paymentsColors.placeholderText
    )
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
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
                border = BorderStroke(
                    width = MaterialTheme.getBorderStrokeWidth(isSelected),
                    color = if (isSelected) {
                        colors.selectedBorder
                    } else {
                        MaterialTheme.paymentsColors.componentBorder
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
                        val inputLength = element.controller.onValueChanged(index, it.text)
                        (0 until inputLength).forEach { _ ->
                            focusManager.moveFocus(FocusDirection.Next)
                        }
                    },
                    modifier = textFieldModifier,
                    enabled = enabled,
                    textStyle = MaterialTheme.typography.h2.copy(
                        color = MaterialTheme.paymentsColors.onComponent,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(MaterialTheme.paymentsColors.textCursor),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = element.controller.keyboardType
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Next)
                        },
                        onDone = {
                            focusManager.clearFocus(true)
                        }
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
                                textColor = MaterialTheme.paymentsColors.onComponent,
                                backgroundColor = Color.Transparent,
                                cursorColor = MaterialTheme.paymentsColors.textCursor,
                                focusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                placeholderColor = colors.placeholder,
                                disabledPlaceholderColor = colors.placeholder
                            ),
                            contentPadding = PaddingValues(
                                TextFieldPadding,
                                TextFieldPadding,
                                TextFieldPadding,
                                TextFieldPadding
                            )
                        )
                    }
                )
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class OTPElementColors(
    val selectedBorder: Color,
    val placeholder: Color
)

private val TextFieldPadding = 12.dp
