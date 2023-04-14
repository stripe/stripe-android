package com.stripe.android.identity.ui

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
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.models.Requirement.Companion.nextDestination
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.text.Html

/**
 * Screen to show debug options for test mode verification.
 */
@Composable
internal fun DebugScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    verificationFlowFinishable: VerificationFlowFinishable
) {
    CheckVerificationPageAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) { verificationPage ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = dimensionResource(id = R.dimen.page_horizontal_margin),
                    end = dimensionResource(id = R.dimen.page_horizontal_margin),
                    top = dimensionResource(id = R.dimen.page_vertical_margin),
                    bottom = dimensionResource(id = R.dimen.page_vertical_margin)
                )
        ) {
            val context = LocalContext.current

            TitleSection()
            Divider(
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.item_vertical_margin))
            )
            FinishMobileFlowWithResultSection(verificationFlowFinishable)
            Divider(
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.item_vertical_margin))
            )
            PreviewUserExperienceSection {
                navController.navigateTo(
                    verificationPage.requirements.missing.nextDestination(
                        context
                    )
                )
            }
        }
    }
}

@Composable
private fun TitleSection() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_exclamation),
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .padding(end = 8.dp),
            contentDescription = stringResource(id = R.string.description_exclamation),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
        )
        Column {
            Text(
                text = stringResource(id = R.string.test_model_title),
                style = MaterialTheme.typography.subtitle2
            )
            Text(
                text = stringResource(id = R.string.test_model_content)
            )
        }
    }
}

@Composable
private fun FinishMobileFlowWithResultSection(
    finishable: VerificationFlowFinishable
) {
    val failureExceptionMessage = stringResource(id = R.string.failure_from_test_mode)
    Text(
        text = stringResource(id = R.string.finish_mobile_flow),
        style = MaterialTheme.typography.h4
    )
    Html(
        html = stringResource(id = R.string.finish_mobile_flow_details),
        modifier = Modifier.padding(vertical = 8.dp)
    )

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_COMPLETE_BUTTON),
        onClick = {
            finishable.finishWithResult(IdentityVerificationSheet.VerificationFlowResult.Completed)
        }
    ) {
        Text(text = stringResource(id = R.string.completed))
    }

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_CANCELLED_BUTTON),
        onClick = {
            finishable.finishWithResult(IdentityVerificationSheet.VerificationFlowResult.Canceled)
        }
    ) {
        Text(text = stringResource(id = R.string.cancelled))
    }

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_FAILED_BUTTON),
        onClick = {
            finishable.finishWithResult(
                IdentityVerificationSheet.VerificationFlowResult.Failed(
                    Exception(failureExceptionMessage)
                )
            )
        }
    ) {
        Text(text = stringResource(id = R.string.failed))
    }
}

@Composable
private fun PreviewUserExperienceSection(
    onProceedClicked: () -> Unit
) {
    Text(
        text = stringResource(id = R.string.preview_user_experience),
        style = MaterialTheme.typography.h4
    )
    Text(
        text = stringResource(id = R.string.preview_user_experience_details),
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_PROCEED_BUTTON),
        onClick = onProceedClicked
    ) {
        Text(text = stringResource(id = R.string.proceed))
    }
}

internal const val TEST_TAG_COMPLETE_BUTTON = "Completed"
internal const val TEST_TAG_CANCELLED_BUTTON = "Cancelled"
internal const val TEST_TAG_FAILED_BUTTON = "Failed"
internal const val TEST_TAG_PROCEED_BUTTON = "Proceed"
