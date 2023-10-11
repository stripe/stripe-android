package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONFIRMATION
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.text.Html

@Composable
internal fun ConfirmationScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    verificationFlowFinishable: VerificationFlowFinishable
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current

    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
            navController.navigateToErrorScreenWithDefaultValues(context)
        }
    ) {
        val successPage = remember { it.success }
        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = SCREEN_NAME_CONFIRMATION
        )
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
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.Center

                ) {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_clock_icon),
                        modifier = Modifier
                            .width(26.dp)
                            .height(26.dp),
                        contentDescription = stringResource(id = R.string.stripe_description_plus)
                    )
                }
                Text(
                    text = successPage.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin)
                        )
                        .semantics {
                            testTag = CONFIRMATION_TITLE_TAG
                        },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Html(
                    html = successPage.body,
                    modifier = Modifier
                        .padding(bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
                        .semantics {
                            testTag = CONFIRMATION_BODY_TAG
                        },
                    color = MaterialTheme.colors.onBackground,
                    urlSpanStyle = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colors.secondary
                    )
                )
            }
            Button(
                onClick = {
                    identityViewModel.sendSucceededAnalyticsRequestForNative()
                    verificationFlowFinishable.finishWithResult(
                        IdentityVerificationSheet.VerificationFlowResult.Completed
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        testTag = CONFIRMATION_BUTTON_TAG
                    }
            ) {
                Text(text = successPage.buttonText.uppercase())
            }
        }
    }
}

internal const val CONFIRMATION_TITLE_TAG = "ConfirmationTitle"
internal const val CONFIRMATION_BUTTON_TAG = "ConfirmButton"
internal const val CONFIRMATION_BODY_TAG = "Body"
