package com.stripe.android.paymentsheet.elements

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.asLiveData
import com.stripe.android.paymentsheet.R

/** This is a helpful method for setting the next action based on the nextFocus Requester **/
internal fun imeAction(nextFocusRequester: FocusRequester?): ImeAction = nextFocusRequester?.let {
    ImeAction.Next
} ?: ImeAction.Done

internal data class TextFieldColors(
    private val isDarkMode: Boolean,
    private val defaultTextColor: Color,
    val textColor: Color = if (isDarkMode) {
        Color.White
    } else {
        defaultTextColor
    },
    val placeholderColor: Color = Color(0x14000000),
    val backgroundColor: Color = Color.Transparent,
    val focusedIndicatorColor: Color = Color.Transparent, // primary color by default
    val unfocusedIndicatorColor: Color = Color.Transparent,
    val disabledIndicatorColor: Color = Color.Transparent
)

/**
 * This is focused on converting an `Element` into what is displayed in a textField.
 * - some focus logic
 * - observes values that impact how things show on the screen
 * - calls through to the Elements worker functions for focus change and value change events
 */
@Composable
internal fun TextField(
    textFieldController: TextFieldController,
    myFocus: FocusRequester,
    nextFocus: FocusRequester?,
    modifier: Modifier = Modifier,
    enabled: Boolean,
) {
    Log.d("Construct", "SimpleTextFieldElement ${textFieldController.debugLabel}")

    val value by textFieldController.fieldValue.asLiveData().observeAsState("")
    val shouldShowError by textFieldController.visibleError.asLiveData().observeAsState(false)
    val fieldIsFull by textFieldController.isFull.asLiveData().observeAsState(false)
    var processedIsFull by rememberSaveable { mutableStateOf(false) }

    var hasFocus by rememberSaveable { mutableStateOf(false) }
    val textFieldColors = TextFieldColors(
        isSystemInDarkTheme(),
        LocalContentColor.current.copy(LocalContentAlpha.current)
    )
    val colors = TextFieldDefaults.textFieldColors(
        textColor = if (shouldShowError) {
            MaterialTheme.colors.error
        } else {
            textFieldColors.textColor
        },
        placeholderColor = textFieldColors.placeholderColor,
        backgroundColor = textFieldColors.backgroundColor,
        focusedIndicatorColor = textFieldColors.focusedIndicatorColor,
        disabledIndicatorColor = textFieldColors.disabledIndicatorColor,
        unfocusedIndicatorColor = textFieldColors.unfocusedIndicatorColor
    )

    // This is setup so that when a field is full it still allows more characters
    // to be entered, it just triggers next focus when the event happens.
    @Suppress("UNUSED_VALUE")
    processedIsFull = if (fieldIsFull) {
        if (!processedIsFull) {
            nextFocus?.requestFocus()
        }
        true
    } else {
        false
    }

    TextField(
        value = value,
        onValueChange = { textFieldController.onValueChange(it) },
        isError = shouldShowError,
        label = {
            Text(
                text = if (textFieldController.isRequired) {
                    stringResource(textFieldController.label)
                } else {
                    stringResource(textFieldController.label) +
                        " " + stringResource(R.string.address_label_optional_field)
                }
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .focusOrder(myFocus) { nextFocus?.requestFocus() }
            .onFocusChanged {
                if (hasFocus != it.isFocused) {
                    textFieldController.onFocusChange(it.isFocused)
                }
                hasFocus = it.isFocused
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = textFieldController.keyboardType,
            capitalization = textFieldController.capitalization,
            imeAction = imeAction(nextFocus)
        ),
        colors = colors,
        maxLines = 1,
        singleLine = true,
        enabled = enabled
    )
}
