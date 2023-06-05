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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_DOC_SELECT
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam.Type
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

internal const val docSelectionTitleTag = "Title"
internal const val singleSelectionTag = "SingleSelection"

internal const val PASSPORT_KEY = "passport"
internal const val DRIVING_LICENSE_KEY = "driving_license"
internal const val ID_CARD_KEY = "id_card"
internal const val SELECTION_NONE = ""

@Composable
internal fun DocSelectionScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    cameraPermissionEnsureable: CameraPermissionEnsureable
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current
    val viewLifecycleOwner = LocalLifecycleOwner.current
    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
            navController.navigateToErrorScreenWithDefaultValues(context)
        }
    ) {
        val documentSelect = remember { it.documentSelect }

        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = SCREEN_NAME_DOC_SELECT
        )

        val onDocTypeSelected: suspend (Type) -> Unit = { type: Type ->
            identityViewModel.postVerificationPageDataForDocSelection(
                type = type,
                navController = navController,
                viewLifecycleOwner = viewLifecycleOwner,
                cameraPermissionEnsureable = cameraPermissionEnsureable
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                    vertical = dimensionResource(id = R.dimen.stripe_page_vertical_margin)
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
                    onDocTypeSelected = onDocTypeSelected
                )
            } else {
                SingleSelection(
                    allowedType = documentSelect.idDocumentTypeAllowlist.entries.first().key,
                    buttonText = documentSelect.buttonText,
                    bodyText = documentSelect.body,
                    onDocTypeSelected = onDocTypeSelected
                )
            }
        }
    }
}

@Composable
internal fun MultiSelection(
    idDocumentTypeAllowlist: Map<String, String>,
    onDocTypeSelected: suspend (Type) -> Unit
) {
    var selectedTypeValue by remember { mutableStateOf(SELECTION_NONE) }
    val coroutineScope = rememberCoroutineScope()
    for ((allowedType, allowedTypeValue) in idDocumentTypeAllowlist) {
        val isSelected = selectedTypeValue == allowedTypeValue
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
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        onDocTypeSelected(Type.fromName(allowedType))
                    }
                    selectedTypeValue = allowedTypeValue
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LocalContentColor.current
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedTypeValue == SELECTION_NONE
            ) {
                Text(
                    text = allowedTypeValue.uppercase(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            if (isSelected) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(24.dp)
                        .padding(top = 4.dp, end = 8.dp),
                    strokeWidth = 3.dp
                )
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
    onDocTypeSelected: suspend (Type) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = singleSelectionTag }
    ) {
        Text(text = bodyText ?: "", modifier = Modifier.weight(1f))

        var buttonState by remember { mutableStateOf(LoadingButtonState.Idle) }
        LoadingButton(text = buttonText, state = buttonState) {
            buttonState = LoadingButtonState.Loading
            coroutineScope.launch {
                onDocTypeSelected(Type.fromName(allowedType))
            }
        }
    }
}
