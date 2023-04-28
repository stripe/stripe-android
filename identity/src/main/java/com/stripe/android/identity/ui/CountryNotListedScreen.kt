package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.navigation.IndividualDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.models.VerificationPageStaticContentCountryNotListedPage
import com.stripe.android.identity.viewmodel.IdentityViewModel

@Composable
internal fun CountryNotListedScreen(
    isMissingID: Boolean,
    navController: NavController,
    identityViewModel: IdentityViewModel,
    verificationFlowFinishable: VerificationFlowFinishable
) {
    CheckVerificationPageAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) { verificationPage ->
        val countryNotListedPage = requireNotNull(verificationPage.countryNotListedPage)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                    horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
                )
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                BodyContent(
                    navController = navController,
                    countryNotListedPage = countryNotListedPage,
                    isMissingID = isMissingID
                )
            }
            Button(
                onClick = {
                    verificationFlowFinishable.finishWithResult(
                        IdentityVerificationSheet.VerificationFlowResult.Canceled
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(COUNTRY_NOT_LISTED_CANCEL_BUTTON_TAG)
            ) {
                Text(text = countryNotListedPage.cancelButtonText.uppercase())
            }
        }
    }
}

@Composable
private fun BodyContent(
    navController: NavController,
    countryNotListedPage: VerificationPageStaticContentCountryNotListedPage,
    isMissingID: Boolean
) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center

    ) {
        Image(
            painter = painterResource(id = R.drawable.stripe_exclamation),
            modifier = Modifier
                .width(26.dp)
                .height(26.dp),
            contentDescription = stringResource(id = R.string.stripe_description_plus)
        )
    }
    Text(
        text = countryNotListedPage.title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin)
            )
            .testTag(COUNTRY_NOT_LISTED_TITLE_TAG),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = countryNotListedPage.body,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin)
            )
            .testTag(COUNTRY_NOT_LISTED_BODY_TAG),
    )
    TextButton(
        modifier = Modifier.testTag(COUNTRY_NOT_LISTED_OTHER_COUNTRY_TAG),
        contentPadding = PaddingValues(0.dp),
        onClick = {
            navController.navigateTo(IndividualDestination)
        }
    ) {
        Text(
            text = if (isMissingID) {
                countryNotListedPage
                    .idFromOtherCountryTextButtonText
            } else {
                countryNotListedPage
                    .addressFromOtherCountryTextButtonText
            },
            style = MaterialTheme.typography.h6
        )
    }
}

internal const val COUNTRY_NOT_LISTED_TITLE_TAG = "CountryNotListedTitle"
internal const val COUNTRY_NOT_LISTED_BODY_TAG = "CountryNotListedBody"
internal const val COUNTRY_NOT_LISTED_CANCEL_BUTTON_TAG = "CountryNotListedCancelButton"
internal const val COUNTRY_NOT_LISTED_OTHER_COUNTRY_TAG =
    "CountryNotListedOtherCountryButton"
