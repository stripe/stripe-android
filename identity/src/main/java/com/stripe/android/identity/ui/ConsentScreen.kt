package com.stripe.android.identity.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONSENT
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.networking.models.VerificationPageIconType
import com.stripe.android.identity.networking.models.VerificationPageStaticConsentLineContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.text.Html
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

internal const val TITLE_TAG = "Title"
internal const val CONSENT_HEADER_TAG = "ConsentHeader"
internal const val PRIVACY_POLICY_TAG = "PrivacyPolicy"
internal const val ACCEPT_BUTTON_TAG = "Accept"
internal const val DECLINE_BUTTON_TAG = "Decline"
internal const val LOADING_SCREEN_TAG = "Loading"
internal const val SCROLLABLE_COLUMN_TAG = "ScrollableColumn"

@Composable
internal fun ConsentScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
            navController.navigateToErrorScreenWithDefaultValues(context)
        }
    ) {
        val verificationPage = remember { it }
        val visitedIndividualWelcomePage by
        identityViewModel.visitedIndividualWelcomeScreen.collectAsState()
        LaunchedEffect(Unit) {
            identityViewModel.updateAnalyticsState { oldState ->
                oldState.copy(
                    requireSelfie = verificationPage.requireSelfie()
                )
            }
        }
        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = SCREEN_NAME_CONSENT
        )
        SuccessUI(
            identityViewModel.verificationArgs.brandLogo,
            verificationPage.biometricConsent,
            verificationPage.bottomSheet,
            visitedIndividualWelcomePage,
            onConsentAgreed = {
                coroutineScope.launch {
                    identityViewModel.postVerificationPageDataAndMaybeNavigate(
                        navController,
                        CollectedDataParam(
                            biometricConsent = true
                        ),
                        ConsentDestination.ROUTE.route
                    )
                }
            },
            onConsentDeclined = {
                coroutineScope.launch {
                    identityViewModel.postVerificationPageDataAndMaybeNavigate(
                        navController,
                        CollectedDataParam(
                            biometricConsent = false
                        ),
                        ConsentDestination.ROUTE.route
                    )
                }
            }
        )
    }
}

@Composable
private fun SuccessUI(
    merchantLogoUri: Uri,
    consentPage: VerificationPageStaticContentConsentPage,
    bottomSheets: Map<String, VerificationPageStaticContentBottomSheetContent>?,
    visitedIndividualWelcomePage: Boolean,
    onConsentAgreed: () -> Unit,
    onConsentDeclined: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                end = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                top = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                bottom = dimensionResource(id = R.dimen.stripe_page_vertical_margin)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .semantics {
                    testTag = SCROLLABLE_COLUMN_TAG
                }
        ) {
            ConsentWelcomeHeader(
                modifier = Modifier.testTag(CONSENT_HEADER_TAG),
                merchantLogoUri = merchantLogoUri,
                title = consentPage.title,
                showLogos = visitedIndividualWelcomePage.not()
            )
            ConsentLines(lines = consentPage.lines, bottomSheets = bottomSheets)
        }

        var acceptState by remember { mutableStateOf(LoadingButtonState.Idle) }
        var declineState by remember { mutableStateOf(LoadingButtonState.Idle) }

        var scrolledToBottom by remember { mutableStateOf(false) }
        LaunchedEffect(scrollState.value) {
            if (!scrolledToBottom) {
                scrolledToBottom = scrollState.value == scrollState.maxValue
            }
        }

        Html(
            html = consentPage.privacyPolicy,
            modifier = Modifier
                .padding(vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
                .semantics {
                    testTag = PRIVACY_POLICY_TAG
                },
            color = colorResource(id = R.color.stripe_html_line),
            urlSpanStyle = SpanStyle(
                textDecoration = TextDecoration.Underline,
                color = colorResource(id = R.color.stripe_html_line)
            )
        )

        LoadingButton(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .semantics { testTag = ACCEPT_BUTTON_TAG },
            text =
            if (scrolledToBottom) {
                consentPage.acceptButtonText.uppercase()
            } else {
                consentPage.scrollToContinueButtonText.uppercase()
            },
            state = if (scrolledToBottom) {
                acceptState
            } else {
                LoadingButtonState.Disabled
            }
        ) {
            acceptState = LoadingButtonState.Loading
            declineState = LoadingButtonState.Disabled
            onConsentAgreed()
        }

        LoadingTextButton(
            modifier = Modifier
                .semantics { testTag = DECLINE_BUTTON_TAG },
            text = consentPage.declineButtonText.uppercase(),
            state = declineState
        ) {
            acceptState = LoadingButtonState.Disabled
            declineState = LoadingButtonState.Loading
            onConsentDeclined()
        }
    }
}

@Preview
@Composable
@ExperimentalMaterialApi
internal fun ConsentPreview() {
    IdentityPreview {
        SuccessUI(
            merchantLogoUri = Uri.EMPTY,
            consentPage = VerificationPageStaticContentConsentPage(
                acceptButtonText = "Accept",
                declineButtonText = "Decline",
                scrollToContinueButtonText = "scroll to button",
                title = "Tora's cat food works with Stripe to verify your identity",
                privacyPolicy = "<a href='https://stripe.com/privacy'>Stripe Privacy Policy</a> • " +
                    "<a href='https://tora.me'>Tora's cat food Privacy Policy</a>",
                lines = listOf(
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.PHONE,
                        content = "This is the line content with phone icon"
                    ),
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.CAMERA,
                        content = "This is the line content with camera icon"
                    ),
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.CLOUD,
                        content = "This is the line content with cloud icon and a " +
                            "<a href='https://stripe.com'>web link</a>"
                    ),
                    VerificationPageStaticConsentLineContent(
                        icon = VerificationPageIconType.WALLET,
                        content = "This is the line content with wallet icon and a <a " +
                            "href='stripe_bottomsheet://open/consent_verification_data'>" +
                            "bottomsheet link</a>"
                    )
                )
            ),
            visitedIndividualWelcomePage = false,
            bottomSheets = mapOf(),
            onConsentAgreed = {},
            onConsentDeclined = {}
        )
    }
}
