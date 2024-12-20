package com.stripe.android.identity.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_LIVE_CAPTURE
import com.stripe.android.identity.camera.DocumentScanCameraManager
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.navigation.navigateToFinalErrorScreen
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.Companion.isFront
import com.stripe.android.identity.states.IdentityScanState.Companion.isNullOrFront
import com.stripe.android.identity.utils.startScanning
import com.stripe.android.identity.viewmodel.DocumentScanViewModel
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

internal const val CONTINUE_BUTTON_TAG = "Continue"
internal const val SCAN_TITLE_TAG = "Title"
internal const val SCAN_MESSAGE_TAG = "Message"
internal const val CHECK_MARK_TAG = "CheckMark"
internal const val VIEW_FINDER_ASPECT_RATIO = 1f

@Composable
internal fun DocumentScanScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    documentScanViewModel: DocumentScanViewModel,
    verificationFlowFinishable: VerificationFlowFinishable,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember {
        DocumentScanCameraManager(
            context = context
        ) { cause ->
            identityViewModel.identityAnalyticsRequestFactory.cameraError(
                scanType = IdentityScanState.ScanType.DOC_FRONT,
                throwable = IllegalStateException(cause)
            )
        }
    }

    CheckVerificationPageModelFilesAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) { pageAndModelFiles ->

        val targetScanType by documentScanViewModel.targetScanTypeFlow.collectAsState()

        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = SCREEN_NAME_LIVE_CAPTURE
        )

        // run once to initialize
        LaunchedEffect(Unit) {
            documentScanViewModel.initializeScanFlowAndUpdateState(pageAndModelFiles, cameraManager)
        }
        val documentScannerState by documentScanViewModel.scannerState.collectAsState()
        val feedback by documentScanViewModel.scanFeedback.collectAsState()
        // TODO: Maybe do another one similar to the above 2
        // When you get an event that an error happened, do the navController.navigate to final error

        LiveCaptureLaunchedEffect(
            scannerState = documentScannerState,
            identityScanViewModel = documentScanViewModel,
            identityViewModel = identityViewModel,
            lifecycleOwner = lifecycleOwner,
            verificationPage = pageAndModelFiles.page,
            navController = navController
        )

        // UX based on documentScannerState
        when (documentScannerState) {
            IdentityScanViewModel.State.Initializing -> {
                LoadingScreen()
            }

            else -> { // can be Scanning or Scanned
                DocumentCaptureScreen(
                    documentScannerState,
                    feedback,
                    targetScanType,
                    documentScanViewModel,
                    identityViewModel,
                    lifecycleOwner,
                    cameraManager,
                ) {
                    coroutineScope.launch {
                        identityViewModel.collectDataForDocumentScanScreen(
                            navController = navController,
                            isFront = requireNotNull(targetScanType) {
                                "targetScanType is still null"
                            }.isFront()
                        ) {
                            startScanning(
                                scanType = IdentityScanState.ScanType.DOC_BACK,
                                identityViewModel = identityViewModel,
                                identityScanViewModel = documentScanViewModel,
                                lifecycleOwner = lifecycleOwner
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentCaptureScreen(
    documentScannerState: IdentityScanViewModel.State,
    @StringRes feedback: Int,
    targetScanType: IdentityScanState.ScanType?,
    identityScanViewModel: IdentityScanViewModel,
    identityViewModel: IdentityViewModel,
    lifecycleOwner: LifecycleOwner,
    cameraManager: IdentityCameraManager,
    onContinueClick: () -> Unit
) {
    val collectedData by identityViewModel.collectedData.collectAsState()
    LaunchedEffect(Unit) {
        val shouldStartFromBack = collectedData.idDocumentFront != null
        if (shouldStartFromBack) {
            startScanning(
                scanType = IdentityScanState.ScanType.DOC_BACK,
                identityViewModel = identityViewModel,
                identityScanViewModel = identityScanViewModel,
                lifecycleOwner = lifecycleOwner
            )
        } else {
            startScanning(
                scanType = IdentityScanState.ScanType.DOC_FRONT,
                identityViewModel = identityViewModel,
                identityScanViewModel = identityScanViewModel,
                lifecycleOwner = lifecycleOwner
            )
        }
    }

    val title = if (targetScanType.isNullOrFront()) {
        stringResource(id = R.string.stripe_front_of_id)
    } else {
        stringResource(id = R.string.stripe_back_of_id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
            )
    ) {
        var loadingButtonState by remember(documentScannerState) {
            mutableStateOf(
                if (documentScannerState is IdentityScanViewModel.State.Scanned) {
                    LoadingButtonState.Idle
                } else {
                    LoadingButtonState.Disabled
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        testTag = SCAN_TITLE_TAG
                    },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = feedback),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(
                        top = dimensionResource(id = R.dimen.stripe_item_vertical_margin),
                        bottom = 48.dp
                    )
                    .semantics {
                        testTag = SCAN_MESSAGE_TAG
                    },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            CameraViewFinder(
                shouldShowFinished =
                documentScannerState is IdentityScanViewModel.State.Scanned,
                cameraManager = cameraManager
            )
        }
        LoadingButton(
            modifier = Modifier.testTag(CONTINUE_BUTTON_TAG),
            text = stringResource(id = R.string.stripe_kontinue).uppercase(),
            state = loadingButtonState
        ) {
            loadingButtonState = LoadingButtonState.Loading
            onContinueClick()
        }
    }
}

@Composable
private fun CameraViewFinder(
    shouldShowFinished: Boolean,
    cameraManager: IdentityCameraManager
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(VIEW_FINDER_ASPECT_RATIO)
            .padding(
                horizontal = 2.dp
            )
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.stripe_view_finder_corner_radius)))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                CameraView(
                    it,
                    CameraView.ViewFinderType.ID,
                    R.drawable.stripe_viewfinder_border_initial
                )
            },
            update =
            {
                cameraManager.onCameraViewUpdate(it)
            }
        )
        if (shouldShowFinished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        colorResource(id = R.color.stripe_check_mark_background)
                    )
                    .testTag(CHECK_MARK_TAG)
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(60.dp),
                    painter = painterResource(id = R.drawable.stripe_check_mark),
                    contentDescription = stringResource(id = R.string.stripe_check_mark),
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                )
            }
        }
    }
}

private const val FEED_BACK_REFRESH_LIMIT = 300L
