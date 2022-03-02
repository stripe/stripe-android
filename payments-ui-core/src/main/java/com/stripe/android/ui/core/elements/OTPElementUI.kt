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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun OTPElementUI(
    colors: TextFieldColors,
    controller: OTPController
) {
    val codes by controller.fieldValue.collectAsState(
        initial = (0 until controller.otpLength).map { "" }
    )
    val width = LocalConfiguration.current.screenWidthDp.dp / (controller.otpLength + 2)
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (0 until controller.otpLength).map { index ->
            OTPCell(
                modifier = Modifier
                    .width(width)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp) {
                            if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL) {
                                focusManager.moveFocus(FocusDirection.Previous)
                            } else {
                                focusManager.moveFocus(FocusDirection.Next)
                            }
                        }
                        false
                    },
                colors = colors,
                value = codes[index],
                onValueChange = { value ->
                    controller.onValueChange(index, value)
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
