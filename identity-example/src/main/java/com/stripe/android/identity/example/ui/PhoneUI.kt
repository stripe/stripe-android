package com.stripe.android.identity.example.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.example.PhoneOTPCheck
import com.stripe.android.identity.example.R

@Composable
internal fun PhoneUI(
    scrollState: ScrollState,
    submissionState: IdentitySubmissionState,
    onSubmissionStateChanged: (IdentitySubmissionState) -> Unit
) {
    var useDocumentFallback by remember {
        mutableStateOf(false)
    }
    var optCheck by remember {
        mutableStateOf(PhoneOTPCheck.Attempt)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = useDocumentFallback, onCheckedChange = {
            useDocumentFallback = it
            onSubmissionStateChanged(
                submissionState.copy(
                    useDocumentFallback = useDocumentFallback
                )
            )
        })
        StyledClickableText(
            text = AnnotatedString(stringResource(id = R.string.document_fallback)),
            onClick = {
                useDocumentFallback = !useDocumentFallback
                onSubmissionStateChanged(
                    submissionState.copy(
                        useDocumentFallback = useDocumentFallback
                    )
                )
            }
        )
    }

    if (useDocumentFallback) {
        if (submissionState.phoneOtpCheck == null) {
            onSubmissionStateChanged(submissionState.copy(phoneOtpCheck = PhoneOTPCheck.Attempt))
        }
        OtpCheckSelectUI(selectedCheckType = optCheck, onNewCheckTypeSelected = {
            optCheck = it
            onSubmissionStateChanged(
                submissionState.copy(
                    phoneOtpCheck = optCheck
                )
            )
        })
        Divider()
        DocumentUI(
            submissionState = submissionState,
            onSubmissionStateChanged = onSubmissionStateChanged,
            shouldShowPhoneNumber = false,
            scrollState = scrollState
        )
    }
}

@Composable
private fun OtpCheckSelectUI(
    selectedCheckType: PhoneOTPCheck,
    onNewCheckTypeSelected: (PhoneOTPCheck) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Text(
        text = stringResource(id = R.string.otp_check),
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 10.dp, top = 16.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
    ) {
        OutlinedTextField(
            value = selectedCheckType.value,
            enabled = false,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable(onClick = { expanded = true }),
            placeholder = {
                Text(text = selectedCheckType.name)
            },
            onValueChange = {}
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                PhoneOTPCheck.None,
                PhoneOTPCheck.Attempt,
                PhoneOTPCheck.Required
            ).forEach { option ->
                DropdownMenuItem(onClick = {
                    onNewCheckTypeSelected(option)
                    expanded = false
                }) {
                    Text(text = option.value)
                }
            }
        }
    }
}
