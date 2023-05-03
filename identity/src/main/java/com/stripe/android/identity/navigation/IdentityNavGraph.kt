package com.stripe.android.identity.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam.Companion.getDisplayName
import com.stripe.android.identity.networking.models.CollectedDataParam.Companion.toUploadDestination
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.Companion.toScanDestination
import com.stripe.android.identity.states.IdentityScanState.Companion.toUploadDestination
import com.stripe.android.identity.ui.ConfirmationScreen
import com.stripe.android.identity.ui.ConsentScreen
import com.stripe.android.identity.ui.CountryNotListedScreen
import com.stripe.android.identity.ui.DebugScreen
import com.stripe.android.identity.ui.DocSelectionScreen
import com.stripe.android.identity.ui.DocumentScanMessageRes
import com.stripe.android.identity.ui.DocumentScanScreen
import com.stripe.android.identity.ui.DocumentUploadSideInfo
import com.stripe.android.identity.ui.ErrorScreen
import com.stripe.android.identity.ui.ErrorScreenButton
import com.stripe.android.identity.ui.IdentityTopAppBar
import com.stripe.android.identity.ui.IdentityTopBarState
import com.stripe.android.identity.ui.IndividualScreen
import com.stripe.android.identity.ui.IndividualWelcomeScreen
import com.stripe.android.identity.ui.InitialLoadingScreen
import com.stripe.android.identity.ui.SelfieScanScreen
import com.stripe.android.identity.ui.UploadScreen
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel

@Composable
internal fun IdentityNavGraph(
    navController: NavHostController = rememberNavController(),
    identityViewModel: IdentityViewModel,
    fallbackUrlLauncher: FallbackUrlLauncher,
    appSettingsOpenable: AppSettingsOpenable,
    cameraPermissionEnsureable: CameraPermissionEnsureable,
    verificationFlowFinishable: VerificationFlowFinishable,
    identityScanViewModelFactory: IdentityScanViewModel.IdentityScanViewModelFactory,
    onTopBarNavigationClick: () -> Unit,
    topBarState: IdentityTopBarState,
    onNavControllerCreated: (NavController) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        onNavControllerCreated(navController)
    }
    Scaffold(
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
            screen(DocSelectionDestination.ROUTE) {
                DocSelectionScreen(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    cameraPermissionEnsureable = cameraPermissionEnsureable
                )
            }
            screen(IDScanDestination.ROUTE) {
                val identityScanViewModel: IdentityScanViewModel =
                    viewModel(factory = identityScanViewModelFactory)
                ScanDestinationEffect(
                    lifecycleOwner = it,
                    identityScanViewModel = identityScanViewModel
                )
                DocumentScanScreenContent(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    identityScanViewModel = identityScanViewModel,
                    backStackEntry = it,
                    route = IDScanDestination.ROUTE.route
                )
            }
            screen(DriverLicenseScanDestination.ROUTE) {
                val identityScanViewModel: IdentityScanViewModel =
                    viewModel(factory = identityScanViewModelFactory)
                ScanDestinationEffect(
                    lifecycleOwner = it,
                    identityScanViewModel = identityScanViewModel
                )
                DocumentScanScreenContent(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    identityScanViewModel = identityScanViewModel,
                    backStackEntry = it,
                    route = DriverLicenseScanDestination.ROUTE.route
                )
            }
            screen(PassportScanDestination.ROUTE) {
                val identityScanViewModel: IdentityScanViewModel =
                    viewModel(factory = identityScanViewModelFactory)
                ScanDestinationEffect(
                    lifecycleOwner = it,
                    identityScanViewModel = identityScanViewModel
                )
                DocumentScanScreenContent(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    identityScanViewModel = identityScanViewModel,
                    backStackEntry = it,
                    route = PassportScanDestination.ROUTE.route
                )
            }
            screen(SelfieDestination.ROUTE) {
                val identityScanViewModel: IdentityScanViewModel =
                    viewModel(factory = identityScanViewModelFactory)
                ScanDestinationEffect(
                    lifecycleOwner = it,
                    identityScanViewModel = identityScanViewModel
                )
                SelfieScanScreen(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    identityScanViewModel = identityScanViewModel
                )
            }
            screen(IDUploadDestination.ROUTE) {
                LaunchedEffect(Unit) {
                    identityViewModel.updateImageHandlerScanTypes(
                        IdentityScanState.ScanType.ID_FRONT,
                        IdentityScanState.ScanType.ID_BACK
                    )
                }
                DocumentUploadScreenContent(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    backStackEntry = it,
                    route = IDUploadDestination.ROUTE.route
                )
            }
            screen(DriverLicenseUploadDestination.ROUTE) {
                LaunchedEffect(Unit) {
                    identityViewModel.updateImageHandlerScanTypes(
                        IdentityScanState.ScanType.DL_FRONT,
                        IdentityScanState.ScanType.DL_BACK
                    )
                }
                DocumentUploadScreenContent(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    backStackEntry = it,
                    route = DriverLicenseUploadDestination.ROUTE.route
                )
            }
            screen(PassportUploadDestination.ROUTE) {
                LaunchedEffect(Unit) {
                    identityViewModel.updateImageHandlerScanTypes(
                        IdentityScanState.ScanType.PASSPORT,
                        null
                    )
                }
                DocumentUploadScreenContent(
                    navController = navController,
                    identityViewModel = identityViewModel,
                    backStackEntry = it,
                    route = PassportUploadDestination.ROUTE.route,
                    hasBack = false
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
            screen(CameraPermissionDeniedDestination.ROUTE) {
                val collectedDataParamType =
                    CameraPermissionDeniedDestination.collectedDataParamType(it)
                ErrorScreen(
                    identityViewModel = identityViewModel,
                    title = stringResource(id = R.string.stripe_camera_permission),
                    message1 = stringResource(id = R.string.stripe_grant_camera_permission_text),
                    message2 =
                    if (collectedDataParamType != CollectedDataParam.Type.INVALID) {
                        stringResource(
                            R.string.stripe_upload_file_text,
                            collectedDataParamType.getDisplayName(context)
                        )
                    } else {
                        null
                    },
                    topButton =
                    if (collectedDataParamType != CollectedDataParam.Type.INVALID) {
                        ErrorScreenButton(
                            buttonText = stringResource(id = R.string.stripe_file_upload)
                        ) {
                            identityViewModel.screenTracker.screenTransitionStart(
                                IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
                            )
                            navController.navigateTo(
                                collectedDataParamType.toUploadDestination(
                                    shouldShowTakePhoto = false,
                                    shouldShowChoosePhoto = true
                                )
                            )
                        }
                    } else {
                        null
                    },
                    bottomButton = ErrorScreenButton(
                        buttonText = stringResource(id = R.string.stripe_app_settings)
                    ) {
                        appSettingsOpenable.openAppSettings()
                        // navigate back to DocSelection, so that when user is back to the app
                        // from settings
                        // the camera permission check can be triggered again from there.
                        navController.navigateTo(DocSelectionDestination)
                    }
                )
            }
            screen(CouldNotCaptureDestination.ROUTE) {
                val scanType = CouldNotCaptureDestination.couldNotCaptureScanType(it)
                val requireLiveCapture = CouldNotCaptureDestination.requireLiveCapture(it)
                ErrorScreen(
                    identityViewModel = identityViewModel,
                    title = stringResource(id = R.string.stripe_could_not_capture_title),
                    message1 = stringResource(id = R.string.stripe_could_not_capture_body1),
                    message2 = if (scanType == IdentityScanState.ScanType.SELFIE) {
                        null
                    } else {
                        stringResource(
                            R.string.stripe_could_not_capture_body2
                        )
                    },
                    topButton = if (scanType == IdentityScanState.ScanType.SELFIE) {
                        null
                    } else {
                        ErrorScreenButton(
                            buttonText = stringResource(id = R.string.stripe_file_upload),
                        ) {
                            identityViewModel.screenTracker.screenTransitionStart(
                                IdentityAnalyticsRequestFactory.SCREEN_NAME_ERROR
                            )
                            navController.navigateTo(
                                scanType.toUploadDestination(
                                    shouldShowTakePhoto = true,
                                    shouldShowChoosePhoto = !requireLiveCapture
                                )
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
                            scanType.toScanDestination()
                        )
                    }
                )
            }
            screen(ErrorDestination.ROUTE) {
                ErrorScreen(
                    identityViewModel = identityViewModel,
                    title = ErrorDestination.errorTitle(it),
                    message1 = ErrorDestination.errorContent(it),
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

@Composable
private fun DocumentScanScreenContent(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    identityScanViewModel: IdentityScanViewModel,
    backStackEntry: NavBackStackEntry,
    route: String
) {
    DocumentScanScreen(
        navController = navController,
        identityViewModel = identityViewModel,
        identityScanViewModel = identityScanViewModel,
        frontScanType = DocumentScanDestination.frontScanType(backStackEntry),
        backScanType = DocumentScanDestination.backScanType(backStackEntry),
        shouldStartFromBack = DocumentScanDestination.shouldStartFromBack(backStackEntry),
        messageRes = DocumentScanMessageRes(
            DocumentScanDestination.frontTitleStringRes(backStackEntry),
            DocumentScanDestination.backTitleStringRes(backStackEntry),
            DocumentScanDestination.frontMessageStringRes(backStackEntry),
            DocumentScanDestination.backMessageStringRes(backStackEntry),
        ),
        collectedDataParamType = DocumentScanDestination.collectedDataParamType(backStackEntry),
        route = route,
    )
}

@Composable
private fun DocumentUploadScreenContent(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    backStackEntry: NavBackStackEntry,
    route: String,
    hasBack: Boolean = true
) {
    UploadScreen(
        navController = navController,
        identityViewModel = identityViewModel,
        collectedDataParamType = DocumentUploadDestination.collectedDataParamType(backStackEntry),
        route = route,
        titleRes = DocumentUploadDestination.titleRes(backStackEntry),
        contextRes = DocumentUploadDestination.contextRes(backStackEntry),
        frontInfo = DocumentUploadSideInfo(
            descriptionRes = DocumentUploadDestination.frontDescriptionRes(
                backStackEntry
            ),
            checkmarkContentDescriptionRes =
            DocumentUploadDestination.frontCheckMarkDescriptionRes(
                backStackEntry
            ),
            scanType = DocumentUploadDestination.frontScanType(backStackEntry)
        ),
        backInfo =
        if (hasBack) {
            DocumentUploadSideInfo(
                descriptionRes =
                DocumentUploadDestination.backDescriptionRes(
                    backStackEntry
                ),
                checkmarkContentDescriptionRes =
                DocumentUploadDestination.backCheckMarkDescriptionRes(
                    backStackEntry
                ),
                scanType = DocumentUploadDestination.backScanType(backStackEntry)
            )
        } else {
            null
        },
        shouldShowTakePhoto = DocumentUploadDestination.shouldShowTakePhoto(backStackEntry),
        shouldShowChoosePhoto = DocumentUploadDestination.shouldShowChoosePhoto(backStackEntry)
    )
}

private fun NavGraphBuilder.screen(
    route: IdentityTopLevelDestination.DestinationRoute,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route.route,
        arguments = route.arguments,
        content = content
    )
}
