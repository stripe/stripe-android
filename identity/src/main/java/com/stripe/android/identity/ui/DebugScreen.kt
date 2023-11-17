package com.stripe.android.identity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.stripe.android.identity.navigation.DebugDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.models.Requirement.Companion.nextDestination
import com.stripe.android.identity.ui.CompleteOption.FAILURE
import com.stripe.android.identity.ui.CompleteOption.FAILURE_ASYNC
import com.stripe.android.identity.ui.CompleteOption.SUCCESS
import com.stripe.android.identity.ui.CompleteOption.SUCCESS_ASYNC
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

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
                    start = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                    end = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                    top = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                    bottom = dimensionResource(id = R.dimen.stripe_page_vertical_margin)
                )
        ) {
            val context = LocalContext.current
            var proceedState by remember { mutableStateOf(LoadingButtonState.Idle) }
            val coroutineScope = rememberCoroutineScope()
            TitleSection()
            Divider(modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin)))
            CompleteWithTestDataSection { completeOption ->
                proceedState = LoadingButtonState.Disabled
                when (completeOption) {
                    SUCCESS -> {
                        coroutineScope.launch {
                            identityViewModel.verifySessionAndTransition(
                                fromRoute = DebugDestination.ROUTE.route,
                                simulateDelay = false,
                                navController = navController
                            )
                        }
                    }

                    FAILURE -> {
                        coroutineScope.launch {
                            identityViewModel.unverifySessionAndTransition(
                                fromRoute = DebugDestination.ROUTE.route,
                                simulateDelay = false,
                                navController = navController
                            )
                        }
                    }

                    SUCCESS_ASYNC -> {
                        coroutineScope.launch {
                            identityViewModel.verifySessionAndTransition(
                                fromRoute = DebugDestination.ROUTE.route,
                                simulateDelay = true,
                                navController = navController
                            )
                        }
                    }

                    FAILURE_ASYNC -> {
                        coroutineScope.launch {
                            identityViewModel.unverifySessionAndTransition(
                                fromRoute = DebugDestination.ROUTE.route,
                                simulateDelay = true,
                                navController = navController
                            )
                        }
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin)))
            FinishMobileFlowWithResultSection(verificationFlowFinishable)
            Divider(modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin)))
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
            painter = painterResource(id = R.drawable.stripe_exclamation),
            modifier = Modifier
                .width(32.dp)
                .height(32.dp)
                .padding(end = 8.dp),
            contentDescription = stringResource(id = R.string.stripe_description_exclamation),
            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
        )
        Column {
            Text(
                text = stringResource(id = R.string.stripe_test_model_title),
                style = MaterialTheme.typography.subtitle2
            )
            Text(
                text = stringResource(id = R.string.stripe_test_model_content)
            )
        }
    }
}

@Composable
internal fun CompleteWithTestDataSection(
    onClickSubmit: (CompleteOption) -> Unit
) {
    var submitState: LoadingButtonState by remember { mutableStateOf(LoadingButtonState.Disabled) }
    var completeOption: CompleteOption? by remember { mutableStateOf(null) }
    Text(
        text = stringResource(id = R.string.stripe_complete_with_test_data),
        style = MaterialTheme.typography.h4
    )
    Text(
        text = stringResource(id = R.string.stripe_complete_with_test_data_details),
        modifier = Modifier.padding(vertical = 8.dp)
    )
    CompleteOptionRow(
        content = stringResource(id = R.string.stripe_verification_success),
        selected = completeOption == SUCCESS,
        enabled = submitState != LoadingButtonState.Loading,
        testTag = TEST_TAG_SUCCESS,
        onClick = {
            completeOption = SUCCESS
            submitState = LoadingButtonState.Idle
        }
    )
    CompleteOptionRow(
        content = stringResource(id = R.string.stripe_verification_failure),
        selected = completeOption == FAILURE,
        enabled = submitState != LoadingButtonState.Loading,
        testTag = TEST_TAG_FAILURE,
        onClick = {
            completeOption = FAILURE
            submitState = LoadingButtonState.Idle
        }
    )
    CompleteOptionRow(
        content = stringResource(id = R.string.stripe_verification_success_async),
        selected = completeOption == SUCCESS_ASYNC,
        enabled = submitState != LoadingButtonState.Loading,
        testTag = TEST_TAG_SUCCESS_ASYNC,
        onClick = {
            completeOption = SUCCESS_ASYNC
            submitState = LoadingButtonState.Idle
        }
    )
    CompleteOptionRow(
        content = stringResource(id = R.string.stripe_verification_failure_async),
        selected = completeOption == FAILURE_ASYNC,
        enabled = submitState != LoadingButtonState.Loading,
        testTag = TEST_TAG_FAILURE_ASYNC,
        onClick = {
            completeOption = FAILURE_ASYNC
            submitState = LoadingButtonState.Idle
        }
    )
    LoadingButton(
        text = stringResource(id = R.string.stripe_submit),
        state = submitState,
        modifier = Modifier.testTag(TEST_TAG_SUBMIT_BUTTON),
        onClick = {
            submitState = LoadingButtonState.Loading
            onClickSubmit(requireNotNull(completeOption))
        }
    )
}

@Composable
private fun FinishMobileFlowWithResultSection(
    finishable: VerificationFlowFinishable
) {
    val failureExceptionMessage = stringResource(id = R.string.stripe_failure_from_test_mode)
    Text(
        text = stringResource(id = R.string.stripe_finish_mobile_flow),
        style = MaterialTheme.typography.h4
    )
    Text(
        text = stringResource(id = R.string.stripe_finish_mobile_flow_details),
        modifier = Modifier.padding(vertical = 8.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .testTag(TEST_TAG_CANCELLED_BUTTON),
            onClick = {
                finishable.finishWithResult(IdentityVerificationSheet.VerificationFlowResult.Canceled)
            }
        ) {
            Text(text = stringResource(id = R.string.stripe_cancelled))
        }

        Button(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .testTag(TEST_TAG_FAILED_BUTTON),
            onClick = {
                finishable.finishWithResult(
                    IdentityVerificationSheet.VerificationFlowResult.Failed(
                        Exception(failureExceptionMessage)
                    )
                )
            }
        ) {
            Text(text = stringResource(id = R.string.stripe_failed))
        }
    }
}

@Composable
private fun PreviewUserExperienceSection(
    onProceedClicked: () -> Unit
) {
    Text(
        text = stringResource(id = R.string.stripe_preview_user_experience),
        style = MaterialTheme.typography.h4
    )
    Text(
        text = stringResource(id = R.string.stripe_preview_user_experience_details),
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_PROCEED_BUTTON),
        onClick = onProceedClicked
    ) {
        Text(text = stringResource(id = R.string.stripe_proceed))
    }
}

@Composable
private fun CompleteOptionRow(
    content: String,
    selected: Boolean,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .testTag(testTag)
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = null
        )

        Text(
            text = content,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.button
        )
    }
}

internal enum class CompleteOption {
    SUCCESS, FAILURE, SUCCESS_ASYNC, FAILURE_ASYNC
}

internal const val TEST_TAG_CANCELLED_BUTTON = "Cancelled"
internal const val TEST_TAG_FAILED_BUTTON = "Failed"
internal const val TEST_TAG_PROCEED_BUTTON = "Proceed"
internal const val TEST_TAG_SUBMIT_BUTTON = "Submit"
internal const val TEST_TAG_SUCCESS = "success"
internal const val TEST_TAG_SUCCESS_ASYNC = "success_async"
internal const val TEST_TAG_FAILURE = "failure"
internal const val TEST_TAG_FAILURE_ASYNC = "failure_async"
