package com.stripe.android.ui.core.elements

import android.view.KeyEvent
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun OTPElementUI(
    colors: TextFieldColors,
    onComplete: (String) -> Unit
) {
    val otpLength = 6
    val codes = remember {
        mutableStateListOf(*((0 until otpLength).map { "" }.toTypedArray() ))
    }
    val focusRequesters: List<FocusRequester> = remember {
        (0 until otpLength).map { FocusRequester() }
    }

    val width = LocalConfiguration.current.screenWidthDp.dp / (otpLength + 2)

    // Prevent the callback from being called twice when updating the field programmatically
    var valueChangedCallbackEnabled = true

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (0 until otpLength).map { index ->
            if (index == 0) {
                LaunchedEffect(Unit) {
                    focusRequesters[0].requestFocus()
                }
            }
            OTPCell(
                modifier = Modifier
                    .width(width)
                    .onKeyEvent { event ->
                        val value = codes[index]
                        if (event.type == KeyEventType.KeyUp) {
                            if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL && value.isEmpty()) {
                                focusRequesters.getOrNull(index - 1)?.requestFocus()
                            } else if (value.isNotEmpty()) {
                                focusRequesters.getOrNull(index + 1)?.requestFocus()
                            }
                        }
                        false
                    }
                    .focusOrder(focusRequesters[index])
                    .focusRequester(focusRequesters[index]),
                colors = colors,
                value = codes[index],

                onValueChange = { value ->
                    if (valueChangedCallbackEnabled && value.isDigitsOnly()) {
                        valueChangedCallbackEnabled = false
                        if (value.length > 1) {
                            val val1 = value.getOrNull(0)?.toString() ?: ""
                            val val2 = value.getOrNull(1)?.toString() ?: ""
                            codes[index] = if (codes[index] == val1) val2 else val1
                            return@OTPCell
                        }

                        codes[index] = value

                        val currentCode = codes.joinToString("")
                        if (currentCode.length == otpLength) {
                            onComplete(codes.joinToString(""))
                        }
                    }
                    valueChangedCallbackEnabled = true
                },
            )
        }
    }
}

@Composable
private fun OTPCell(
    modifier: Modifier,
    value: String,
    colors: TextFieldColors,
    onValueChange: (String) -> Unit,
) {
    val placeholder = remember { mutableStateOf("●") }
    SectionCard {
        androidx.compose.material.TextField(
            modifier = modifier.onFocusChanged {
                placeholder.value = if (!it.hasFocus) "●" else ""
            },
            maxLines = 1,
            value = value,
            placeholder = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = placeholder.value,
                    textAlign = TextAlign.Center
                )
            },
            onValueChange = { onValueChange(it) },
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
        )
    }
}