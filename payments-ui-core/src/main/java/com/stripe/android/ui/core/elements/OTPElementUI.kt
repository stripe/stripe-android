package com.stripe.android.ui.core.elements

import android.view.KeyEvent
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun OTPElementUI(
    element: OTPElement,
    colors: TextFieldColors? = null
) {
    val width = LocalConfiguration.current.screenWidthDp.dp / (element.controller.otpLength + 2)
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (0 until element.controller.otpLength).map { index ->
            OTPCell(
                modifier = Modifier
                    .width(width)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp) {
                            if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL) {
                                element.controller.onDelete(index)
                                focusManager.moveFocus(FocusDirection.Previous)
                            } else {
                                focusManager.moveFocus(FocusDirection.Next)
                            }
                        }
                        false
                    },
                colors = colors,
                textFieldController = element.controller.textFieldControllers[index],
            )
        }
    }
}

@Composable
private fun OTPCell(
    modifier: Modifier,
    colors: TextFieldColors?,
    textFieldController: TextFieldController,
) {
    val placeholder = remember { mutableStateOf("●") }
    SectionCard {
        TextField(
            textFieldController = textFieldController,
            modifier = modifier.onFocusChanged {
                placeholder.value = if (!it.hasFocus) "●" else ""
            },
            placeholder = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = placeholder.value,
                    textAlign = TextAlign.Center
                )
            },
            colors = colors,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            enabled = true
        )
    }
}
