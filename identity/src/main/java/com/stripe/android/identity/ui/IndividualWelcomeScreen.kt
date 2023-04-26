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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_INDIVIDUAL_WELCOME
import com.stripe.android.identity.navigation.IndividualDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.models.VerificationPageStaticContentIndividualWelcomePage
import com.stripe.android.identity.utils.isRemote
import com.stripe.android.identity.utils.urlWithoutQuery
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.image.getDrawableFromUri
import com.stripe.android.uicore.image.rememberDrawablePainter
import com.stripe.android.uicore.text.Html

@Composable
internal fun IndividualWelcomeScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel
) {
    CheckVerificationPageAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) { verificationPage ->

        val individualWelcomePage = verificationPage.individualWelcome
        val merchantLogoUri = identityViewModel.verificationArgs.brandLogo
        var acceptState by remember { mutableStateOf(LoadingButtonState.Idle) }
        val scrollState = rememberScrollState()

        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = SCREEN_NAME_INDIVIDUAL_WELCOME
        )
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                WelcomeHeader(merchantLogoUri, individualWelcomePage)
                WelcomeBody(individualWelcomePage)
            }

            LoadingButton(
                modifier = Modifier
                    .semantics { testTag = INDIVIDUAL_WELCOME_GET_STARTED_BUTTON_TAG },
                text = individualWelcomePage.getStartedButtonText.uppercase(),
                state = acceptState
            ) {
                acceptState = LoadingButtonState.Disabled
                navController.navigateTo(IndividualDestination)
            }
        }
    }
}

@Composable
private fun WelcomeHeader(
    merchantLogoUri: Uri,
    individualWelcomePage: VerificationPageStaticContentIndividualWelcomePage
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                    LocalContext.current.getDrawableFromUri(merchantLogoUri)
                ),
                modifier = Modifier
                    .width(32.dp)
                    .height(32.dp),
                contentDescription = stringResource(id = R.string.stripe_description_merchant_logo)
            )
        }
        Image(
            painter = painterResource(id = R.drawable.plus_icon),
            modifier = Modifier
                .width(16.dp)
                .height(16.dp),
            contentDescription = stringResource(id = R.string.stripe_description_plus)
        )

        Image(
            painter = painterResource(id = R.drawable.ic_stripe_square_32),
            modifier = Modifier
                .width(32.dp)
                .height(32.dp),
            contentDescription = stringResource(id = R.string.stripe_description_stripe_logo)
        )
    }
    Text(
        text = individualWelcomePage.title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(id = R.dimen.item_vertical_margin)
            )
            .semantics {
                testTag = INDIVIDUAL_WELCOME_TITLE_TAG
            },
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun WelcomeBody(individualWelcomePage: VerificationPageStaticContentIndividualWelcomePage) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.time_estimate_icon),
            contentDescription = stringResource(id = R.string.stripe_description_time_estimate)
        )
        Html(
            html = individualWelcomePage.timeEstimate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp)
                .semantics {
                    testTag = INDIVIDUAL_WELCOME_TIME_ESTIMATE_TAG
                },
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
            urlSpanStyle = SpanStyle(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colors.secondary
            )
        )
    }
    Html(
        html = individualWelcomePage.privacyPolicy,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.item_vertical_margin))
            .semantics {
                testTag = INDIVIDUAL_WELCOME_PRIVACY_POLICY_TAG
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
            .padding(bottom = dimensionResource(id = R.dimen.item_vertical_margin))
    )

    Html(
        html = individualWelcomePage.body,
        modifier = Modifier
            .padding(bottom = dimensionResource(id = R.dimen.item_vertical_margin))
            .semantics {
                testTag = INDIVIDUAL_WELCOME_BODY_TAG
            },
        color = MaterialTheme.colors.onBackground,
        urlSpanStyle = SpanStyle(
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colors.secondary
        )
    )
}

internal const val INDIVIDUAL_WELCOME_PRIVACY_POLICY_TAG = "PrivacyPolicy"
internal const val INDIVIDUAL_WELCOME_TIME_ESTIMATE_TAG = "TimeEstimate"
internal const val INDIVIDUAL_WELCOME_TITLE_TAG = "Title"
internal const val INDIVIDUAL_WELCOME_GET_STARTED_BUTTON_TAG = "GetStarted"
internal const val INDIVIDUAL_WELCOME_BODY_TAG = "Body"
