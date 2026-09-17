package com.stripe.android.identity.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.stripe.android.identity.networked.NetworkedIdentityEntry
import com.stripe.android.identity.networked.NetworkedIdentitySheetHost
import com.stripe.android.identity.networked.NetworkedIdentityViewModel
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.networking.models.VerificationPageIconType
import com.stripe.android.identity.networking.models.VerificationPageStaticConsentLineContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

internal const val TITLE_TAG = "Title"
internal const val CONSENT_HEADER_TAG = "ConsentHeader"
internal const val PRIVACY_POLICY_TAG = "PrivacyPolicy"
internal const val ACCEPT_BUTTON_TAG = "Accept"
internal const val DECLINE_BUTTON_TAG = "Decline"
internal const val LOADING_SCREEN_TAG = "Loading"
internal const val SCROLLABLE_COLUMN_TAG = "ScrollableColumn"
internal const val LINK_SAVED_ID_CHIP_TAG = "LinkSavedIdChip"
internal const val LINK_CONTINUE_BUTTON_TAG = "LinkContinue"
internal const val LINK_MANUAL_BUTTON_TAG = "LinkManual"

@Composable
internal fun ConsentScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    networkedIdentityViewModel: NetworkedIdentityViewModel?
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    NetworkedIdentitySheetHost(viewModel = networkedIdentityViewModel, savedItems = emptyList()) {
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
                hideBrandingHeader = identityViewModel.verificationArgs.biometricConsent?.hideBrandingHeader == true,
                showStripeLogo = !verificationPage.isStripe,
                linkEntry = networkedIdentityViewModel?.entry?.collectAsState()?.value?.takeIf { it.offersReuse },
                onContinueWithLink = { networkedIdentityViewModel?.startReuse() },
                onConsentAgreed = {
                    coroutineScope.launch {
                        identityViewModel.postVerificationPageDataAndMaybeNavigate(
                            navController,
                            CollectedDataParam(biometricConsent = true),
                            ConsentDestination.ROUTE.route
                        )
                    }
                },
                onConsentDeclined = {
                    coroutineScope.launch {
                        identityViewModel.postVerificationPageDataAndMaybeNavigate(
                            navController,
                            CollectedDataParam(biometricConsent = false),
                            ConsentDestination.ROUTE.route
                        )
                    }
                }
            )
        }
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SuccessUI(
    merchantLogoUri: Uri,
    consentPage: VerificationPageStaticContentConsentPage,
    bottomSheets: Map<String, VerificationPageStaticContentBottomSheetContent>?,
    visitedIndividualWelcomePage: Boolean,
    hideBrandingHeader: Boolean,
    showStripeLogo: Boolean = true,
    linkEntry: NetworkedIdentityEntry?,
    onContinueWithLink: () -> Unit,
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
                showLogos = !hideBrandingHeader && !visitedIndividualWelcomePage,
                showStripeLogo = showStripeLogo
            )
            ConsentLines(
                lines = consentPage.lines,
                bottomSheets = bottomSheets
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BottomSheetHTML(
                    html = consentPage.privacyPolicy,
                    bottomSheets = bottomSheets,
                    modifier = Modifier
                        .padding(vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
                        .semantics {
                            testTag = PRIVACY_POLICY_TAG
                        },
                    color = colorResource(id = R.color.stripe_html_line),
                    style = MaterialTheme.typography.body1,
                    urlSpanStyle = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = colorResource(id = R.color.stripe_html_line)
                    )
                )
            }
        }

        var acceptState by remember { mutableStateOf(LoadingButtonState.Idle) }
        var declineState by remember { mutableStateOf(LoadingButtonState.Idle) }
        var linkEntryVisible by remember(linkEntry) { mutableStateOf(linkEntry != null) }

        var scrolledToBottom by remember { mutableStateOf(false) }
        LaunchedEffect(scrollState.value) {
            if (!scrolledToBottom) {
                scrolledToBottom = scrollState.value == scrollState.maxValue
            }
        }

        if (linkEntry != null && linkEntryVisible) {
            // Without a known account there's no chip; the sheet asks for the email instead.
            linkEntry.accountEmail?.let { email ->
                LinkSavedIdChip(email = email, onDismiss = { linkEntryVisible = false })
            }
            // Networked Identity only starts from this explicit tap.
            LoadingButton(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .semantics { testTag = LINK_CONTINUE_BUTTON_TAG },
                text = stringResource(R.string.stripe_identity_link_continue_with_link).uppercase(),
                state = acceptState
            ) {
                onContinueWithLink()
            }

            LoadingTextButton(
                modifier = Modifier
                    .semantics { testTag = LINK_MANUAL_BUTTON_TAG },
                text = stringResource(R.string.stripe_identity_link_manual_instead).uppercase(),
                state = declineState
            ) {
                acceptState = LoadingButtonState.Disabled
                declineState = LoadingButtonState.Loading
                onConsentAgreed()
            }
        } else {
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
}

@Composable
private fun LinkSavedIdChip(email: String?, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(start = 12.dp)
            .testTag(LINK_SAVED_ID_CHIP_TAG),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_identity_link_logo),
            contentDescription = stringResource(R.string.stripe_identity_link),
            modifier = Modifier.width(40.dp).height(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = email?.let { stringResource(R.string.stripe_identity_link_saved_id_with_email, it) }
                ?: stringResource(R.string.stripe_identity_link_saved_id),
            style = MaterialTheme.typography.body2,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(
                painter = painterResource(R.drawable.stripe_close),
                contentDescription = stringResource(R.string.stripe_identity_link_dismiss),
                modifier = Modifier.size(16.dp)
            )
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
            hideBrandingHeader = false,
            bottomSheets = mapOf(),
            linkEntry = null,
            onContinueWithLink = {},
            onConsentAgreed = {},
            onConsentDeclined = {}
        )
    }
}
