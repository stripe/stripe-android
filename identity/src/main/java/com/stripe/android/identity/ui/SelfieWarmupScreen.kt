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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.navigation.SelfieDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.text.Html

@Composable
@Suppress("LongMethod")
internal fun SelfieWarmupScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel
) {
    CheckVerificationPageAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) { verificationPage ->
        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = IdentityAnalyticsRequestFactory.SCREEN_NAME_SELFIE_WARMUP
        )
        SelfieWarmupView(
            trainingConsentText = verificationPage.selfieCapture?.trainingConsentText
        ) { trainingConsent ->
            navController.navigateTo(
                SelfieDestination(trainingConsent = trainingConsent ?: false)
            )
        }
    }
}

@Composable
internal fun SelfieWarmupView(
    trainingConsentText: String?,
    onContinueClick: (Boolean?) -> Unit
) {
    val showsTrainingConsent = !trainingConsentText.isNullOrBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
            )
    ) {
        var allowButtonState by remember {
            mutableStateOf(LoadingButtonState.Idle)
        }
        var declineButtonState by remember {
            mutableStateOf(LoadingButtonState.Idle)
        }
        var continueButtonState by remember {
            mutableStateOf(LoadingButtonState.Idle)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag(SELFIE_WARMUP_CONTENT_TAG),
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.stripe_selfie_warmup),
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterHorizontally),
                contentDescription = stringResource(id = R.string.stripe_description_exclamation)
            )
            Text(
                text = stringResource(id = R.string.stripe_selfie_warmup_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(id = R.dimen.stripe_item_vertical_margin)
                    ),
                style = MaterialTheme.typography.h4,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.stripe_selfie_warmup_body),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(id = R.dimen.stripe_item_vertical_margin),
                    ),
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center
            )
            if (showsTrainingConsent) {
                Text(
                    text = stringResource(id = R.string.stripe_selfie_warmup_training_consent_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = TextAlign.Center
                )
                Html(
                    html = requireNotNull(trainingConsentText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag(SELFIE_TRAINING_CONSENT_TAG),
                    color = colorResource(id = R.color.stripe_html_line),
                    urlSpanStyle = SpanStyle(
                        color = colorResource(id = R.color.stripe_html_line)
                    )
                )
            }
        }

        if (showsTrainingConsent) {
            LoadingButton(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .testTag(SELFIE_ALLOW_BUTTON_TAG),
                text = stringResource(id = R.string.stripe_allow).uppercase(),
                state = allowButtonState
            ) {
                allowButtonState = LoadingButtonState.Loading
                declineButtonState = LoadingButtonState.Disabled
                onContinueClick(true)
            }
            LoadingTextButton(
                modifier = Modifier.testTag(SELFIE_DECLINE_BUTTON_TAG),
                text = stringResource(id = R.string.stripe_decline).uppercase(),
                state = declineButtonState
            ) {
                allowButtonState = LoadingButtonState.Disabled
                declineButtonState = LoadingButtonState.Loading
                onContinueClick(false)
            }
        } else {
            LoadingButton(
                modifier = Modifier.testTag(SELFIE_CONTINUE_BUTTON_TAG),
                text = stringResource(id = R.string.stripe_kontinue).uppercase(),
                state = continueButtonState
            ) {
                continueButtonState = LoadingButtonState.Loading
                onContinueClick(null)
            }
        }
    }
}

internal const val SELFIE_WARMUP_CONTENT_TAG = "SelfieWarmupContentTag"
internal const val SELFIE_CONTINUE_BUTTON_TAG = "SelfieContinueButtonTag"
internal const val SELFIE_ALLOW_BUTTON_TAG = "SelfieAllowButtonTag"
internal const val SELFIE_DECLINE_BUTTON_TAG = "SelfieDeclineButtonTag"
internal const val SELFIE_TRAINING_CONSENT_TAG = "SelfieTrainingConsentTag"
