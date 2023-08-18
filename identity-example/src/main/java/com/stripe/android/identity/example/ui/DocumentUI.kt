package com.stripe.android.identity.example.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.example.R
import com.stripe.android.identity.R as StripeR
@Composable
internal fun DocumentUI(
    submissionState: IdentitySubmissionState,
    onSubmissionStateChanged: (IdentitySubmissionState) -> Unit,
    shouldShowPhoneNumber: Boolean,
    scrollState: ScrollState
) {
    AllowedDocumentTypes(submissionState, onSubmissionStateChanged)
    RequireDocTypes(submissionState, onSubmissionStateChanged)
    // TODO(ccen) re-enable when backend supports PII
    //    if (shouldShowPhoneNumber) {
    //        Divider()
    //        RequirePhoneVerificationUI(scrollState, submissionState, onSubmissionStateChanged)
    //    }
}

@Composable
private fun AllowedDocumentTypes(
    identitySubmissionState: IdentitySubmissionState,
    onSubmissionStateChangedListener: (IdentitySubmissionState) -> Unit
) {
    Text(
        text = stringResource(id = R.string.allowed_types),
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 10.dp, top = 16.dp)
    )
    Column {
        Row(
            modifier = Modifier.padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = identitySubmissionState.allowDrivingLicense,
                onCheckedChange = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowDrivingLicense = it
                        )
                    )
                },
                modifier = Modifier.padding(end = 0.dp)
            )

            StyledClickableText(
                text = AnnotatedString(stringResource(id = StripeR.string.stripe_driver_license)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowDrivingLicense = !identitySubmissionState.allowDrivingLicense
                        )
                    )
                }
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = identitySubmissionState.allowPassport, onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        allowPassport = it
                    )
                )
            })
            StyledClickableText(
                text = AnnotatedString(stringResource(id = StripeR.string.stripe_passport)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowPassport = !identitySubmissionState.allowPassport
                        )
                    )
                }
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = identitySubmissionState.allowId, onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        allowId = it
                    )
                )
            })
            StyledClickableText(
                text = AnnotatedString(stringResource(id = StripeR.string.stripe_id_card)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowId = !identitySubmissionState.allowId
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun RequireDocTypes(
    identitySubmissionState: IdentitySubmissionState,
    onSubmissionStateChangedListener: (IdentitySubmissionState) -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = identitySubmissionState.requireLiveCapture,
            onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireLiveCapture = it
                    )
                )
            }
        )
        StyledClickableText(
            text = AnnotatedString(stringResource(id = R.string.require_live_capture)),
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireLiveCapture = !identitySubmissionState.requireLiveCapture
                    )
                )
            }
        )
    }

    Row(
        modifier = Modifier.padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = identitySubmissionState.requireId,
            onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireId = it
                    )
                )
            }
        )
        StyledClickableText(
            text = AnnotatedString(stringResource(id = R.string.require_id_number)),
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireId = !identitySubmissionState.requireId
                    )
                )
            }
        )
    }

    Row(
        modifier = Modifier.padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = identitySubmissionState.requireSelfie,
            onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireSelfie = it
                    )
                )
            }
        )
        StyledClickableText(
            text = AnnotatedString(stringResource(id = R.string.require_matching_selfie)),
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireSelfie = !identitySubmissionState.requireSelfie
                    )
                )
            }
        )
    }

    Row(
        modifier = Modifier.padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = identitySubmissionState.requireAddress,
            onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireAddress = it
                    )
                )
            }
        )
        StyledClickableText(
            text = AnnotatedString(stringResource(id = R.string.require_address)),
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireAddress = !identitySubmissionState.requireAddress
                    )
                )
            }
        )
    }
}
