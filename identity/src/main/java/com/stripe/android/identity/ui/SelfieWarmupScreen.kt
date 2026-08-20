package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.navigation.SelfieDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.models.VerificationPage.Companion.has3DFaceCaptureExperiment
import com.stripe.android.identity.viewmodel.IdentityViewModel

@Composable
internal fun SelfieWarmupScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel
) {
    CheckVerificationPageAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) { verificationPage ->
        val useRedesignedTrainingConsent = verificationPage.has3DFaceCaptureExperiment()
        SelfieWarmupContent(
            navController = navController,
            identityViewModel = identityViewModel,
            trainingConsentText = verificationPage.selfieCapture?.consentText.orEmpty(),
            useRedesignedTrainingConsent = useRedesignedTrainingConsent,
            declineButtonText = if (useRedesignedTrainingConsent) {
                verificationPage.selfieCapture?.declineAndContinueButtonText
                    ?: verificationPage.biometricConsent.declineButtonText
            } else {
                null
            }
        )
    }
}

@Composable
private fun SelfieWarmupContent(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    trainingConsentText: String,
    useRedesignedTrainingConsent: Boolean,
    declineButtonText: String?
) {
    val shouldShowTrainingConsent = trainingConsentText.isNotBlank()
    val shouldUseRedesignedTrainingConsent = shouldShowTrainingConsent && useRedesignedTrainingConsent
    var selectedTrainingConsent by remember {
        mutableStateOf<Boolean?>(null)
    }

    fun continueWithTrainingConsent(trainingConsent: Boolean) {
        selectedTrainingConsent = trainingConsent
        identityViewModel.setSelfieTrainingConsent(trainingConsent)
        identityViewModel.screenTracker.screenTransitionStart(
            IdentityAnalyticsRequestFactory.SCREEN_NAME_SELFIE_WARMUP
        )
        navController.navigateTo(SelfieDestination)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
            )
    ) {
        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = IdentityAnalyticsRequestFactory.SCREEN_NAME_SELFIE_WARMUP
        )

        SelfieWarmupMainContent(
            shouldUseRedesignedTrainingConsent = shouldUseRedesignedTrainingConsent,
            modifier = Modifier.weight(1f)
        )
        SelfieWarmupActions(
            trainingConsentText = trainingConsentText,
            shouldUseRedesignedTrainingConsent = shouldUseRedesignedTrainingConsent,
            declineButtonText = declineButtonText,
            selectedTrainingConsent = selectedTrainingConsent,
            onTrainingConsentSelected = ::continueWithTrainingConsent
        )
    }
}

@Composable
private fun SelfieWarmupMainContent(
    shouldUseRedesignedTrainingConsent: Boolean,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag(SELFIE_WARMUP_CONTENT_TAG),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                id = if (shouldUseRedesignedTrainingConsent) {
                    R.string.stripe_selfie_consent_title
                } else {
                    R.string.stripe_selfie_warmup_title
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SELFIE_WARMUP_TITLE_TAG),
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(
                id = if (shouldUseRedesignedTrainingConsent) {
                    R.string.stripe_selfie_consent_body
                } else {
                    R.string.stripe_selfie_warmup_body
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(id = R.dimen.stripe_item_vertical_margin),
                )
                .testTag(SELFIE_WARMUP_BODY_TAG),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(
                if (shouldUseRedesignedTrainingConsent) 32.dp else 64.dp
            )
        )
        Image(
            painter = painterResource(id = R.drawable.stripe_selfie_warmup),
            modifier = Modifier
                .size(144.dp)
                .align(Alignment.CenterHorizontally),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorResource(id = R.color.stripe_selfie_warmup_icon_tint))
        )
    }
}

@Composable
private fun SelfieWarmupActions(
    trainingConsentText: String,
    shouldUseRedesignedTrainingConsent: Boolean,
    declineButtonText: String?,
    selectedTrainingConsent: Boolean?,
    onTrainingConsentSelected: (Boolean) -> Unit
) {
    if (trainingConsentText.isNotBlank()) {
        TrainingConsentActions(
            trainingConsentText = trainingConsentText,
            shouldUseRedesignedTrainingConsent = shouldUseRedesignedTrainingConsent,
            declineButtonText = declineButtonText,
            selectedTrainingConsent = selectedTrainingConsent,
            onTrainingConsentSelected = onTrainingConsentSelected
        )
    } else {
        ContinueAction(
            isLoading = selectedTrainingConsent == false,
            onContinue = { onTrainingConsentSelected(false) }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun TrainingConsentActions(
    trainingConsentText: String,
    shouldUseRedesignedTrainingConsent: Boolean,
    declineButtonText: String?,
    selectedTrainingConsent: Boolean?,
    onTrainingConsentSelected: (Boolean) -> Unit
) {
    BottomSheetHTML(
        html = if (shouldUseRedesignedTrainingConsent) {
            trainingConsentText
        } else {
            trainingConsentHtml(
                title = stringResource(id = R.string.stripe_selfie_training_consent_title),
                body = trainingConsentText
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin)
            )
            .testTag(SELFIE_TRAINING_CONSENT_FOOTER_TAG),
        bottomSheets = null,
        color = MaterialTheme.colors.onBackground,
        style = MaterialTheme.typography.caption.copy(textAlign = TextAlign.Center),
        urlSpanStyle = SpanStyle(
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colors.onBackground
        )
    )

    LoadingButton(
        modifier = Modifier.testTag(SELFIE_ALLOW_BUTTON_TAG),
        text = stringResource(id = R.string.stripe_allow),
        state = when (selectedTrainingConsent) {
            true -> LoadingButtonState.Loading
            false -> LoadingButtonState.Disabled
            null -> LoadingButtonState.Idle
        }
    ) {
        onTrainingConsentSelected(true)
    }
    Spacer(modifier = Modifier.height(8.dp))
    LoadingTextButton(
        modifier = Modifier.testTag(SELFIE_DECLINE_BUTTON_TAG),
        text = declineButtonText ?: stringResource(id = R.string.stripe_decline),
        state = when (selectedTrainingConsent) {
            true -> LoadingButtonState.Disabled
            false -> LoadingButtonState.Loading
            null -> LoadingButtonState.Idle
        }
    ) {
        onTrainingConsentSelected(false)
    }
}

@Composable
private fun ContinueAction(
    isLoading: Boolean,
    onContinue: () -> Unit
) {
    LoadingButton(
        modifier = Modifier.testTag(SELFIE_CONTINUE_BUTTON_TAG),
        text = stringResource(id = R.string.stripe_kontinue).uppercase(),
        state = if (isLoading) {
            LoadingButtonState.Loading
        } else {
            LoadingButtonState.Idle
        },
        onClick = onContinue
    )
}

private fun trainingConsentHtml(
    title: String,
    body: String
) = "<b>$title</b><br/><br/>$body"

internal const val SELFIE_WARMUP_CONTENT_TAG = "SelfieWarmupContentTag"
internal const val SELFIE_WARMUP_TITLE_TAG = "SelfieWarmupTitleTag"
internal const val SELFIE_WARMUP_BODY_TAG = "SelfieWarmupBodyTag"
internal const val SELFIE_TRAINING_CONSENT_FOOTER_TAG = "SelfieTrainingConsentFooterTag"
internal const val SELFIE_CONTINUE_BUTTON_TAG = "SelfieContinueButtonTag"
internal const val SELFIE_ALLOW_BUTTON_TAG = "SelfieAllowButtonTag"
internal const val SELFIE_DECLINE_BUTTON_TAG = "SelfieDeclineButtonTag"
