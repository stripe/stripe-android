package com.stripe.android.uicore.elements

import android.annotation.SuppressLint
import android.view.KeyEvent
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BasicTooltipDefaults
import androidx.compose.foundation.BasicTooltipState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.onAutofillText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.BuildConfig
import com.stripe.android.uicore.LocalInstrumentationTest
import com.stripe.android.uicore.LocalTextFieldInsets
import com.stripe.android.uicore.R
import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.compat.CompatTextField
import com.stripe.android.uicore.moveFocusSafely
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.forEach

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
    modifier: Modifier = Modifier,
    textFieldController: TextFieldController,
    isSelected: Boolean = false,
    @StringRes sectionTitle: Int? = null,
    content: @Composable () -> Unit,
) {
    val validationMessage by textFieldController.validationMessage.collectAsState()

    Section(
        modifier = modifier,
        title = sectionTitle?.let { resolvableString(it) },
        validationMessage = validationMessage,
        isSelected = isSelected,
        content = content,
    )
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
@Suppress("LongMethod")
fun TextField(
    textFieldController: TextFieldController,
    enabled: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    onTextStateChanged: (TextFieldState?) -> Unit = {},
    nextFocusDirection: FocusDirection = FocusDirection.Next,
    previousFocusDirection: FocusDirection = FocusDirection.Previous,
    focusRequester: FocusRequester = remember { FocusRequester() },
    shouldAnnounceLabel: Boolean = true,
    shouldAnnounceFieldValue: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    val value by textFieldController.fieldValue.collectAsState()
    val trailingIcon by textFieldController.trailingIcon.collectAsState()
    val shouldShowValidationMessage by textFieldController.visibleValidationMessage.collectAsState()
    val loading by textFieldController.loading.collectAsState()
    val contentDescription by textFieldController.contentDescription.collectAsState()
    val visualTransformation by textFieldController.visualTransformation.collectAsState()
    val placeHolder by textFieldController.placeHolder.collectAsState()

    val hasFocus = rememberSaveable { mutableStateOf(false) }

    val fieldState by textFieldController.fieldState.collectAsState()
    val label by textFieldController.label.collectAsState()

    val error by textFieldController.validationMessage.collectAsState()

    LaunchedEffect(fieldState) {
        // When field is in focus and full, move to next field so the user can keep typing
        if (fieldState is TextFieldStateConstants.Valid.Full && hasFocus.value) {
            focusManager.moveFocusSafely(nextFocusDirection)
        }
    }

    val autofillReporter = LocalAutofillEventReporter.current

    var selection by remember {
        mutableStateOf<TextRange?>(null)
    }

    var composition by remember {
        mutableStateOf<TextRange?>(null)
    }

    val context = LocalContext.current

    // here
    TextFieldUi(
        value = TextFieldValue(
            text = value,
            selection = selection ?: TextRange(value.length),
            composition = composition
        ),
        loading = loading,
        onValueChange = { newValue ->
            val newTextValue = newValue.text
            val acceptInput = fieldState.canAcceptInput(value, newTextValue)

            if (newTextValue == value || acceptInput) {
                selection = newValue.selection
                composition = newValue.composition
            }

            if (acceptInput) {
                val newTextState = textFieldController.onValueChange(newTextValue)

                if (newTextState != null) {
                    onTextStateChanged(newTextState)
                }
            }
        },
        onDropdownItemClicked = textFieldController::onDropdownItemClicked,
        modifier = modifier
            .onPreviewKeyEvent(
                value = value,
                focusManager = focusManager,
                direction = previousFocusDirection
            )
            .onAutofill(
                textFieldController = textFieldController,
                autofillReporter = autofillReporter,
            )
            .onFocusChanged(
                textFieldController = textFieldController,
                hasFocus = hasFocus,
            )
            .focusRequester(focusRequester)
            .semantics {
                this.contentDescription = contentDescription.resolve(context)
                if (!shouldAnnounceFieldValue) this.editableText = AnnotatedString("")
            },
        enabled = enabled && textFieldController.enabled,
        label = label.resolve(),
        showOptionalLabel = textFieldController.showOptionalLabel,
        shouldAnnounceLabel = shouldAnnounceLabel,
        placeholder = placeHolder,
        trailingIcon = trailingIcon,
        shouldShowValidationMessage = shouldShowValidationMessage,
        validationMessage = error,
        visualTransformation = visualTransformation,
        layoutDirection = textFieldController.layoutDirection,
        keyboardOptions = KeyboardOptions(
            keyboardType = textFieldController.keyboardType,
            capitalization = textFieldController.capitalization,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocusSafely(nextFocusDirection)
            },
            onDone = {
                focusManager.clearFocus(true)
            }
        ),
    )
}

// here
@Composable
internal fun TextFieldUi(
    value: TextFieldValue,
    enabled: Boolean,
    loading: Boolean,
    label: String,
    placeholder: String?,
    trailingIcon: TextFieldIcon?,
    showOptionalLabel: Boolean,
    shouldShowValidationMessage: Boolean,
    validationMessage: FieldValidationMessage?,
    shouldAnnounceLabel: Boolean = true,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    layoutDirection: LayoutDirection? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (value: TextFieldValue) -> Unit = {},
    // update this
    onDropdownItemClicked: (item: TextFieldIcon.Selector.Item) -> Unit = {}
) {
    val displayState = when (shouldShowValidationMessage) {
        true -> {
            when (validationMessage) {
                is FieldValidationMessage.Error, null -> FieldDisplayState.ERROR
                is FieldValidationMessage.Warning -> FieldDisplayState.WARNING
            }
        }
        false -> FieldDisplayState.NORMAL
    }
    val colors = TextFieldColors(displayState)
    val textFieldInsets = LocalTextFieldInsets.current

    val layoutDirectionToUse = layoutDirection ?: LocalLayoutDirection.current

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionToUse) {
        CompatTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            label = {
                FormLabel(
                    text = if (showOptionalLabel) {
                        stringResource(
                            R.string.stripe_form_label_optional,
                            label,
                        )
                    } else {
                        label
                    },
                    modifier = if (shouldAnnounceLabel) Modifier else Modifier.clearAndSetSemantics {}
                )
            },
            placeholder = placeholder?.let {
                {
                    Placeholder(text = it)
                }
            },
            trailingIcon = trailingIcon?.let { icon ->
                {
                    // here
                    icon.Composable(loading, onDropdownItemClicked)
                }
            },
            isError = shouldShowValidationMessage,
            errorMessage = validationMessage?.resolvable?.resolve(),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            colors = colors,
            contentPadding = textFieldInsets.asPaddingValues(),
        )
    }
}

// here
@Composable
private fun TextFieldIcon.Composable(
    loading: Boolean,
    onDropdownItemClicked: (item: TextFieldIcon.Selector.Item) -> Unit,
) {
    Row {
        when (this@Composable) {
            is TextFieldIcon.Trailing -> {
                TrailingIcon(this@Composable, loading)
            }

            is TextFieldIcon.MultiTrailing -> {
                Row(modifier = Modifier.padding(10.dp)) {
                    staticIcons.forEach {
                        TrailingIcon(it, loading)
                    }
                    AnimatedIcons(icons = animatedIcons, loading = loading)
                }
            }

            // here
            is TextFieldIcon.Dropdown -> {
//                TrailingDropdown(
//                    icon = this@Composable,
//                    loading = loading,
//                    onDropdownItemClicked = onDropdownItemClicked
//                )
            }
            is TextFieldIcon.Selector -> {
                CardBrandSelector(
                    icon = this@Composable,
                    loading = loading,
                    onDropdownItemClicked = onDropdownItemClicked
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CardBrandSelector(
    icon: TextFieldIcon.Selector,
    loading: Boolean,
    onDropdownItemClicked: (item: TextFieldIcon.Selector.Item) -> Unit
) {
    var hasShownTooltip by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .semantics {
                this.contentDescription = icon.title.resolve(context)
            }
            .testTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tooltipState = remember { BasicTooltipState(isPersistent = false) }
            val density = LocalDensity.current
            val positionProvider = DropdownMenuPositionProvider(
                contentOffset = DpOffset(0.dp, 0.dp),
                density = density,
            )
            BasicTooltipBox(
                positionProvider = positionProvider,
                tooltip = {
                    Box(Modifier.background(color = MaterialTheme.stripeColors.component)) {
                        Text("Choose a card brand")
                    }
                },
                state = tooltipState,
            ) {
                Row {
                    icon.items.forEach {
                        LaunchedEffect(icon.items) {
                            if (!hasShownTooltip) {
                                hasShownTooltip = true
                                tooltipState.show()
                            }
                        }
                        if (it == icon.currentItem) {
                            Icon(
                                painter = painterResource(R.drawable.stripe_ic_checkmark),
                                contentDescription = null
                            )
                        }
                        Image(
                            modifier = Modifier
                                .clickable(
                                    enabled = true,
                                    onClick = {
                                        onDropdownItemClicked(it)
                                    }
                                ),
                            painter = painterResource(id = it.icon),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

internal val MenuVerticalMargin = 48.dp
internal data class DropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(
                toRight,
                toLeft,
                // If the anchor gets outside of the window on the left, we want to position
                // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
                if (anchorBounds.left >= 0) toDisplayRight else toDisplayLeft
            )
        } else {
            sequenceOf(
                toLeft,
                toRight,
                // If the anchor gets outside of the window on the right, we want to position
                // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
                if (anchorBounds.right <= windowSize.width) toDisplayLeft else toDisplayRight
            )
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        // Compute vertical position.
        val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
        val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
        val toCenter = anchorBounds.top - popupContentSize.height / 2
        val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
        val y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
            it >= verticalMargin &&
                it + popupContentSize.height <= windowSize.height - verticalMargin
        } ?: toTop

        onPositionCalculated(
            anchorBounds,
            IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
        )
        return IntOffset(x, y)
    }
}
@Composable
fun AnimatedIcons(
    icons: List<TextFieldIcon.Trailing>,
    loading: Boolean
) {
    if (icons.isEmpty()) return

    val composableScope = rememberCoroutineScope()

    val isRunningInTestHarness = LocalInstrumentationTest.current

    @SuppressLint("ProduceStateDoesNotAssignValue")
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
    fieldDisplayState: FieldDisplayState = FieldDisplayState.NORMAL,
    textColor: Color = MaterialTheme.stripeColors.onComponent,
    disabledTextColor: Color = textColor.copy(ContentAlpha.disabled),
    backgroundColor: Color = MaterialTheme.stripeColors.component,
    disabledIndicatorColor: Color = Color.Transparent,
) = TextFieldDefaults.textFieldColors(
    textColor = when (fieldDisplayState) {
        FieldDisplayState.ERROR -> MaterialTheme.colors.error
        FieldDisplayState.NORMAL, FieldDisplayState.WARNING -> textColor
    },
    disabledTextColor = disabledTextColor,
    unfocusedLabelColor = MaterialTheme.stripeColors.placeholderText,
    focusedLabelColor = MaterialTheme.stripeColors.placeholderText,
    placeholderColor = MaterialTheme.stripeColors.placeholderText,
    backgroundColor = backgroundColor,
    focusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = disabledIndicatorColor,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.stripeColors.textCursor,
    errorCursorColor = when (fieldDisplayState) {
        FieldDisplayState.ERROR -> MaterialTheme.colors.error
        FieldDisplayState.NORMAL, FieldDisplayState.WARNING -> MaterialTheme.stripeColors.textCursor
    },
    errorIndicatorColor = when (fieldDisplayState) {
        FieldDisplayState.ERROR -> MaterialTheme.colors.error
        FieldDisplayState.NORMAL, FieldDisplayState.WARNING -> Color.Transparent
    },
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun TrailingIcon(
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
                    LocalContentColor provides MaterialTheme.stripeColors.placeholderText
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

private fun Modifier.onPreviewKeyEvent(
    value: String,
    focusManager: FocusManager,
    direction: FocusDirection,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown &&
        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL &&
        value.isEmpty()
    ) {
        focusManager.moveFocusSafely(direction)
        true
    } else {
        false
    }
}

@Composable
private fun Modifier.onAutofill(
    textFieldController: TextFieldController,
    autofillReporter: (String) -> Unit
): Modifier = semantics {
    textFieldController.autofillType?.let {
        contentType = it
    }

    onAutofillText {
        textFieldController.autofillType?.let { type ->
            autofillReporter(type.toString())
        }

        textFieldController.onValueChange(it.text)

        true
    }
}

private fun Modifier.onFocusChanged(
    textFieldController: TextFieldController,
    hasFocus: MutableState<Boolean>
): Modifier = onFocusChanged {
    if (hasFocus.value != it.isFocused) {
        textFieldController.onFocusChange(it.isFocused)
    }
    hasFocus.value = it.isFocused
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class FieldDisplayState {
    NORMAL, ERROR, WARNING
}
