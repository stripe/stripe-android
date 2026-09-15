package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONFIRMATION
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networked.NetworkedIdentitySheetHost
import com.stripe.android.identity.networked.NetworkedIdentityViewModel
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.utils.collectAsState

@OptIn(ExperimentalMaterialApi::class)
@Suppress("LongMethod")
@Composable
internal fun ConfirmationScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    verificationFlowFinishable: VerificationFlowFinishable,
    networkedIdentityViewModel: NetworkedIdentityViewModel?
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current

    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
            navController.navigateToErrorScreenWithDefaultValues(context)
        }
    ) { verificationPage ->
        val successPage = remember { verificationPage.success }
        val savedItems = listOfNotNull(
            stringResource(R.string.stripe_identity_link_saved_item_id),
            stringResource(R.string.stripe_identity_link_saved_item_selfie).takeIf { verificationPage.requireSelfie() }
        )
        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = SCREEN_NAME_CONFIRMATION
        )
        NetworkedIdentitySheetHost(viewModel = networkedIdentityViewModel, savedItems = savedItems) {
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
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.stripe_clock_icon),
                            modifier = Modifier.size(26.dp),
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
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    BottomSheetHTML(
                        html = successPage.body,
                        bottomSheets = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
                            .semantics {
                                testTag = CONFIRMATION_BODY_TAG
                            },
                        color = MaterialTheme.colors.onBackground,
                        style = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center),
                        urlSpanStyle = SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colors.secondary
                        )
                    )

                    networkedIdentityViewModel?.let { viewModel ->
                        val entry by viewModel.entry.collectAsState()
                        val shared by viewModel.shared.collectAsState()
                        val saved by viewModel.saved.collectAsState()
                        LaunchedEffect(viewModel) { viewModel.refreshEntry() }
                        // A reused ID is already in Link, so only the thank-you remains.
                        if (entry.linkAvailable && !shared) {
                            LinkSaveCard(
                                items = savedItems,
                                accountEmail = entry.accountEmail,
                                saved = saved,
                                onSave = viewModel::startSave,
                            )
                        }
                    }
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
}

/** Networked Identity: offers saving this verification's ID to Link. Saving only starts from the tap. */
@Composable
private fun LinkSaveCard(items: List<String>, accountEmail: String?, saved: Boolean, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .semantics { testTag = LINK_SAVE_CARD_TAG }
    ) {
        Text(
            text = stringResource(R.string.stripe_identity_link_save_card_title),
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.SemiBold
        )
        accountEmail?.let { email ->
            Text(
                text = stringResource(R.string.stripe_identity_link_account_with_email, email),
                style = MaterialTheme.typography.body2,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { testTag = LINK_SAVE_ACCOUNT_TAG }
            )
        }
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        if (saved) {
            Text(
                text = stringResource(R.string.stripe_identity_link_saved_card),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { testTag = LINK_SAVED_TAG }
            )
        } else {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { testTag = LINK_SAVE_BUTTON_TAG }
            ) {
                Text(text = stringResource(R.string.stripe_identity_link_save_button))
            }
        }
    }
}

internal const val CONFIRMATION_TITLE_TAG = "ConfirmationTitle"
internal const val CONFIRMATION_BUTTON_TAG = "ConfirmButton"
internal const val CONFIRMATION_BODY_TAG = "Body"
internal const val LINK_SAVE_CARD_TAG = "LinkSaveCard"
internal const val LINK_SAVE_ACCOUNT_TAG = "LinkSaveAccount"
internal const val LINK_SAVE_BUTTON_TAG = "LinkSaveButton"
internal const val LINK_SAVED_TAG = "LinkSaved"
