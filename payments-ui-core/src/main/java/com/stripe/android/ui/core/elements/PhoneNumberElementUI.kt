package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.paymentsColors

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PhoneNumberCollectionSection(
    enabled: Boolean,
    phoneNumberController: PhoneNumberController,
    @StringRes sectionTitle: Int? = null,
    requestFocusWhenShown: Boolean = false
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
        PhoneNumberElementUI(enabled, phoneNumberController, requestFocusWhenShown)
    }
}

@Composable
internal fun PhoneNumberElementUI(
    enabled: Boolean,
    controller: PhoneNumberController,
    requestFocusWhenShown: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val selectedIndex by controller.countryDropdownController.selectedIndex.collectAsState(0)
    controller.onSelectedCountryIndex(selectedIndex)
    val value by controller.fieldValue.collectAsState("")
    val shouldShowError by controller.error.collectAsState(null)
    val label by controller.label.collectAsState(R.string.address_label_phone_number)
    val placeholder by controller.placeholder.collectAsState("")
    val prefix by controller.prefix.collectAsState("")
    val visualTransformation by controller.visualTransformation.collectAsState(VisualTransformation.None)
    val colors = TextFieldColors(shouldShowError != null)
    var hasFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                hasFocus = focusState.hasFocus
            }
    ) {
        androidx.compose.material.TextField(
            value = value,
            onValueChange = controller::onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            enabled = enabled,
            label = {
                if (hasFocus || value.isNotBlank()) {
                    // Must show empty label so that the actual text field is correctly positioned
                    Text(text = "")
                } else {
                    AnimatedVisibility(visible = true) {
                        FormLabel(text = stringResource(label))
                    }
                }
            },
            placeholder = {
                if (hasFocus || value.isNotBlank()) {
                    Text(text = placeholder)
                }
            },
            leadingIcon = if (hasFocus || value.isNotBlank()) {
                {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = ExitTransition.None
                    ) {
                        Row(
                            modifier = Modifier.padding(top = 11.dp, start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DropDown(
                                controller = controller.countryDropdownController,
                                enabled = enabled
                            )
                            prefix.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.paymentsColors.placeholderText
                                )
                            }
                        }
                    }
                }
            } else null,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
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

        if (hasFocus || value.isNotBlank()) {
            FormLabel(
                text = stringResource(label),
                modifier = Modifier.padding(top = 6.dp, start = 16.dp)
            )
        }
    }

    if (requestFocusWhenShown) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
