package com.stripe.android.identity.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONSENT
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.utils.isRemote
import com.stripe.android.identity.utils.urlWithoutQuery
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.image.getDrawableFromUri
import com.stripe.android.uicore.image.rememberDrawablePainter
import com.stripe.android.uicore.text.Html
import kotlinx.coroutines.launch

internal const val TITLE_TAG = "Title"
internal const val TIME_ESTIMATE_TAG = "TimeEstimate"
internal const val CONSENT_HEADER_TAG = "ConsentHeader"
internal const val PRIVACY_POLICY_TAG = "PrivacyPolicy"
internal const val DIVIDER_TAG = "divider"
internal const val BODY_TAG = "Body"
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
            verificationPage,
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
    verificationPage: VerificationPage,
    onConsentAgreed: () -> Unit,
    onConsentDeclined: () -> Unit
) {
    val consentPage = verificationPage.biometricConsent
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                end = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                top = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                bottom = dimensionResource(id = R.dimen.stripe_page_vertical_margin)
            )
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
            val shouldShowHeader =
                consentPage.title != null && consentPage.privacyPolicy != null && consentPage.timeEstimate != null

            if (shouldShowHeader) {
                ConsentHeader(merchantLogoUri = merchantLogoUri, consentPage = consentPage)
            }

            Html(
                html = consentPage.body,
                modifier = Modifier
                    .padding(bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
                    .semantics {
                        testTag = BODY_TAG
                    },
                color = MaterialTheme.colors.onBackground,
                urlSpanStyle = SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colors.secondary
                )
            )
        }

        var acceptState by remember { mutableStateOf(LoadingButtonState.Idle) }
        var declineState by remember { mutableStateOf(LoadingButtonState.Idle) }

        var scrolledToBottom by remember { mutableStateOf(false) }
        LaunchedEffect(scrollState.value) {
            if (!scrolledToBottom) {
                scrolledToBottom = scrollState.value == scrollState.maxValue
            }
        }

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

        LoadingButton(
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

@Composable
private fun ConsentHeader(
    merchantLogoUri: Uri,
    consentPage: VerificationPageStaticContentConsentPage
) {
    val title = requireNotNull(consentPage.title)
    val privacyPolicy = requireNotNull(consentPage.privacyPolicy)
    val timeEstimate = requireNotNull(consentPage.timeEstimate)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CONSENT_HEADER_TAG),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (merchantLogoUri.isRemote()) {
            val localContext = LocalContext.current
            val imageLoader = remember(merchantLogoUri) {
                StripeImageLoader(localContext)
            }
            StripeImage(
                url = merchantLogoUri.urlWithoutQuery(),
                imageLoader = imageLoader,
                contentDescription = stringResource(id = R.string.stripe_description_merchant_logo),
                modifier = Modifier
                    .width(32.dp)
                    .height(32.dp)
            )
        } else {
            Image(
                painter = rememberDrawablePainter(
                    LocalContext.current.getDrawableFromUri(
                        merchantLogoUri
                    )
                ),
                modifier = Modifier
                    .width(32.dp)
                    .height(32.dp),
                contentDescription = stringResource(id = R.string.stripe_description_merchant_logo)
            )
        }
        Image(
            painter = painterResource(id = R.drawable.stripe_plus_icon),
            modifier = Modifier
                .width(16.dp)
                .height(16.dp),
            contentDescription = stringResource(id = R.string.stripe_description_plus)
        )

        Image(
            painter = painterResource(id = R.drawable.stripe_square),
            modifier = Modifier
                .width(32.dp)
                .height(32.dp),
            contentDescription = stringResource(id = R.string.stripe_description_stripe_logo)
        )
    }
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(
                    id = R.dimen.stripe_item_vertical_margin
                )
            )
            .semantics {
                testTag = TITLE_TAG
            },
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.stripe_time_estimate_icon),
            contentDescription = stringResource(id = R.string.stripe_description_time_estimate)
        )
        Html(
            html = timeEstimate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp)
                .semantics {
                    testTag = TIME_ESTIMATE_TAG
                },
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
            urlSpanStyle = SpanStyle(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colors.secondary
            )
        )
    }
    Html(
        html = privacyPolicy,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
            .semantics {
                testTag = PRIVACY_POLICY_TAG
            },
        color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
        urlSpanStyle = SpanStyle(
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colors.secondary
        )
    )
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
            .semantics {
                testTag = DIVIDER_TAG
            }
    )
}
