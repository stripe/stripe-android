package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.viewmodel.IdentityViewModel

@Composable
internal fun ErrorScreen(
    identityViewModel: IdentityViewModel,
    title: String,
    modifier: Modifier = Modifier,
    message1: String? = null,
    message2: String? = null,
    topButton: ErrorScreenButton? = null,
    bottomButton: ErrorScreenButton? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                vertical = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
            )
    ) {
        val scrollState = rememberScrollState()
        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(180.dp))
            Image(
                painter = painterResource(id = R.drawable.stripe_exclamation),
                modifier = Modifier
                    .size(92.dp)
                    .align(Alignment.CenterHorizontally),
                contentDescription = stringResource(id = R.string.stripe_description_exclamation)
            )
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(id = R.dimen.stripe_item_vertical_margin),
                        bottom = 12.dp
                    )
                    .testTag(ErrorTitleTag),
                fontSize = dimensionResource(id = R.dimen.stripe_camera_permission_title_text_size).value.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            message1?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin)
                        )
                        .testTag(ErrorMessage1Tag),
                    textAlign = TextAlign.Center
                )
            }

            message2?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ErrorMessage2Tag),
                    textAlign = TextAlign.Center
                )
            }
        }

        topButton?.let { (buttonText, onClick) ->
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(
                        ErrorTopButtonTag
                    )
            ) {
                Text(text = buttonText.uppercase())
            }
        }
        bottomButton?.let { (buttonText, onClick) ->
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(
                        ErrorBottomButtonTag
                    )
            ) {
                Text(text = buttonText.uppercase())
            }
        }
    }
}

internal data class ErrorScreenButton(
    val buttonText: String,
    val onButtonClick: () -> Unit
)

internal const val ErrorTitleTag = "ConfirmationTitle"
internal const val ErrorMessage1Tag = "Message1"
internal const val ErrorMessage2Tag = "Message2"
internal const val ErrorTopButtonTag = "TopButton"
internal const val ErrorBottomButtonTag = "BottomButton"
