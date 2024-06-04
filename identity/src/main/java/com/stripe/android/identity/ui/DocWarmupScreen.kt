package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentSelectPage
import com.stripe.android.identity.viewmodel.IdentityViewModel

internal const val PASSPORT_KEY = "passport"
internal const val DRIVING_LICENSE_KEY = "driving_license"
internal const val ID_CARD_KEY = "id_card"
internal const val DOC_FRONT_CONTINUE_BUTTON_TAG = "DocFrontContinueButtonTag"
internal const val DOC_FRONT_ACCEPTED_IDS_TAG = "AcceptedFormsOfIdTag"

@Composable
internal fun DocWarmupScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    cameraPermissionEnsureable: CameraPermissionEnsureable
) {
    CheckVerificationPageAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) {
        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = IdentityAnalyticsRequestFactory.SCREEN_NAME_DOC_WARMUP
        )
        DocWarmupView(documentSelectPage = it.documentSelect) {
            identityViewModel.checkPermissionAndNavigate(navController, cameraPermissionEnsureable)
        }
    }
}

@Composable
internal fun DocWarmupView(
    documentSelectPage: VerificationPageStaticContentDocumentSelectPage,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
            )
    ) {
        var continueButtonState by remember {
            mutableStateOf(LoadingButtonState.Idle)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.stripe_doc_warmup_front),
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterHorizontally),
                contentDescription = ""
            )
            Text(
                text = stringResource(id = R.string.stripe_doc_front_warmup_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(id = R.dimen.stripe_item_vertical_margin)
                    ),
                style = MaterialTheme.typography.h4,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )

            val driverLicense = stringResource(id = R.string.stripe_driver_license)
            val governmentId = stringResource(id = R.string.stripe_government_id)
            val passport = stringResource(id = R.string.stripe_passport)
            val formsOfId = stringResource(id = R.string.stripe_accepted_forms_of_id_include)

            val allowedListString = remember(documentSelectPage) {
                "$formsOfId " + documentSelectPage.idDocumentTypeAllowlist.keys.mapNotNull {
                    when (it) {
                        DRIVING_LICENSE_KEY -> driverLicense
                        ID_CARD_KEY -> governmentId
                        PASSPORT_KEY -> passport
                        else -> null
                    }
                }.joinToString(", ") + "."
            }

            Text(
                text = allowedListString,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(id = R.dimen.stripe_item_vertical_margin),
                    )
                    .testTag(DOC_FRONT_ACCEPTED_IDS_TAG),
                style = MaterialTheme.typography.subtitle1.merge(lineHeight = 22.sp),
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = stringResource(id = R.string.stripe_doc_front_warmup_body),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin),
                ),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center
        )

        LoadingButton(
            modifier = Modifier.testTag(DOC_FRONT_CONTINUE_BUTTON_TAG),
            text = stringResource(id = R.string.stripe_im_ready).uppercase(),
            state = continueButtonState
        ) {
            continueButtonState = LoadingButtonState.Loading
            onContinueClick()
        }
    }
}

@Preview
@Composable
@ExperimentalMaterialApi
internal fun DocWarmupPreview() {
    IdentityPreview {
        DocWarmupView(
            documentSelectPage = VerificationPageStaticContentDocumentSelectPage(
                buttonText = "continue",
                title = "title",
                body = "body",
                idDocumentTypeAllowlist = mapOf(
                    "passport" to "Passport",
                    "driving_license" to "Driver's license",
                    "id_card" to "Identity card"
                )
            )
        ) {}
    }
}
