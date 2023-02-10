@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.R

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
    @StringRes sectionTitle: Int? = null,
    requestFocusWhenShown: Boolean = false,
    imeAction: ImeAction = ImeAction.Done
) {
    val error by phoneNumberController.error.collectAsState(null)

    val sectionErrorString = error?.let {
        it.formatArgs?.let { args ->
            stringResource(
                it.errorMessage,
                *args
            )
        } ?: stringResource(it.errorMessage)
    }

    Section(sectionTitle, sectionErrorString) {
        PhoneNumberElementUI(enabled, phoneNumberController, requestFocusWhenShown, imeAction)
    }
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PhoneNumberElementUI(
    enabled: Boolean,
    controller: PhoneNumberController,
    requestFocusWhenShown: Boolean = false,
    imeAction: ImeAction = ImeAction.Done
) {
    val focusManager = LocalFocusManager.current
    val selectedIndex by controller.countryDropdownController.selectedIndex.collectAsState(0)
    controller.onSelectedCountryIndex(selectedIndex)
    val value by controller.fieldValue.collectAsState("")
    val shouldShowError by controller.error.collectAsState(null)
    val label by controller.label.collectAsState(R.string.address_label_phone_number)
    val placeholder by controller.placeholder.collectAsState("")
    val visualTransformation by controller.visualTransformation.collectAsState(VisualTransformation.None)
    val colors = TextFieldColors(shouldShowError != null)
    val focusRequester = remember { FocusRequester() }
    var hasFocus by rememberSaveable { mutableStateOf(false) }

    androidx.compose.material.TextField(
        value = value,
        onValueChange = controller::onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (hasFocus != it.isFocused) {
                    controller.onFocusChange(it.isFocused)
                }
                hasFocus = it.isFocused
            },
        enabled = enabled,
        label = {
            FormLabel(
                text = if (controller.showOptionalLabel) {
                    stringResource(
                        R.string.form_label_optional,
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
        leadingIcon = {
            DropDown(
                controller = controller.countryDropdownController,
                enabled = enabled,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp)
            )
        },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
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
        colors = colors
    )

    if (requestFocusWhenShown) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
