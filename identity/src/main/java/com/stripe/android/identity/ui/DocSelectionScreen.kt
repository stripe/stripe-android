package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.composethemeadapter.MdcTheme
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.SELECTION_NONE
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.CollectedDataParam.Type
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie

internal const val docSelectionTitleTag = "Title"
internal const val singleSelectionTag = "SingleSelection"

@Composable
internal fun DocSelectionScreen(
    verificationPageState: Resource<VerificationPage>,
    onDocTypeSelected: (Type, Boolean) -> Unit
) {
    MdcTheme {
        when (verificationPageState.status) {
            Status.SUCCESS -> {
                val documentSelect =
                    remember { requireNotNull(verificationPageState.data).documentSelect }
                val requireSelfie =
                    remember { requireNotNull(verificationPageState.data).requireSelfie() }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = dimensionResource(id = R.dimen.page_horizontal_margin),
                            end = dimensionResource(id = R.dimen.page_horizontal_margin),
                            top = dimensionResource(id = R.dimen.page_vertical_margin),
                            bottom = dimensionResource(id = R.dimen.page_vertical_margin)
                        )
                ) {
                    Text(
                        text = documentSelect.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 58.dp,
                                bottom = 32.dp
                            )
                            .semantics {
                                testTag = docSelectionTitleTag
                            },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (documentSelect.idDocumentTypeAllowlist.count() > 1) {
                        MultiSelection(
                            documentSelect.idDocumentTypeAllowlist,
                            requireSelfie = requireSelfie,
                            onDocTypeSelected = onDocTypeSelected
                        )
                    } else {
                        SingleSelection(
                            allowedType = documentSelect.idDocumentTypeAllowlist.entries.first().key,
                            buttonText = documentSelect.buttonText,
                            bodyText = documentSelect.body,
                            requireSelfie = requireSelfie,
                            onDocTypeSelected = onDocTypeSelected
                        )
                    }
                }
            }
            else -> {
                // Unreachable as DocSelection can only be reached after Consent, at this point
                // a successful VerificationPage is already saved locally
            }
        }

    }
}

@Composable
internal fun MultiSelection(
    idDocumentTypeAllowlist: Map<String, String>,
    requireSelfie: Boolean,
    onDocTypeSelected: (Type, Boolean) -> Unit
) {
    var selectedTypeValue by remember { mutableStateOf(SELECTION_NONE) }

    for ((allowedType, allowedTypeValue) in idDocumentTypeAllowlist) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    bottom = 8.dp
                )
                .semantics {
                    testTag = allowedType
                }
        ) {
            when (selectedTypeValue) {
                SELECTION_NONE -> { // None selected
                    TextButton(
                        onClick = {
                            onDocTypeSelected(Type.fromName(allowedType), requireSelfie)
                            selectedTypeValue = allowedTypeValue
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = LocalContentColor.current
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = allowedTypeValue.uppercase(),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
                allowedTypeValue -> { // Own selected
                    TextButton(
                        onClick = {},
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = LocalContentColor.current
                        ),
                        enabled = false
                    ) {
                        Text(text = allowedTypeValue.uppercase())
                    }

                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(24.dp)
                            .padding(top = 4.dp, end = 8.dp),
                        strokeWidth = 3.dp
                    )
                }
                else -> { // Other selected
                    TextButton(
                        onClick = {},
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = LocalContentColor.current
                        ),
                        enabled = false
                    ) {
                        Text(text = allowedTypeValue.uppercase())
                    }
                }
            }
        }
        Divider()
    }
}


@Composable
internal fun SingleSelection(
    allowedType: String,
    buttonText: String,
    bodyText: String?,
    requireSelfie: Boolean,
    onDocTypeSelected: (Type, Boolean) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .semantics { testTag = singleSelectionTag }) {
        Text(text = bodyText ?: "", modifier = Modifier.weight(1f))

        var buttonState by remember { mutableStateOf(LoadingButtonState.Idle) }
        LoadingButton(text = buttonText, state = buttonState) {
            buttonState = LoadingButtonState.Loading
            onDocTypeSelected(Type.fromName(allowedType), requireSelfie)
        }
    }
}
