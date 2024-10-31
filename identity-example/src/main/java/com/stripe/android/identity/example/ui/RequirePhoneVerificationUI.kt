package com.stripe.android.identity.example.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.stripe.android.identity.example.R
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI

@Composable
internal fun RequirePhoneVerificationUI(
    scrollState: ScrollState,
    submissionState: IdentitySubmissionState,
    onSubmissionStateChanged: (IdentitySubmissionState) -> Unit
) {
    var requirePhoneVerification by remember {
        mutableStateOf(false)
    }

    val phoneController = remember {
        PhoneNumberController.createPhoneNumberController(
            overrideCountryCodes = setOf("US")
        )
    }

    val currentPhoneNumber by phoneController.fieldValue.collectAsState(initial = "")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = requirePhoneVerification, onCheckedChange = {
            requirePhoneVerification = it
        })
        StyledClickableText(
            text = AnnotatedString(stringResource(id = R.string.require_phone_number)),
            onClick = {
                requirePhoneVerification = !requirePhoneVerification
            }
        )
    }
    if (requirePhoneVerification) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            SectionElementUI(
                modifier = Modifier.padding(vertical = 8.dp),
                enabled = true,
                element = SectionElement.wrap(
                    PhoneNumberElement(
                        identifier = IdentifierSpec.Phone,
                        controller = phoneController
                    )
                ),
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = IdentifierSpec.Phone,
            )
        }
    }
    LaunchedEffect(requirePhoneVerification) {
        scrollState.scrollTo(scrollState.maxValue)
    }
    LaunchedEffect(currentPhoneNumber, requirePhoneVerification) {
        if (requirePhoneVerification) {
            onSubmissionStateChanged(
                submissionState.copy(
                    requirePhoneVerification = true,
                    providedPhoneNumber = phoneController.getE164PhoneNumber(currentPhoneNumber)
                )
            )
        } else {
            onSubmissionStateChanged(
                submissionState.copy(
                    requirePhoneVerification = false,
                    providedPhoneNumber = null
                )
            )
        }
    }
}
