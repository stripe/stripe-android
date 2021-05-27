package com.stripe.android.paymentsheet.elements.common

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction

/** This is a helpful method for setting the next action based on the nextFocus Requester **/
internal fun imeAction(nextFocusRequester: FocusRequester?): ImeAction = nextFocusRequester?.let {
    ImeAction.Next
} ?: ImeAction.Done

/**
 * This is focused on converting an `Element` into what is displayed in a textField.
 * - some focus logic
 * - observes values that impact how things show on the screen
 * - calls through to the Elements worker functions for focus change and value change events
 */
@Composable
internal fun TextField(
    textFieldElement: TextFieldElement,
    myFocus: FocusRequester,
    nextFocus: FocusRequester?,
    modifier: Modifier = Modifier,
    label: Int? = null, // Let the caller choose if they want a label, if it is in a section by itself it might not make sense.
) {
    Log.d("Construct", "SimpleTextFieldElement ${textFieldElement.debugLabel}")

    val value by textFieldElement.input.observeAsState("")
    val shouldShowError by textFieldElement.visibleError.observeAsState(false)
    val elementIsFull by textFieldElement.isFull.observeAsState(false)
    var processedIsFull by rememberSaveable { mutableStateOf(false) }

    var hasFocus by rememberSaveable { mutableStateOf(false) }
    val colors = TextFieldDefaults.textFieldColors(
        textColor = if (shouldShowError) {
            MaterialTheme.colors.error
        } else {
            LocalContentColor.current.copy(LocalContentAlpha.current)
        }
    )

    // This is setup so that when a field is full it still allows more characters
    // to be entered, it just triggers next focus when the event happens.
    @Suppress("UNUSED_VALUE")
    processedIsFull = if (elementIsFull) {
        if (!processedIsFull) {
            nextFocus?.requestFocus()
        }
        true
    } else {
        false
    }

    TextField(
        value = value,
        onValueChange = { textFieldElement.onValueChange(it) },
        isError = shouldShowError,
        label = { label?.let { Text(text = stringResource(it)) } },
        modifier = modifier
            .fillMaxWidth()
            .focusOrder(myFocus) { nextFocus?.requestFocus() }
            .onFocusChanged {
                if (hasFocus != it.isFocused) {
                    textFieldElement.onFocusChange(it.isFocused)
                }
                hasFocus = it.isFocused
            },
        keyboardOptions = KeyboardOptions(imeAction = imeAction(nextFocus)),
        colors = colors
    )
}
