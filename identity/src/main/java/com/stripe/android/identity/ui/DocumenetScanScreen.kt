package com.stripe.android.identity.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.navigation.NavController
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.R
import com.stripe.android.identity.camera.DocumentScanCameraManager
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.navigation.routeToScreenName
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.Companion.isFront
import com.stripe.android.identity.states.IdentityScanState.Companion.isNullOrFront
import com.stripe.android.identity.utils.startScanning
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

internal const val CONTINUE_BUTTON_TAG = "Continue"
internal const val SCAN_TITLE_TAG = "Title"
internal const val SCAN_MESSAGE_TAG = "Message"
internal const val CHECK_MARK_TAG = "CheckMark"
internal const val VIEW_FINDER_ASPECT_RATIO = 1.5f

internal data class DocumentScanMessageRes(
    @StringRes
    val frontTitleStringRes: Int,
    @StringRes
    val backTitleStringRes: Int,
    @StringRes
    val frontMessageStringRes: Int,
    @StringRes
    val backMessageStringRes: Int
)

@Composable
internal fun DocumentScanScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    identityScanViewModel: IdentityScanViewModel,
    frontScanType: IdentityScanState.ScanType,
    backScanType: IdentityScanState.ScanType?,
    shouldStartFromBack: Boolean,
    messageRes: DocumentScanMessageRes,
    collectedDataParamType: CollectedDataParam.Type,
    route: String
) {
    val changedDisplayState by identityScanViewModel.displayStateChangedFlow.collectAsState()
    val newDisplayState by remember {
        derivedStateOf {
            changedDisplayState?.first
        }
    }
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
            navController.navigateToErrorScreenWithDefaultValues(context)
        }
    ) { verificationPage ->
        val cameraManager = remember {
            DocumentScanCameraManager(
                context = context
            ) { cause ->
                identityViewModel.sendAnalyticsRequest(
                    identityViewModel.identityAnalyticsRequestFactory.cameraError(
                        scanType = frontScanType,
                        throwable = IllegalStateException(cause)
                    )
                )
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current

        val targetScanType by identityScanViewModel.targetScanTypeFlow.collectAsState()

        val title = if (targetScanType.isNullOrFront()) {
            stringResource(id = messageRes.frontTitleStringRes)
        } else {
            stringResource(id = messageRes.backTitleStringRes)
        }

        val message = when (newDisplayState) {
            is IdentityScanState.Finished -> stringResource(id = R.string.stripe_scanned)
            is IdentityScanState.Found -> stringResource(id = R.string.stripe_hold_still)
            is IdentityScanState.Initial -> {
                if (targetScanType.isNullOrFront()) {
                    stringResource(id = messageRes.frontMessageStringRes)
                } else {
                    stringResource(id = messageRes.backMessageStringRes)
                }
            }

            is IdentityScanState.Satisfied -> stringResource(id = R.string.stripe_scanned)
            is IdentityScanState.TimeOut -> ""
            is IdentityScanState.Unsatisfied -> ""
            null -> {
                if (targetScanType.isNullOrFront()) {
                    stringResource(id = messageRes.frontMessageStringRes)
                } else {
                    stringResource(id = messageRes.backMessageStringRes)
                }
            }
        }

        LaunchedEffect(newDisplayState) {
            when (newDisplayState) {
                null -> {
                    cameraManager.toggleInitial()
                }
                is IdentityScanState.Initial -> {
                    cameraManager.toggleInitial()
                }
                is IdentityScanState.Found -> {
                    cameraManager.toggleFound()
                }
                is IdentityScanState.Finished -> {
                    cameraManager.toggleFinished()
                }
                else -> {} // no-op
            }
        }

        LaunchedEffect(Unit) {
            if (shouldStartFromBack) {
                identityViewModel.resetDocumentUploadedState()
            }
        }

        CameraScreenLaunchedEffect(
            identityViewModel = identityViewModel,
            identityScanViewModel = identityScanViewModel,
            verificationPage = verificationPage,
            navController = navController,
            cameraManager = cameraManager
        ) {
            if (shouldStartFromBack) {
                startScanning(
                    scanType = requireNotNull(backScanType) {
                        "$backScanType should not be null when trying to scan from back"
                    },
                    identityViewModel = identityViewModel,
                    identityScanViewModel = identityScanViewModel,
                    lifecycleOwner = lifecycleOwner
                )
            } else {
                startScanning(
                    scanType = frontScanType,
                    identityViewModel = identityViewModel,
                    identityScanViewModel = identityScanViewModel,
                    lifecycleOwner = lifecycleOwner
                )
            }
        }

        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            scanType = frontScanType,
            screenName = route.routeToScreenName()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = dimensionResource(id = R.dimen.page_vertical_margin),
                    horizontal = dimensionResource(id = R.dimen.page_horizontal_margin)
                )
        ) {
            var loadingButtonState by remember(newDisplayState) {
                mutableStateOf(
                    if (newDisplayState is IdentityScanState.Finished) {
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
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(
                            top = dimensionResource(id = R.dimen.item_vertical_margin),
                            bottom = 48.dp
                        )
                        .semantics {
                            testTag = SCAN_MESSAGE_TAG
                        },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                CameraViewFinder(newDisplayState, cameraManager)
            }
            LoadingButton(
                modifier = Modifier.testTag(CONTINUE_BUTTON_TAG),
                text = stringResource(id = R.string.stripe_kontinue).uppercase(),
                state = loadingButtonState
            ) {
                loadingButtonState = LoadingButtonState.Loading

                coroutineScope.launch {
                    identityViewModel.collectDataForDocumentScanScreen(
                        navController = navController,
                        isFront = requireNotNull(targetScanType) {
                            "targetScanType is still null"
                        }.isFront(),
                        collectedDataParamType = collectedDataParamType,
                        route = route
                    ) {
                        startScanning(
                            scanType = requireNotNull(backScanType) {
                                "backScanType is null while still missing back"
                            },
                            identityViewModel = identityViewModel,
                            identityScanViewModel = identityScanViewModel,
                            lifecycleOwner = lifecycleOwner
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraViewFinder(
    newScanState: IdentityScanState?,
    cameraManager: IdentityCameraManager
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(VIEW_FINDER_ASPECT_RATIO)
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.view_finder_corner_radius)))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                CameraView(
                    it,
                    CameraView.ViewFinderType.ID,
                    R.drawable.viewfinder_border_initial
                )
            },
            update =
            {
                cameraManager.onCameraViewUpdate(it)
            }
        )
        if (newScanState is IdentityScanState.Finished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        colorResource(id = R.color.check_mark_background)
                    )
                    .testTag(CHECK_MARK_TAG)
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(60.dp),
                    painter = painterResource(id = R.drawable.check_mark),
                    contentDescription = stringResource(id = R.string.stripe_check_mark),
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                )
            }
        }
    }
}
