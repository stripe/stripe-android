package com.stripe.android.compose.elements

import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation

/** This is a helpful method for setting the next action based on the nextFocus Requester **/
fun imeAction(nextFocusRequester: FocusRequester?): ImeAction = nextFocusRequester?.let {
    ImeAction.Next
} ?: ImeAction.Done

@Composable
fun TextFieldLabel(
    intRes: Int?
) {
    if (intRes != null) {
        Text(
            text = stringResource(intRes)
        )
    }
}

/**
 * This is focused on converting an `Element` into what is displayed in a textField.
 * - some focus logic
 * - observes values that impact how things show on the screen
 * - calls through to the Elements worker functions for focus change and value change events
 */
@Composable
fun SimpleTextFieldElement(
    element: Element,
    myFocus: FocusRequester,
    nextFocus: FocusRequester?,
    modifier: Modifier = Modifier,
    isBorder: Boolean = false, // If you want an element outside a section it might need a border
    label: Int? = null, // Let the caller choose if they want a label, if it is in a section by itself it might not make sense.
    textStyle: TextStyle = LocalTextStyle.current,
) {
    Log.d("Construct", "SimpleTextFieldElement ${element.debugLabel}")
    val value by element.input.observeAsState("")
    val shouldShowError by element.visibleError.observeAsState(false)

    val elementIsFull by element.isFull.observeAsState(false)
    var processedIsFull by rememberSaveable { mutableStateOf(false) }

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


    SimpleTextField(
        debugLabel = element.debugLabel,
        isBorder = isBorder,
        value = value,
        shouldShowError = shouldShowError,
        onValueChange = { element.onValueChange(it) },
        onFocusChange = { element.onFocusChange(it) },
        label = label,
        modifier = modifier.focusOrder(myFocus) { nextFocus?.requestFocus() },
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(imeAction = imeAction(nextFocus)),
        singleLine = true,
        maxLines = 1,
    )
}

/**
 * This is a very configurable text view,
 * TODO: Combine this with the view below.
 */
@Composable
fun SimpleTextField(
    value: String,
    modifier: Modifier = Modifier,
    debugLabel: String = "unknown",
    onValueChange: (String) -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
    label: Int? = null,
    isBorder: Boolean,
    shouldShowError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small.copy(
        topEnd = ZeroCornerSize,
        topStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
        bottomStart = ZeroCornerSize
    ),
) {
    var hasFocus by rememberSaveable { mutableStateOf(false) }
    Log.d("Construct", "SimpleTextField $debugLabel")

    OptionalBorderTextField(
        debugLabel = debugLabel,
        value = value,
        onValueChange = {
            onValueChange(it)
        },
        isBorder = isBorder,
        isError = shouldShowError,
        label = label,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (hasFocus != it.isFocused) {
                    onFocusChange(it.isFocused)
                }
                hasFocus = it.isFocused
            },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        interactionSource = interactionSource,
        shape = shape
    )
}

@Composable
fun OptionalBorderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isBorder: Boolean,
    modifier: Modifier = Modifier,
    debugLabel: String = "unknown",
    isError: Boolean = false,
    label: Int? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small.copy(
        topEnd = ZeroCornerSize,
        topStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
        bottomStart = ZeroCornerSize
    )
) {
    // AnimatedVisibility(visible = autoCompleteState.isSearching) {
    Log.d("Construct", "OptionalBorderTextField $debugLabel")
    if (isBorder) {
        CustomOutlinedTextField(
            value = value,
            modifier = modifier,
            onValueChange = onValueChange,
            label = { TextFieldLabel(label) },
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            interactionSource = interactionSource,
        )
    } else {
        CustomTextFieldNoOutline(
            debugLabel = debugLabel,
            value = value,
            modifier = modifier,
            onValueChange = onValueChange,
            label = { TextFieldLabel(label) },
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            interactionSource = interactionSource,
            shape = shape,
        )
    }

}

@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    debugLabel: String = "unknown",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Log.d("Construct", "CustomOutlinedTextField $debugLabel")
    val colors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = if (isError) {
            MaterialTheme.colors.error
        } else {
            LocalContentColor.current.copy(LocalContentAlpha.current)
        }
    )

    OutlinedTextField(
        value,
        onValueChange,
        modifier,
        enabled,
        readOnly,
        textStyle,
        label,
        placeholder,
        leadingIcon,
        trailingIcon,
        isError = false,
        visualTransformation,
        keyboardOptions,
        keyboardActions,
        singleLine,
        maxLines,
        interactionSource,
        colors,
    )
}


@Composable
fun CustomTextFieldNoOutline(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    debugLabel: String = "unknown",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small.copy(
        topEnd = ZeroCornerSize,
        topStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
        bottomStart = ZeroCornerSize
    ),
) {
    Log.d("Construct", "CustomTextFieldNoOutline $debugLabel")
    val colors = TextFieldDefaults.textFieldColors(
        textColor = if (isError) {
            MaterialTheme.colors.error
        } else {
            LocalContentColor.current.copy(LocalContentAlpha.current)
        }
    )

    TextField(
        value,
        onValueChange,
        modifier,
        enabled,
        readOnly,
        textStyle,
        label,
        placeholder,
        leadingIcon,
        trailingIcon,
        isError,
        visualTransformation,
        keyboardOptions,
        keyboardActions,
        singleLine,
        maxLines,
        interactionSource,
        shape,
        colors
    )
}