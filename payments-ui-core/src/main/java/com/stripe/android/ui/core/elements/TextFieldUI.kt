package com.stripe.android.ui.core.elements

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.R

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
    textFieldController: TextFieldController,
    modifier: Modifier = Modifier,
    enabled: Boolean,
) {
    Log.d("Construct", "SimpleTextFieldElement ${textFieldController.debugLabel}")

    val focusManager = LocalFocusManager.current
    val value by textFieldController.fieldValue.collectAsState("")
    val shouldShowError by textFieldController.visibleError.collectAsState(false)

    var hasFocus by rememberSaveable { mutableStateOf(false) }
    val colors = TextFieldDefaults.textFieldColors(
        textColor = if (shouldShowError) {
            PaymentsTheme.colors.material.error
        } else {
            PaymentsTheme.colors.onComponent
        },
        unfocusedLabelColor = PaymentsTheme.colors.placeholderText,
        focusedLabelColor = PaymentsTheme.colors.placeholderText,
        placeholderColor = PaymentsTheme.colors.placeholderText,
        backgroundColor = PaymentsTheme.colors.colorComponentBackground,
        focusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = PaymentsTheme.colors.colorTextCursor
    )

    TextField(
        value = value,
        onValueChange = { textFieldController.onValueChange(it) },
        isError = shouldShowError,
        label = {
            FormLabel(
                text = if (textFieldController.showOptionalLabel) {
                    stringResource(
                        R.string.form_label_optional,
                        stringResource(textFieldController.label)
                    )
                } else {
                    stringResource(textFieldController.label)
                },
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (hasFocus != it.isFocused) {
                    textFieldController.onFocusChange(it.isFocused)
                }
                hasFocus = it.isFocused
            },
        keyboardActions = KeyboardActions(
            onNext = {
                if (!focusManager.moveFocus(FocusDirection.Down)) {
                    focusManager.clearFocus(true)
                }
            }
        ),
        visualTransformation = textFieldController.visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = textFieldController.keyboardType,
            capitalization = textFieldController.capitalization,
            imeAction = ImeAction.Next
        ),
        colors = colors,
        maxLines = 1,
        singleLine = true,
        enabled = enabled
    )
}
