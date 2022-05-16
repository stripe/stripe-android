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

@Composable
fun NameFieldSection(
    value: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction,
    enabled: Boolean,
    onValueChanged: (String) -> Unit,
    onValidValue: (String?) -> Unit
) {
    TextField(
        value = value,
        config = SimpleTextFieldConfig.NAME,
        modifier = modifier,
        imeAction = imeAction,
        enabled = enabled,
        onValueChanged = onValueChanged,
        onValidValue = onValidValue
    )
}

@Composable
fun NameFieldSection(
    textFieldController: TextFieldController,
    modifier: Modifier = Modifier,
    imeAction: ImeAction,
    enabled: Boolean,
    onValueChanged: (String) -> Unit = {},
    onValidValue: (String?) -> Unit = {}
) {
    TextField(
        textFieldController = textFieldController,
        modifier = modifier,
        imeAction = imeAction,
        enabled = enabled,
        onValueChanged = onValueChanged,
        onValidValue = onValidValue
    )
}

@Composable
fun EmailFieldSection(
    textFieldController: TextFieldController,
    modifier: Modifier = Modifier,
    imeAction: ImeAction,
    enabled: Boolean,
    onValueChanged: (String) -> Unit = {},
    onValidValue: (String?) -> Unit = {}
) {
    TextField(
        textFieldController = textFieldController,
        modifier = modifier,
        imeAction = imeAction,
        enabled = enabled,
        onValueChanged = onValueChanged,
        onValidValue = onValidValue
    )
}

//@Composable
//fun TextField(
//    initialValue: String,
//    config: TextFieldConfig,
//    modifier: Modifier = Modifier,
//    imeAction: ImeAction,
//    enabled: Boolean,
//    onValueChanged: (String) -> Unit,
//    onValidValue: (String?) -> Unit
//) {
//    val controller = remember {
//        SimpleTextFieldController(
//            config,
//            initialValue = initialValue,
//            showOptionalLabel = false
//        )
//    }
//
//    val label by controller.label.collectAsState(null)
//    val error by controller.error.collectAsState(null)
//
//    val sectionErrorString = error?.let {
//        it.formatArgs?.let { args ->
//            stringResource(
//                it.errorMessage,
//                *args
//            )
//        } ?: stringResource(it.errorMessage)
//    }
//
//    Section(label, sectionErrorString) {
//        TextField(
//            textFieldController = controller,
//            enabled = enabled,
//            imeAction = imeAction,
//            modifier = modifier,
//            onValueChanged = onValueChanged,
//            onValidValue = onValidValue
//        )
//    }
//}

/**
 * This is focused on converting an `Element` into what is displayed in a textField.
 * - some focus logic
 * - observes values that impact how things show on the screen
 * - calls through to the Elements worker functions for focus change and value change events
 */
@Composable
fun TextField(
    textFieldController: TextFieldController,
    modifier: Modifier = Modifier,
    imeAction: ImeAction,
    enabled: Boolean,
    onValueChanged: (String) -> Unit = {},
    onValidValue: (String?) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val value by textFieldController.fieldValue.collectAsState("")
    val trailingIcon by textFieldController.trailingIcon.collectAsState(null)
    val shouldShowError by textFieldController.visibleError.collectAsState(false)
    val loading by textFieldController.loading.collectAsState(false)
    val contentDescription by textFieldController.contentDescription.collectAsState("")

    var hasFocus by rememberSaveable { mutableStateOf(false) }
    val colors = TextFieldColors(shouldShowError)
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
        onValueChange = {
            val validValue = textFieldController.onValueChange(it)
            onValueChanged(it)
            onValidValue(validValue)
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
        enabled = enabled,
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
        trailingIcon = trailingIcon?.let {
            { TrailingIcon(it, colors, loading) }
        },
        isError = shouldShowError,
        visualTransformation = textFieldController.visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = textFieldController.keyboardType,
            capitalization = textFieldController.capitalization,
            imeAction = imeAction
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
        colors = colors,
    )
}

@Composable
internal fun TextFieldColors(
    shouldShowError: Boolean = false
) = TextFieldDefaults.textFieldColors(
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
