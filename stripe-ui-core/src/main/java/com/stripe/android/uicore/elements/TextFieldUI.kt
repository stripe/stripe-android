package com.stripe.android.uicore.elements

import android.view.KeyEvent
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.stripe.android.core.Logger
import com.stripe.android.uicore.BuildConfig
import com.stripe.android.uicore.LocalInstrumentationTest
import com.stripe.android.uicore.R
import com.stripe.android.uicore.stripeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalAutofillEventReporter = staticCompositionLocalOf(::defaultAutofillEventReporter)

private fun defaultAutofillEventReporter(): (String) -> Unit {
    return { autofillType ->
        Logger.getInstance(BuildConfig.DEBUG)
            .debug("LocalAutofillEventReporter $autofillType event not reported")
    }
}

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
@OptIn(ExperimentalComposeUiApi::class)
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
    val placeHolder by textFieldController.placeHolder.collectAsState(null)

    var hasFocus by rememberSaveable { mutableStateOf(false) }

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

    val autofillReporter = LocalAutofillEventReporter.current

    val autofillNode = remember {
        AutofillNode(
            autofillTypes = listOfNotNull(textFieldController.autofillType),
            onFill = {
                textFieldController.autofillType?.let { type ->
                    autofillReporter(type.name)
                }
                textFieldController.onValueChange(it)
            }
        )
    }
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current

    LaunchedEffect(autofillNode) {
        autofillTree += autofillNode
    }

    TextFieldUi(
        value = value,
        loading = loading,
        onValueChange = { newValue ->
            val acceptInput = fieldState.canAcceptInput(value, newValue)

            if (acceptInput) {
                val newTextState = textFieldController.onValueChange(newValue)

                if (newTextState != null) {
                    onTextStateChanged(newTextState)
                }
            }
        },
        onDropdownItemClicked = textFieldController::onDropdownItemClicked,
        modifier = modifier
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
            .onGloballyPositioned {
                autofillNode.boundingBox = it.boundsInWindow()
            }
            .onFocusChanged {
                if (hasFocus != it.isFocused) {
                    textFieldController.onFocusChange(it.isFocused)
                }
                hasFocus = it.isFocused

                if (autofill != null && autofillNode.boundingBox != null) {
                    if (it.isFocused) {
                        autofill.requestAutofillForNode(autofillNode)
                    } else {
                        autofill.cancelAutofillForNode(autofillNode)
                    }
                }
            }
            .semantics {
                this.contentDescription = contentDescription
                this.editableText = AnnotatedString("")
            },
        enabled = enabled && textFieldController.enabled,
        label = label?.let {
            stringResource(it)
        },
        showOptionalLabel = textFieldController.showOptionalLabel,
        placeholder = placeHolder,
        trailingIcon = trailingIcon,
        shouldShowError = shouldShowError,
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
        )
    )
}

@Composable
internal fun TextFieldUi(
    value: String,
    enabled: Boolean,
    loading: Boolean,
    label: String?,
    placeholder: String?,
    trailingIcon: TextFieldIcon?,
    showOptionalLabel: Boolean,
    shouldShowError: Boolean,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (value: String) -> Unit = {},
    onDropdownItemClicked: (item: TextFieldIcon.Dropdown.Item) -> Unit = {}
) {
    val colors = TextFieldColors(shouldShowError)

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = label?.let {
            {
                FormLabel(
                    text = if (showOptionalLabel) {
                        stringResource(
                            R.string.stripe_form_label_optional,
                            it
                        )
                    } else {
                        it
                    }
                )
            }
        },
        placeholder = placeholder?.let {
            { Placeholder(text = it) }
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
                        is TextFieldIcon.Dropdown -> {
                            TrailingDropdown(
                                icon = it,
                                loading = loading,
                                onDropdownItemClicked = onDropdownItemClicked
                            )
                        }
                    }
                }
            }
        },
        isError = shouldShowError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
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

    val isRunningInTestHarness = LocalInstrumentationTest.current

    val target by produceState(initialValue = icons.first()) {
        if (!isRunningInTestHarness) {
            composableScope.launch {
                while (true) {
                    icons.forEach {
                        delay(1000)
                        value = it
                    }
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
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = modifier
                .progressSemantics()
                .height(LOADING_INDICATOR_SIZE.dp)
        )
    } else if (trailingIcon.isTintable) {
        Icon(
            painter = painterResource(id = trailingIcon.idRes),
            contentDescription = trailingIcon.contentDescription?.let {
                stringResource(trailingIcon.contentDescription)
            },
            modifier = modifier.conditionallyClickable(trailingIcon.onClick),
        )
    } else {
        Image(
            painter = painterResource(id = trailingIcon.idRes),
            contentDescription = trailingIcon.contentDescription?.let {
                stringResource(trailingIcon.contentDescription)
            },
            modifier = modifier.conditionallyClickable(trailingIcon.onClick),
        )
    }
}

@Composable
private fun TrailingDropdown(
    icon: TextFieldIcon.Dropdown,
    loading: Boolean,
    onDropdownItemClicked: (item: TextFieldIcon.Dropdown.Item) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val show = !loading && !icon.hide

    Box(
        modifier = Modifier
            .focusProperties { canFocus = false }
            .clickable(enabled = show) { expanded = true }
            .testTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TrailingIcon(
                TextFieldIcon.Trailing(
                    icon.currentItem.icon,
                    isTintable = false
                ),
                loading
            )

            if (show) {
                CompositionLocalProvider(
                    LocalContentColor
                        provides
                            MaterialTheme.stripeColors.placeholderText
                ) {
                    TrailingIcon(
                        trailingIcon = TextFieldIcon.Trailing(
                            R.drawable.stripe_ic_chevron_down,
                            isTintable = true
                        ),
                        loading = false,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }

        SingleChoiceDropdown(
            expanded = expanded,
            title = icon.title,
            currentChoice = icon.currentItem,
            choices = icon.items,
            headerTextColor = MaterialTheme.stripeColors.subtitle,
            optionTextColor = MaterialTheme.stripeColors.onComponent,
            onChoiceSelected = { item ->
                onDropdownItemClicked(item)

                expanded = false
            },
            onDismiss = {
                expanded = false
            }
        )
    }
}

private fun Modifier.conditionallyClickable(onClick: (() -> Unit)?): Modifier {
    return if (onClick != null) {
        clickable { onClick() }
    } else {
        this
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DROPDOWN_MENU_CLICKABLE_TEST_TAG = "dropdown_menu_clickable"

// Default size of Material Theme icons
private const val LOADING_INDICATOR_SIZE = 24
