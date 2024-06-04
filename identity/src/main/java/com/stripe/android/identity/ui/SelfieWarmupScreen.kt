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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.navigation.SelfieDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.viewmodel.IdentityViewModel

@Composable
@Suppress("LongMethod")
internal fun SelfieWarmupScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel
) {
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
        }

        LoadingButton(
            modifier = Modifier.testTag(SELFIE_CONTINUE_BUTTON_TAG),
            text = stringResource(id = R.string.stripe_kontinue).uppercase(),
            state = continueButtonState
        ) {
            continueButtonState = LoadingButtonState.Loading
            navController.navigateTo(SelfieDestination)
        }
    }
}

internal const val SELFIE_WARMUP_CONTENT_TAG = "SelfieWarmupContentTag"
internal const val SELFIE_CONTINUE_BUTTON_TAG = "SelfieContinueButtonTag"
