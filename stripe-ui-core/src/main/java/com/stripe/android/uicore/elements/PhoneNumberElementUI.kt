@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.R
import com.stripe.android.uicore.moveFocusSafely
import com.stripe.android.uicore.text.autofill
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val PHONE_NUMBER_TEXT_FIELD_TAG = "PhoneNumberTextField"

@Preview
@Composable
private fun PhoneNumberCollectionPreview() {
    PhoneNumberCollectionSection(
        enabled = true,
        phoneNumberController = PhoneNumberController.createPhoneNumberController("6508989787")
    )
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PhoneNumberCollectionSection(
    enabled: Boolean,
    phoneNumberController: PhoneNumberController,
    modifier: Modifier = Modifier,
    countryDropdown: @Composable () -> Unit = { CountryDropdown(phoneNumberController, enabled) },
    isSelected: Boolean = false,
    @StringRes sectionTitle: Int? = null,
    requestFocusWhenShown: Boolean = false,
    moveToNextFieldOnceComplete: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    imeAction: ImeAction = ImeAction.Done
) {
    val error by phoneNumberController.error.collectAsState()

    val sectionErrorString = error?.let {
        it.formatArgs?.let { args ->
            stringResource(
                it.errorMessage,
                *args
            )
        } ?: stringResource(it.errorMessage)
    }

    Section(
        modifier = Modifier.padding(vertical = 8.dp),
        title = sectionTitle,
        error = sectionErrorString,
        isSelected = isSelected
    ) {
        PhoneNumberElementUI(
            modifier = modifier,
            countryDropdown = countryDropdown,
            enabled = enabled,
            controller = phoneNumberController,
            requestFocusWhenShown = requestFocusWhenShown,
            moveToNextFieldOnceComplete = moveToNextFieldOnceComplete,
            focusRequester = focusRequester,
            imeAction = imeAction
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
@Suppress("LongMethod")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PhoneNumberElementUI(
    enabled: Boolean,
    controller: PhoneNumberController,
    modifier: Modifier = Modifier,
    countryDropdown: @Composable () -> Unit = { CountryDropdown(controller, enabled) },
    requestFocusWhenShown: Boolean = false,
    moveToNextFieldOnceComplete: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    trailingIcon: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusManager = LocalFocusManager.current

    val value by controller.fieldValue.collectAsState()
    val isComplete by controller.isComplete.collectAsState()
    val shouldShowError by controller.error.collectAsState()
    val label by controller.label.collectAsState()
    val placeholder by controller.placeholder.collectAsState()
    val visualTransformation by controller.visualTransformation.collectAsState()
    val colors = TextFieldColors(shouldShowError != null)
    var hasFocus by rememberSaveable { mutableStateOf(false) }

    if (moveToNextFieldOnceComplete) {
        LaunchedEffect(isComplete) {
            if (isComplete && hasFocus) {
                focusManager.moveFocusSafely(FocusDirection.Next)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        TextField(
            value = value,
            onValueChange = controller::onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .focusRequester(focusRequester)
                .autofill(
                    types = listOf(AutofillType.PhoneNumberNational),
                    onFill = controller::onValueChange,
                )
                .onFocusEvent {
                    if (it.isFocused) {
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                }
                .onFocusChanged {
                    if (hasFocus != it.isFocused) {
                        controller.onFocusChange(it.isFocused)
                    }
                    hasFocus = it.isFocused
                }
                .testTag(PHONE_NUMBER_TEXT_FIELD_TAG),
            enabled = enabled,
            label = {
                FormLabel(
                    text = if (controller.showOptionalLabel) {
                        stringResource(
                            R.string.stripe_form_label_optional,
                            stringResource(label)
                        )
                    } else {
                        stringResource(label)
                    }
                )
            },
            placeholder = {
                Text(text = placeholder)
            },
            leadingIcon = countryDropdown,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocusSafely(FocusDirection.Next)
                },
                onDone = {
                    focusManager.clearFocus(true)
                }
            ),
            singleLine = true,
            colors = colors
        )
    }

    if (requestFocusWhenShown) {
        LaunchedEffect(Unit) {
            coroutineContext.job.invokeOnCompletion {
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
private fun CountryDropdown(
    phoneNumberController: PhoneNumberController,
    enabled: Boolean
) {
    DropDown(
        controller = phoneNumberController.countryDropdownController,
        enabled = enabled,
        modifier = Modifier
            .padding(start = 16.dp, end = 8.dp)
    )
}
