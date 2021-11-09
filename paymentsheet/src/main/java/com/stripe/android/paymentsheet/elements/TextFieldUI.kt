package com.stripe.android.paymentsheet.elements

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

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
    modifier: Modifier = Modifier,
    enabled: Boolean,
) {
    Log.d("Construct", "SimpleTextFieldElement ${textFieldController.debugLabel}")

    val focusManager = LocalFocusManager.current
    val value by textFieldController.fieldValue.collectAsState("")
    val trailingIcon by textFieldController.trailingIcon.collectAsState(null)
    val shouldShowError by textFieldController.visibleError.collectAsState(false)

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
    val fieldState by textFieldController.fieldState.collectAsState(
        TextFieldStateConstants.Error.Blank
    )
    val label by textFieldController.label.collectAsState(
        null
    )
    var processedIsFull by rememberSaveable { mutableStateOf(false) }

    /**
     * This is setup so that when a field is full it still allows more characters
     * to be entered, it just triggers next focus when the event happens.
     */
    @Suppress("UNUSED_VALUE")
    processedIsFull = if (fieldState == TextFieldStateConstants.Valid.Full) {
        if (!processedIsFull) {
            nextFocus(focusManager)
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
                text = if (textFieldController.showOptionalLabel) {
                    stringResource(
                        R.string.stripe_paymentsheet_form_label_optional,
                        label?.let { stringResource(it) } ?: ""
                    )
                } else {
                    label?.let { stringResource(it) } ?: ""
                }
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
                nextFocus(focusManager)
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
        enabled = enabled,
        trailingIcon = trailingIcon?.let {
            { TrailingIcon(it) }
        }
    )
}

internal fun nextFocus(focusManager: FocusManager) {
    if (!focusManager.moveFocus(FocusDirection.Right)) {
        if (!focusManager.moveFocus(FocusDirection.Down)) {
            focusManager.clearFocus(true)
        }
    }
}

@Composable
internal fun TrailingIcon(trailingIcon: TextFieldIcon) {
    Image(
        painter = painterResource(id = trailingIcon.idRes),
        modifier = Modifier.padding(0.dp),
        contentDescription = trailingIcon.contentDescription?.let {
            stringResource(trailingIcon.contentDescription)
        }
    )
}
