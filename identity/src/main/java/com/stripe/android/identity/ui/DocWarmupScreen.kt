package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
            screenName = IdentityAnalyticsRequestFactory.SCREEN_NAME_SELFIE_WARMUP
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
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.stripe_doc_front_warmup_body),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(id = R.dimen.stripe_item_vertical_margin),
                    ),
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 20.dp)
                    .border(
                        width = 2.dp,
                        color = Color.LightGray,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp)
                    .testTag(DOC_FRONT_ACCEPTED_IDS_TAG)
            ) {
                Text(
                    text = stringResource(id = R.string.stripe_accepted_forms_of_id),
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                )
                documentSelectPage.idDocumentTypeAllowlist.keys.map {
                    when (it) {
                        DRIVING_LICENSE_KEY -> stringResource(id = R.string.stripe_driver_license)
                        ID_CARD_KEY -> stringResource(id = R.string.stripe_government_id)
                        PASSPORT_KEY -> stringResource(id = R.string.stripe_passport)
                        else -> null
                    }
                }.forEach { idType ->
                    idType?.let {
                        Text(
                            text = "• $idType",
                            modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
                        )
                    }
                }
            }
        }

        LoadingButton(
            modifier = Modifier.testTag(DOC_FRONT_CONTINUE_BUTTON_TAG),
            text = stringResource(id = R.string.stripe_kontinue).uppercase(),
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
