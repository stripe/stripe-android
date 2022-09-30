package com.stripe.android.identity.ui

import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.composethemeadapter.MdcTheme
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPage.Companion.isUnsupportedClient
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.utils.setHtmlString

internal const val TITLE_TAG = "Title"
internal const val TIME_ESTIMATE_TAG = "TimeEstimate"
internal const val PRIVACY_POLICY_TAG = "PrivacyPolicy"
internal const val DIVIDER_TAG = "divider"
internal const val BODY_TAG = "Body"
internal const val ACCEPT_BUTTON_TAG = "Accept"
internal const val DECLINE_BUTTON_TAG = "Decline"
internal const val LOADING_SCREEN_TAG = "Loading"
internal const val SCROLLABLE_COLUMN_TAG = "ScrollableColumn"

@Composable
internal fun ConsentScreen(
    verificationState: Resource<VerificationPage>,
    onMerchantViewCreated: (ImageView) -> Unit,
    onSuccess: (VerificationPage) -> Unit,
    onFallbackUrl: (String) -> Unit,
    onError: (Throwable) -> Unit,
    onConsentAgreed: (Boolean) -> Unit,
    onConsentDeclined: (Boolean) -> Unit
) {
    MdcTheme {
        when (verificationState.status) {
            Status.SUCCESS -> {
                val verificationPage = remember { requireNotNull(verificationState.data) }
                LaunchedEffect(Unit) {
                    onSuccess(verificationPage)
                }
                if (verificationPage.isUnsupportedClient()) {
                    LaunchedEffect(Unit) {
                        onFallbackUrl(verificationPage.fallbackUrl)
                    }
                } else {
                    SuccessUI(
                        verificationPage,
                        onMerchantViewCreated,
                        onConsentAgreed,
                        onConsentDeclined
                    )
                }
            }

            Status.ERROR -> {
                LaunchedEffect(Unit) {
                    onError(
                        verificationState.throwable
                            ?: IllegalStateException("Failed to get verificationPage")
                    )
                }
            }

            Status.LOADING -> {
                LoadingUI()
            }
        }
    }
}

@Composable
private fun SuccessUI(
    verificationPage: VerificationPage,
    onMerchantViewCreated: (ImageView) -> Unit,
    onConsentAgreed: (Boolean) -> Unit,
    onConsentDeclined: (Boolean) -> Unit
) {
    val requireSelfie = verificationPage.requireSelfie()
    val consentPage = verificationPage.biometricConsent

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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .semantics {
                    testTag = SCROLLABLE_COLUMN_TAG
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AndroidView(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp),
                    factory = { ImageView(it) },
                    update = onMerchantViewCreated
                )

                Image(
                    painter = painterResource(id = R.drawable.plus_icon),
                    modifier = Modifier
                        .width(16.dp)
                        .height(16.dp),
                    contentDescription = stringResource(id = R.string.description_plus)
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_stripe_square_32),
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp),
                    contentDescription = stringResource(id = R.string.description_stripe_logo)
                )
            }
            Text(
                text = consentPage.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = dimensionResource(
                            id = R.dimen.item_vertical_margin
                        )
                    )
                    .semantics {
                        testTag = TITLE_TAG
                    },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            consentPage.timeEstimate?.let { timeEstimateString ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.time_estimate_icon),
                        contentDescription = stringResource(id = R.string.description_time_estimate)
                    )
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp)
                            .semantics {
                                testTag = TIME_ESTIMATE_TAG
                            },
                        factory = { TextView(it) },
                        update = {
                            it.text = timeEstimateString
                        }
                    )
                }
            }
            consentPage.privacyPolicy?.let { privacyPolicyString ->
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensionResource(id = R.dimen.item_vertical_margin))
                        .semantics {
                            testTag = PRIVACY_POLICY_TAG
                        },
                    factory = { TextView(it) },
                    update = {
                        it.setHtmlString(privacyPolicyString)
                    }
                )
            }
            if (consentPage.timeEstimate != null && consentPage.privacyPolicy != null) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensionResource(id = R.dimen.item_vertical_margin))
                        .semantics {
                            testTag = DIVIDER_TAG
                        }
                )
            }

            // TODO(ccen-stripe): replace with [Html]
            AndroidView(
                modifier = Modifier
                    .padding(bottom = dimensionResource(id = R.dimen.item_vertical_margin))
                    .semantics {
                        testTag = BODY_TAG
                    },
                factory = { TextView(it) },
                update = {
                    it.setHtmlString(consentPage.body)
                }
            )
        }

        var acceptState by remember { mutableStateOf(LoadingButtonState.Idle) }
        var declineState by remember { mutableStateOf(LoadingButtonState.Idle) }
        var scrolledToBottom by remember { mutableStateOf(false) }

        if (scrollState.value == scrollState.maxValue) {
            scrolledToBottom = true
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
            onConsentAgreed(requireSelfie)
        }

        LoadingButton(
            modifier = Modifier
                .semantics { testTag = DECLINE_BUTTON_TAG },
            text = consentPage.declineButtonText.uppercase(),
            state = declineState
        ) {
            acceptState = LoadingButtonState.Disabled
            declineState = LoadingButtonState.Loading
            onConsentDeclined(requireSelfie)
        }
    }
}

@Composable
private fun LoadingUI() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTag = LOADING_SCREEN_TAG
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(bottom = 32.dp))
        Text(text = stringResource(id = R.string.loading), fontSize = 24.sp)
    }
}
