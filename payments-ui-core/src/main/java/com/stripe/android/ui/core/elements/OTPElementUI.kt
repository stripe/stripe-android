package com.stripe.android.ui.core.elements

import android.view.KeyEvent
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
private fun OTPElementPreview() {
    OTPElementUI(
        enabled = true,
        element = OTPSpec.transform()
    )
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun OTPElementUI(
    enabled: Boolean,
    element: OTPElement,
    modifier: Modifier = Modifier,
    colors: TextFieldColors = TextFieldColors()
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        var focusedElementIndex by remember { mutableStateOf(-1) }

        (0 until element.controller.otpLength).map { index ->
            SectionCard(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
            ) {
                val value by element.controller.fieldValues[index].collectAsState("")

                var textFieldModifier = Modifier
                    .padding(0.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            focusedElementIndex = index
                        } else if (!focusState.isFocused && focusedElementIndex == index) {
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

                androidx.compose.material.TextField(
                    value = TextFieldValue(
                        text = value,
                        selection = if (focusedElementIndex == index) {
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
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
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
                    placeholder = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = if (focusedElementIndex != index) "●" else "",
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = colors
                )
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
