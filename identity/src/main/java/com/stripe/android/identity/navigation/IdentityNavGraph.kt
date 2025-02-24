package com.stripe.android.identity.navigation

import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.ui.BottomSheet
import com.stripe.android.identity.ui.ConfirmationScreen
import com.stripe.android.identity.ui.ConsentScreen
import com.stripe.android.identity.ui.CountryNotListedScreen
import com.stripe.android.identity.ui.DebugScreen
import com.stripe.android.identity.ui.DocWarmupScreen
import com.stripe.android.identity.ui.DocumentScanScreen
import com.stripe.android.identity.ui.ErrorScreen
import com.stripe.android.identity.ui.ErrorScreenButton
import com.stripe.android.identity.ui.IdentityTopAppBar
import com.stripe.android.identity.ui.IdentityTopBarState
import com.stripe.android.identity.ui.IndividualScreen
import com.stripe.android.identity.ui.IndividualWelcomeScreen
import com.stripe.android.identity.ui.InitialLoadingScreen
import com.stripe.android.identity.ui.OTPScreen
import com.stripe.android.identity.ui.SelfieScanScreen
import com.stripe.android.identity.ui.SelfieWarmupScreen
import com.stripe.android.identity.ui.UploadScreen
import com.stripe.android.identity.viewmodel.BottomSheetViewModel
import com.stripe.android.identity.viewmodel.DocumentScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.SelfieScanViewModel
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

@Composable
@ExperimentalMaterialApi
internal fun IdentityNavGraph(
    navController: NavHostController = rememberNavController(),
    identityViewModel: IdentityViewModel,
    fallbackUrlLauncher: FallbackUrlLauncher,
    appSettingsOpenable: AppSettingsOpenable,
    cameraPermissionEnsureable: CameraPermissionEnsureable,
    verificationFlowFinishable: VerificationFlowFinishable,
    documentScanViewModelFactory: DocumentScanViewModel.DocumentScanViewModelFactory,
    selfieScanViewModelFactory: SelfieScanViewModel.SelfieScanViewModelFactory,
    onTopBarNavigationClick: () -> Unit,
    topBarState: IdentityTopBarState,
    onNavControllerCreated: (NavController) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        onNavControllerCreated(navController)
    }
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            IdentityTopAppBar(topBarState, onTopBarNavigationClick)
        }
    ) { contentPadding ->
        NavHost(
            navController = navController,
            modifier = Modifier.padding(contentPadding),
            startDestination = InitialLoadingDestination.destinationRoute.route
        ) {
            screen(DebugDestination.ROUTE) {
                DebugScreen(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    verificationFlowFinishable = verificationFlowFinishable
                )
            }

            screen(InitialLoadingDestination.ROUTE) {
                InitialLoadingScreen(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    fallbackUrlLauncher = fallbackUrlLauncher
                )
            }
            screen(IndividualWelcomeDestination.ROUTE) {
                IndividualWelcomeScreen(
                    navController = navController,
                    identityViewModel = identityViewModel
                )
            }
            screen(ConsentDestination.ROUTE) {
                ConsentScreen(
                    navController = navController,
                    identityViewModel = identityViewModel
                )
            }
            screen(DocWarmupDestination.ROUTE) {
                DocWarmupScreen(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    cameraPermissionEnsureable = cameraPermissionEnsureable
                )
            }
            screen(DocumentScanDestination.ROUTE) {
                val documentScanViewModel: DocumentScanViewModel =
                    viewModel(factory = documentScanViewModelFactory)
                ScanDestinationEffect(
                    lifecycleOwner = it,
                    identityScanViewModel = documentScanViewModel
                )
                DocumentScanScreen(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    documentScanViewModel = documentScanViewModel,
                )
            }
            screen(SelfieWarmupDestination.ROUTE) {
                SelfieWarmupScreen(
                    navController = navController,
                    identityViewModel = identityViewModel
                )
            }
            screen(SelfieDestination.ROUTE) {
                val selfieScanViewModel: SelfieScanViewModel =
                    viewModel(factory = selfieScanViewModelFactory)

                ScanDestinationEffect(
                    lifecycleOwner = it,
                    identityScanViewModel = selfieScanViewModel
                )
                SelfieScanScreen(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    selfieScanViewModel = selfieScanViewModel
                )
            }
            screen(DocumentUploadDestination.ROUTE) {
                UploadScreen(
                    navController = navController,
                    identityViewModel = identityViewModel,
                )
            }
            screen(IndividualDestination.ROUTE) {
                IndividualScreen(
                    navController = navController,
                    identityViewModel = identityViewModel
                )
            }
            screen(ConfirmationDestination.ROUTE) {
                ConfirmationScreen(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    verificationFlowFinishable = verificationFlowFinishable
                )
            }
            screen(CountryNotListedDestination.ROUTE) {
                CountryNotListedScreen(
                    isMissingID = CountryNotListedDestination.isMissingId(it),
                    navController = navController,
                    identityViewModel = identityViewModel,
                    verificationFlowFinishable = verificationFlowFinishable
                )
            }
            screen(OTPDestination.ROUTE) {
                OTPScreen(navController = navController, identityViewModel = identityViewModel)
            }
            screen(CameraPermissionDeniedDestination.ROUTE) {
                val requireLiveCapture =
                    identityViewModel.verificationPage.value?.data?.documentCapture?.requireLiveCapture ?: false

                ErrorScreen(
                    identityViewModel = identityViewModel,
                    title = stringResource(id = R.string.stripe_camera_permission),
                    message1 = stringResource(id = R.string.stripe_grant_camera_permission_text),
                    message2 =
                    if (!requireLiveCapture) {
                        stringResource(
                            R.string.stripe_upload_file_generic_text
                        )
                    } else {
                        null
                    },
                    topButton =
                    if (!requireLiveCapture) {
                        ErrorScreenButton(
                            buttonText = stringResource(id = R.string.stripe_file_upload)
                        ) {
                            identityViewModel.screenTracker.screenTransitionStart(
                                IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
                            )
                            navController.navigateTo(
                                DocumentUploadDestination
                            )
                        }
                    } else {
                        null
                    },
                    bottomButton = ErrorScreenButton(
                        buttonText = stringResource(id = R.string.stripe_app_settings)
                    ) {
                        appSettingsOpenable.openAppSettings()
                        // navigate back to DocWarmup, so that when user is back to the app
                        // from settings
                        // the camera permission check can be triggered again from there.
                        navController.navigateTo(DocWarmupDestination)
                    }
                )
            }
            screen(CouldNotCaptureDestination.ROUTE) {
                val fromSelfie = CouldNotCaptureDestination.fromSelfie(it)
                ErrorScreen(
                    identityViewModel = identityViewModel,
                    title = stringResource(id = R.string.stripe_could_not_capture_title),
                    message1 = stringResource(id = R.string.stripe_could_not_capture_body1),
                    message2 = if (fromSelfie) {
                        null
                    } else {
                        stringResource(
                            R.string.stripe_could_not_capture_body2
                        )
                    },
                    topButton = if (fromSelfie) {
                        null
                    } else {
                        ErrorScreenButton(
                            buttonText = stringResource(id = R.string.stripe_upload_a_photo),
                        ) {
                            identityViewModel.screenTracker.screenTransitionStart(
                                IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
                            )
                            navController.navigateTo(
                                DocumentUploadDestination
                            )
                        }
                    },
                    bottomButton =
                    ErrorScreenButton(
                        buttonText = stringResource(id = R.string.stripe_try_again)
                    ) {
                        identityViewModel.screenTracker.screenTransitionStart(
                            IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
                        )
                        navController.navigateTo(
                            if (fromSelfie) {
                                SelfieDestination
                            } else {
                                DocumentScanDestination
                            }
                        )
                    }
                )
            }
            screen(ErrorDestination.ROUTE) {
                Log.d(
                    ErrorDestination.TAG,
                    "About to show error screen with error caused by ${identityViewModel.errorCause.value?.cause}"
                )
                ErrorScreen(
                    identityViewModel = identityViewModel,
                    title = ErrorDestination.errorTitle(it),
                    message1 = ErrorDestination.errorContent(it),
                    topButton = ErrorDestination.continueButtonContext(it)
                        ?.let { (topButtonText, topButtonRequirement) ->
                            ErrorScreenButton(buttonText = topButtonText) {
                                coroutineScope.launch {
                                    identityViewModel.postVerificationPageDataForForceConfirm(
                                        requirementToForceConfirm = topButtonRequirement,
                                        navController = navController
                                    )
                                }
                            }
                        },
                    bottomButton = ErrorScreenButton(
                        buttonText = ErrorDestination.backButtonText(it)
                    ) {
                        identityViewModel.screenTracker.screenTransitionStart(
                            IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
                        )
                        if (ErrorDestination.shouldFail(it)) {
                            verificationFlowFinishable.finishWithResult(
                                IdentityVerificationSheet.VerificationFlowResult.Failed(
                                    requireNotNull(identityViewModel.errorCause.value) {
                                        "cause of error is null"
                                    }
                                )
                            )
                        } else {
                            val destination = ErrorDestination.backButtonDestination(it)
                            if (destination == ErrorDestination.UNEXPECTED_ROUTE) {
                                navController.navigateTo(ConsentDestination)
                            } else {
                                var shouldContinueNavigateUp = true
                                while (
                                    shouldContinueNavigateUp &&
                                    navController.currentDestination?.route?.toRouteBase() !=
                                    destination
                                ) {
                                    shouldContinueNavigateUp =
                                        navController.clearDataAndNavigateUp(identityViewModel)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@ExperimentalMaterialApi
/**
 * Built a composable screen with ModalBottomSheetLayout
 */
private fun NavGraphBuilder.screen(
    route: IdentityTopLevelDestination.DestinationRoute,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route.route,
        arguments = route.arguments
    ) { navBackStackEntry ->
        val bottomSheetViewModel = viewModel<BottomSheetViewModel>()
        val bottomSheetState by bottomSheetViewModel.bottomSheetState.collectAsState()
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden
        )

        // Required when bottomsheet is dismissed by swiping down or clicking outside, need to
        // update the state inside viewmodel
        LaunchedEffect(modalSheetState.isVisible) {
            if (modalSheetState.isVisible.not()) {
                bottomSheetViewModel.dismissBottomSheet()
            }
        }

        LaunchedEffect(bottomSheetState.shouldShow) {
            if (bottomSheetState.shouldShow) {
                modalSheetState.show()
            } else {
                modalSheetState.hide()
            }
        }

        ModalBottomSheetLayout(
            sheetContent = {
                BottomSheet()
            },
            sheetState = modalSheetState,
            sheetGesturesEnabled = true,
            sheetShape = RoundedCornerShape(
                topStart = MaterialTheme.stripeShapes.cornerRadius.dp,
                topEnd = MaterialTheme.stripeShapes.cornerRadius.dp,
            )
        ) {
            content(navBackStackEntry)
        }
    }
}
