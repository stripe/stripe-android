package com.stripe.android.uicore.elements

import android.view.KeyEvent
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.R
import com.stripe.android.uicore.stripeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This is focused on converting an [TextFieldController] into what is displayed in a section
 * with a single textField.
 * - some focus logic
 * - observes values that impact how things show on the screen
 */
@Composable
fun TextFieldSection(
    textFieldController: TextFieldController,
    imeAction: ImeAction,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    @StringRes sectionTitle: Int? = null,
    onTextStateChanged: (TextFieldState?) -> Unit = {}
) {
    val error by textFieldController.error.collectAsState(null)

    val sectionErrorString = error?.let {
        it.formatArgs?.let { args ->
            stringResource(
                it.errorMessage,
                *args
            )
        } ?: stringResource(it.errorMessage)
    }

    Section(sectionTitle, sectionErrorString) {
        TextField(
            textFieldController = textFieldController,
            enabled = enabled,
            imeAction = imeAction,
            modifier = modifier,
            onTextStateChanged = onTextStateChanged
        )
    }
}

/**
 * This is focused on converting an [TextFieldController] into what is displayed in a textField.
 * - some focus logic
 * - observes values that impact how things show on the screen
 *
 * @param enabled Whether to show this TextField as enabled or not. Note that the `enabled`
 * attribute of [textFieldController] is also taken into account to decide if the UI should be
 * enabled.
 */
@Composable
fun TextField(
    textFieldController: TextFieldController,
    enabled: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    onTextStateChanged: (TextFieldState?) -> Unit = {},
    nextFocusDirection: FocusDirection = FocusDirection.Next,
    previousFocusDirection: FocusDirection = FocusDirection.Previous
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
    val label by textFieldController.label.collectAsState(null)

    LaunchedEffect(fieldState) {
        // When field is in focus and full, move to next field so the user can keep typing
        if (fieldState == TextFieldStateConstants.Valid.Full && hasFocus) {
            focusManager.moveFocus(nextFocusDirection)
        }
    }

    TextField(
        value = value,
        onValueChange = { newValue ->
            val acceptInput = fieldState.canAcceptInput(value, newValue)

            if (acceptInput) {
                val newTextState = textFieldController.onValueChange(newValue)

                if (newTextState != null) {
                    onTextStateChanged(newTextState)
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL &&
                    value.isEmpty()
                ) {
                    focusManager.moveFocus(previousFocusDirection)
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
        enabled = enabled && textFieldController.enabled,
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
            {
                Row {
                    when (it) {
                        is TextFieldIcon.Trailing -> {
                            TrailingIcon(it, loading)
                        }
                        is TextFieldIcon.MultiTrailing -> {
                            Row(modifier = Modifier.padding(10.dp)) {
                                it.staticIcons.forEach {
                                    TrailingIcon(it, loading)
                                }
                                AnimatedIcons(icons = it.animatedIcons, loading = loading)
                            }
                        }
                    }
                }
            }
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
                focusManager.moveFocus(nextFocusDirection)
            },
            onDone = {
                focusManager.clearFocus(true)
            }
        ),
        singleLine = true,
        colors = colors
    )
}

@Composable
fun AnimatedIcons(
    icons: List<TextFieldIcon.Trailing>,
    loading: Boolean
) {
    if (icons.isEmpty()) return

    val composableScope = rememberCoroutineScope()

    val target by produceState(initialValue = icons.first()) {
        composableScope.launch {
            while (true) {
                icons.forEach {
                    delay(1000)
                    value = it
                }
            }
        }
    }

    Crossfade(targetState = target) {
        TrailingIcon(it, loading)
    }
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun TextFieldColors(
    shouldShowError: Boolean = false
) = TextFieldDefaults.textFieldColors(
    textColor = if (shouldShowError) {
        MaterialTheme.colors.error
    } else {
        MaterialTheme.stripeColors.onComponent
    },
    unfocusedLabelColor = MaterialTheme.stripeColors.placeholderText,
    focusedLabelColor = MaterialTheme.stripeColors.placeholderText,
    placeholderColor = MaterialTheme.stripeColors.placeholderText,
    backgroundColor = MaterialTheme.stripeColors.component,
    focusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.stripeColors.textCursor
)

@Composable
internal fun TrailingIcon(
    trailingIcon: TextFieldIcon.Trailing,
    loading: Boolean
) {
    if (loading) {
        CircularProgressIndicator()
    } else if (trailingIcon.isTintable) {
        Icon(
            painter = painterResource(id = trailingIcon.idRes),
            contentDescription = trailingIcon.contentDescription?.let {
                stringResource(trailingIcon.contentDescription)
            },
            modifier = Modifier.clickable {
                trailingIcon.onClick?.invoke()
            }
        )
    } else {
        Image(
            painter = painterResource(id = trailingIcon.idRes),
            contentDescription = trailingIcon.contentDescription?.let {
                stringResource(trailingIcon.contentDescription)
            },
            modifier = Modifier.clickable {
                trailingIcon.onClick?.invoke()
            }
        )
    }
}
