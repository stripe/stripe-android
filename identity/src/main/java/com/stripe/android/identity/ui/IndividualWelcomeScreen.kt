package com.stripe.android.identity.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_INDIVIDUAL_WELCOME
import com.stripe.android.identity.navigation.IndividualDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import com.stripe.android.identity.networking.models.VerificationPageStaticContentIndividualWelcomePage
import com.stripe.android.identity.viewmodel.IdentityViewModel
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

        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = SCREEN_NAME_INDIVIDUAL_WELCOME
        )
        LaunchedEffect(Unit) {
            identityViewModel.visitedIndividualWelcome()
        }

        SuccessUI(
            merchantLogoUri = merchantLogoUri,
            welcomePage = individualWelcomePage,
            bottomSheets = verificationPage.bottomSheet,
            navController = navController
        )
    }
}

@Composable
private fun SuccessUI(
    merchantLogoUri: Uri,
    welcomePage: VerificationPageStaticContentIndividualWelcomePage,
    bottomSheets: Map<String, VerificationPageStaticContentBottomSheetContent>?,
    navController: NavController,
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
                merchantLogoUri = merchantLogoUri,
                title = welcomePage.title
            )
            ConsentLines(lines = welcomePage.lines, bottomSheets = bottomSheets)
        }

        var scrolledToBottom by remember { mutableStateOf(false) }
        LaunchedEffect(scrollState.value) {
            if (!scrolledToBottom) {
                scrolledToBottom = scrollState.value == scrollState.maxValue
            }
        }

        Html(
            html = welcomePage.privacyPolicy,
            modifier = Modifier
                .padding(vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
                .semantics {
                    testTag = PRIVACY_POLICY_TAG
                },
            color = MaterialTheme.colors.onBackground,
            urlSpanStyle = SpanStyle(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colors.secondary
            )
        )

        var acceptState by remember { mutableStateOf(LoadingButtonState.Idle) }
        LoadingButton(
            modifier = Modifier
                .semantics { testTag = INDIVIDUAL_WELCOME_GET_STARTED_BUTTON_TAG },
            text = welcomePage.getStartedButtonText.uppercase(),
            state = acceptState
        ) {
            acceptState = LoadingButtonState.Disabled
            navController.navigateTo(IndividualDestination)
        }
    }
}

internal const val INDIVIDUAL_WELCOME_PRIVACY_POLICY_TAG = "PrivacyPolicy"
internal const val INDIVIDUAL_WELCOME_TITLE_TAG = "Title"
internal const val INDIVIDUAL_WELCOME_GET_STARTED_BUTTON_TAG = "GetStarted"
