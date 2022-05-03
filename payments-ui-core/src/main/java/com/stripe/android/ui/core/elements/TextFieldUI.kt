package com.stripe.android.ui.core.elements

import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.R

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
    imeAction: ImeAction,
    enabled: Boolean
) {
    val focusManager = LocalFocusManager.current
    val value by textFieldController.fieldValue.collectAsState("")
    val trailingIcon by textFieldController.trailingIcon.collectAsState(null)
    val shouldShowError by textFieldController.visibleError.collectAsState(false)
    val loading by textFieldController.loading.collectAsState(false)
    val contentDescription by textFieldController.contentDescription.collectAsState("")

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
        backgroundColor = PaymentsTheme.colors.component,
        focusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = PaymentsTheme.colors.colorTextCursor
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
            focusManager.moveFocus(FocusDirection.Next)
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
            FormLabel(
                text = if (textFieldController.showOptionalLabel) {
                    stringResource(
                        R.string.form_label_optional,
                        label?.let { stringResource(it) } ?: ""
                    )
                } else {
                    label?.let { stringResource(it) } ?: ""
                }
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL &&
                    value.isEmpty()
                ) {
                    focusManager.moveFocus(FocusDirection.Previous)
                    true
                } else {
                    false
                }
            }
            .onFocusChanged {
                if (hasFocus != it.isFocused) {
                    textFieldController.onFocusChange(it.isFocused)
                }
                hasFocus = it.isFocused
            }
            .semantics {
                this.contentDescription = contentDescription
                this.editableText = AnnotatedString("")
            },
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Next)
            },
            onDone = {
                focusManager.clearFocus(true)
            }
        ),
        visualTransformation = textFieldController.visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = textFieldController.keyboardType,
            capitalization = textFieldController.capitalization,
            imeAction = imeAction
        ),
        colors = colors,
        maxLines = 1,
        singleLine = true,
        enabled = enabled,
        trailingIcon = trailingIcon?.let {
            { TrailingIcon(it, colors, loading) }
        }
    )
}

@Composable
internal fun TrailingIcon(
    trailingIcon: TextFieldIcon,
    colors: androidx.compose.material.TextFieldColors,
    loading: Boolean
) {
    if (loading) {
        CircularProgressIndicator()
    } else if (trailingIcon.isIcon) {
        Icon(
            painter = painterResource(id = trailingIcon.idRes),
            contentDescription = trailingIcon.contentDescription?.let {
                stringResource(trailingIcon.contentDescription)
            }
        )
    } else {
        Image(
            painter = painterResource(id = trailingIcon.idRes),
            contentDescription = trailingIcon.contentDescription?.let {
                stringResource(trailingIcon.contentDescription)
            }
        )
    }
}
