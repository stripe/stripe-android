package com.stripe.android.identity.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.OTPDestination
import com.stripe.android.identity.navigation.navigateOnVerificationPageData
import com.stripe.android.identity.navigation.navigateToFinalErrorScreen
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.OTPViewModel
import com.stripe.android.identity.viewmodel.OTPViewState
import com.stripe.android.identity.viewmodel.OTPViewState.ErrorOTP
import com.stripe.android.identity.viewmodel.OTPViewState.InputtingOTP
import com.stripe.android.identity.viewmodel.OTPViewState.RequestingCannotVerify
import com.stripe.android.identity.viewmodel.OTPViewState.RequestingCannotVerifySuccess
import com.stripe.android.identity.viewmodel.OTPViewState.RequestingError
import com.stripe.android.identity.viewmodel.OTPViewState.RequestingOTP
import com.stripe.android.identity.viewmodel.OTPViewState.SubmittingOTP
import com.stripe.android.uicore.elements.OTPElementUI
import kotlinx.coroutines.launch

/**
 * Screen to collect an OTP from user's phone number or email address
 */
@Composable
internal fun OTPScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    otpViewModelFactory: ViewModelProvider.Factory = OTPViewModel.Factory(
        identityRepository = identityViewModel.identityRepository,
        verificationArgs = identityViewModel.verificationArgs
    )
) {
    CheckVerificationPageAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) { verificationPage ->
        val otpViewModel: OTPViewModel = viewModel(
            factory = otpViewModelFactory
        )

        val viewState by otpViewModel.viewState.collectAsState()
        val otpStaticPage = requireNotNull(verificationPage.phoneOtp)
        val focusRequester = remember { FocusRequester() }

        OTPViewStateEffect(
            viewState = viewState,
            navController = navController,
            identityViewModel = identityViewModel,
            viewModel = otpViewModel,
            focusRequester = focusRequester
        )

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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = otpStaticPage.title,
                    style = MaterialTheme.typography.h4,
                    modifier = Modifier
                        .testTag(OTP_TITLE_TAG)
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
                )

                Text(
                    text = otpStaticPage.body.replace(
                        PHONE_NUMBER_PATTERN,
                        otpStaticPage.redactedPhoneNumber
                            ?: identityViewModel.collectedData.value.phone?.phoneNumber?.takeLast(4)
                            ?: EMPTY_PHONE_NUMBER
                    ),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
                        .testTag(OTP_BODY_TAG)
                )

                OTPElementUI(
                    enabled = (
                        viewState == InputtingOTP || viewState == ErrorOTP || viewState is RequestingCannotVerifySuccess
                        ),
                    element = otpViewModel.otpElement,
                    modifier = Modifier
                        .padding(vertical = dimensionResource(id = R.dimen.stripe_item_vertical_margin))
                        .testTag(OTP_ELEMENT_TAG),
                    focusRequester = focusRequester
                )

                if (viewState == ErrorOTP) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = dimensionResource(id = R.dimen.stripe_item_vertical_margin)
                            ),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.stripe_warning_icon),
                            contentDescription = stringResource(id = R.string.stripe_description_camera),
                            modifier = Modifier.padding(end = 5.dp),
                            tint = MaterialTheme.colors.error
                        )
                        Text(
                            text = otpStaticPage.errorOtpMessage,
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.testTag(OTP_ERROR_TAG)
                        )
                    }
                }

                if (viewState is SubmittingOTP) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(id = R.dimen.stripe_item_vertical_margin)),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            LoadingTextButton(
                modifier = Modifier.testTag(OTP_RESEND_BUTTON_TAG),
                text = otpStaticPage.resendButtonText.uppercase(),
                state = when (viewState) {
                    is SubmittingOTP -> LoadingButtonState.Disabled
                    RequestingOTP -> LoadingButtonState.Loading
                    RequestingCannotVerify -> LoadingButtonState.Disabled
                    is RequestingError -> LoadingButtonState.Disabled
                    else -> LoadingButtonState.Idle
                }
            ) {
                otpViewModel.generatePhoneOtp()
            }

            LoadingTextButton(
                modifier = Modifier
                    .testTag(OTP_CANNOT_VERIFY_BUTTON_TAG),
                text = otpStaticPage.cannotVerifyButtonText.uppercase(),
                state = when (viewState) {
                    is SubmittingOTP -> LoadingButtonState.Disabled
                    RequestingOTP -> LoadingButtonState.Disabled
                    RequestingCannotVerify -> LoadingButtonState.Loading
                    is RequestingError -> LoadingButtonState.Disabled
                    else -> LoadingButtonState.Idle
                }
            ) {
                otpViewModel.onCannotVerifyPhoneOtpClicked()
            }
        }
    }
}

@Composable
private fun OTPViewStateEffect(
    viewState: OTPViewState?,
    navController: NavController,
    identityViewModel: IdentityViewModel,
    viewModel: OTPViewModel,
    focusRequester: FocusRequester
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewState) {
        when (viewState) {
            is InputtingOTP -> {
                focusRequester.requestFocus()
            }

            is SubmittingOTP -> {
                identityViewModel.postVerificationPageDataForOTP(
                    otp = viewState.otp,
                    navController = navController,
                    onMissingOtp = { viewModel.onInputErrorOtp() }
                )
            }

            is ErrorOTP -> {
                focusRequester.requestFocus()
            }

            is RequestingError -> {
                postErrorAndNavigateToFinalErrorScreen(
                    identityViewModel,
                    navController,
                    context,
                    viewState.cause
                )
            }

            is RequestingCannotVerifySuccess -> {
                val verificationPageData = viewState.verificationPageData
                // requirements.errors might contain Phone Verification declined
                // requirements.missings might contain document related fields
                // screen transition will happen for either case
                identityViewModel.updateStatesWithVerificationPageData(
                    fromRoute = OTPDestination.ROUTE.route,
                    newVerificationPageData = verificationPageData,
                    navController = navController
                ) {
                    // If we reach this point, it's possible that user needs to continue collecting
                    // document or individual fields, navigate accordingly.
                    navController.navigateOnVerificationPageData(
                        verificationPageData = verificationPageData,
                        onMissingOtp = {
                            postErrorAndNavigateToFinalErrorScreen(
                                identityViewModel,
                                navController,
                                context,
                                IllegalStateException("Sending CannotVerify receives missing otp")
                            )
                        },
                        onMissingBack = {
                            postErrorAndNavigateToFinalErrorScreen(
                                identityViewModel,
                                navController,
                                context,
                                IllegalStateException("Sending CannotVerify receives missing back")
                            )
                        },
                        onReadyToSubmit = {
                            coroutineScope.launch {
                                identityViewModel.submitAndNavigate(
                                    navController = navController,
                                    fromRoute = OTPDestination.ROUTE.route
                                )
                            }
                        }
                    )
                }
            }

            else -> {} // no-op
        }
    }

    DisposableEffect(Unit) {
        viewModel.initialize()
        onDispose {
            viewModel.resetViewState()
        }
    }
}

private fun postErrorAndNavigateToFinalErrorScreen(
    identityViewModel: IdentityViewModel,
    navController: NavController,
    context: Context,
    cause: Throwable
) {
    identityViewModel.errorCause.postValue(cause)
    navController.navigateToFinalErrorScreen(context)
}

private const val EMPTY_PHONE_NUMBER = ""
internal const val PHONE_NUMBER_PATTERN = "{phone_number}"
internal const val OTP_TITLE_TAG = "OtpTitleTag"
internal const val OTP_BODY_TAG = "OtpBodyTag"
internal const val OTP_ELEMENT_TAG = "OtpElementTag"
internal const val OTP_ERROR_TAG = "OtpErrorTag"
internal const val OTP_RESEND_BUTTON_TAG = "OtpResendButtonTag"
internal const val OTP_CANNOT_VERIFY_BUTTON_TAG = "OtpCannotVerifyButtonTag"
