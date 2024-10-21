package com.stripe.android.identity.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.stripe.android.camera.framework.image.mirrorHorizontally
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_SELFIE
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.camera.SelfieCameraManager
import com.stripe.android.identity.navigation.SelfieDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.startScanning
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.SelfieScanViewModel
import com.stripe.android.uicore.text.Html
import com.stripe.android.uicore.text.dimensionResourceSp
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

internal const val SELFIE_VIEW_FINDER_ASPECT_RATIO = 1f
internal const val SELFIE_SCAN_TITLE_TAG = "SelfieScanTitle"
internal const val SELFIE_SCAN_MESSAGE_TAG = "SelfieScanMessage"
internal const val SELFIE_SCAN_CONTINUE_BUTTON_TAG = "SelfieScanContinue"
internal const val SCAN_VIEW_TAG = "SelfieScanViewTag"
internal const val RESULT_VIEW_TAG = "SelfieResultViewTag"
internal const val RETAKE_SELFIE_BUTTON_TAG = "RetakeSelfieButtonTag"
internal const val CONSENT_CHECKBOX_TAG = "ConsentCheckboxTag"
private const val FLASH_MAX_ALPHA = 0.5f
private const val FLASH_ANIMATION_TIME = 200

@Composable
internal fun SelfieScanScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    selfieScanViewModel: SelfieScanViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember {
        SelfieCameraManager(context = context) { cause ->
            identityViewModel.identityAnalyticsRequestFactory.cameraError(
                scanType = IdentityScanState.ScanType.SELFIE,
                throwable = IllegalStateException(cause)
            )
        }
    }

    CheckVerificationPageModelFilesAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) { pageAndModelFiles ->

        ScreenTransitionLaunchedEffect(
            identityViewModel = identityViewModel,
            screenName = SCREEN_NAME_SELFIE,
            scanType = IdentityScanState.ScanType.SELFIE
        )
        // run once to initialize
        LaunchedEffect(Unit) {
            selfieScanViewModel.initializeScanFlowAndUpdateState(pageAndModelFiles, cameraManager)
        }

        val selfieScannerState by selfieScanViewModel.scannerState.collectAsState()
        val feedback by selfieScanViewModel.scanFeedback.collectAsState()

        LiveCaptureLaunchedEffect(
            scannerState = selfieScannerState,
            identityScanViewModel = selfieScanViewModel,
            identityViewModel = identityViewModel,
            lifecycleOwner = lifecycleOwner,
            verificationPage = pageAndModelFiles.page,
            navController = navController
        )

        when (selfieScannerState) {
            IdentityScanViewModel.State.Initializing -> {
                LoadingScreen()
            }

            else -> {
                val successSelfieCapturePage =
                    remember {
                        requireNotNull(pageAndModelFiles.page.selfieCapture) {
                            identityViewModel.errorCause.postValue(
                                IllegalStateException("VerificationPage.selfieCapture is null")
                            )
                            navController.navigateToErrorScreenWithDefaultValues(context)
                        }
                    }
                SelfieCaptureScreen(
                    selfieScannerState = selfieScannerState,
                    feedback = feedback,
                    successSelfieCapturePage = successSelfieCapturePage,
                    identityViewModel = identityViewModel,
                    identityScanViewModel = selfieScanViewModel,
                    navController = navController,
                    lifecycleOwner = lifecycleOwner,
                    cameraManager = cameraManager
                )
            }
        }
    }
}

@Composable
private fun SelfieCaptureScreen(
    selfieScannerState: IdentityScanViewModel.State,
    feedback: Int?,
    successSelfieCapturePage: VerificationPageStaticContentSelfieCapturePage,
    identityViewModel: IdentityViewModel,
    identityScanViewModel: IdentityScanViewModel,
    navController: NavController,
    lifecycleOwner: LifecycleOwner,
    cameraManager: SelfieCameraManager,
) {
    LaunchedEffect(Unit) {
        startScanning(
            IdentityScanState.ScanType.SELFIE,
            identityViewModel = identityViewModel,
            identityScanViewModel = identityScanViewModel,
            lifecycleOwner = lifecycleOwner
        )
    }

    val coroutineScope = rememberCoroutineScope()

    var allowImageCollection by remember {
        mutableStateOf(false)
    }

    var isSubmittingSelfie by remember {
        mutableStateOf(false)
    }

    var flashed by remember {
        mutableStateOf(false)
    }
    val imageAlpha: Float by animateFloatAsState(
        targetValue = if (
            !flashed && selfieScannerState is IdentityScanViewModel.State.Scanning &&
            selfieScannerState.scanState is IdentityScanState.Found
        ) {
            FLASH_MAX_ALPHA
        } else {
            0f
        },
        animationSpec = tween(
            durationMillis = FLASH_ANIMATION_TIME,
            easing = LinearEasing,
        ),
        finishedListener = {
            flashed = true
        },
        label = "flashAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = dimensionResource(id = R.dimen.stripe_page_vertical_margin)
            )
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.stripe_selfie_captures),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
                    )
                    .semantics {
                        testTag = SELFIE_SCAN_TITLE_TAG
                    },
                fontSize = dimensionResourceSp(id = R.dimen.stripe_scan_title_text_size),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = feedback?.let { stringResource(id = it) } ?: "",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(
                        top = 20.dp,
                        bottom = dimensionResource(id = R.dimen.stripe_item_vertical_margin),
                        start = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                        end = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
                    )
                    .semantics {
                        testTag = SELFIE_SCAN_MESSAGE_TAG
                    },
                maxLines = 3
            )

            if (selfieScannerState is IdentityScanViewModel.State.Scanned) {
                ResultView(
                    displayState = selfieScannerState.result.identityState,
                    allowImageCollectionHtml = successSelfieCapturePage.consentText,
                    isSubmittingSelfie = isSubmittingSelfie,
                    allowImageCollection = allowImageCollection,
                    navController = navController
                ) {
                    allowImageCollection = it
                }
            } else {
                SelfieCameraViewFinder(imageAlpha, cameraManager)
            }
        }
        var loadingButtonState by remember(selfieScannerState) {
            mutableStateOf(
                if (selfieScannerState is IdentityScanViewModel.State.Scanned) {
                    LoadingButtonState.Idle
                } else {
                    LoadingButtonState.Disabled
                }
            )
        }
        LoadingButton(
            modifier = Modifier
                .testTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG)
                .padding(dimensionResource(id = R.dimen.stripe_page_horizontal_margin)),
            text = stringResource(id = R.string.stripe_kontinue).uppercase(),
            state = loadingButtonState
        ) {
            loadingButtonState = LoadingButtonState.Loading
            isSubmittingSelfie = true
            coroutineScope.launch {
                identityViewModel.collectDataForSelfieScreen(
                    navController = navController,
                    faceDetectorTransitioner = requireNotNull(
                        (selfieScannerState as? IdentityScanViewModel.State.Scanned)
                            ?.result?.identityState?.transitioner as FaceDetectorTransitioner
                    ) {
                        "Failed to retrieve final result for Selfie"
                    },
                    allowImageCollection = allowImageCollection
                )
            }
        }
    }
}

@Composable
private fun ResultView(
    displayState: IdentityScanState,
    allowImageCollectionHtml: String,
    isSubmittingSelfie: Boolean,
    allowImageCollection: Boolean,
    navController: NavController,
    onAllowImageCollectionChanged: (Boolean) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .padding(
                horizontal = 5.dp
            )
            .testTag(RESULT_VIEW_TAG),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            (displayState.transitioner as FaceDetectorTransitioner)
                .filteredFrames.map { it.first.cameraPreviewImage.image.mirrorHorizontally() }
        ) { bitmap ->
            val imageBitmap = remember {
                bitmap.asImageBitmap()
            }
            Image(
                painter = BitmapPainter(imageBitmap),
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen.stripe_view_finder_corner_radius))),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(id = R.string.stripe_selfie_item_description)
            )
        }
    }

    Row(
        modifier = Modifier
            .padding(
                start = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                end = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                top = 20.dp
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            modifier = Modifier.testTag(RETAKE_SELFIE_BUTTON_TAG),
            onClick = { navController.navigateTo(SelfieDestination) },
            enabled = !isSubmittingSelfie
        ) {
            Icon(
                painter = painterResource(id = R.drawable.stripe_camera_icon),
                contentDescription = stringResource(id = R.string.stripe_description_camera),
                modifier = Modifier.padding(end = 5.dp)
            )
            Text(text = stringResource(id = R.string.stripe_retake_photos))
        }
    }

    Row(
        modifier = Modifier.padding(
            start = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
            end = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
            top = 20.dp
        )
    ) {
        Checkbox(
            modifier = Modifier.testTag(CONSENT_CHECKBOX_TAG),
            checked = allowImageCollection,
            onCheckedChange = { onAllowImageCollectionChanged(!allowImageCollection) },
            enabled = !isSubmittingSelfie
        )

        Html(
            html = allowImageCollectionHtml,
            color = MaterialTheme.colors.onBackground,
            urlSpanStyle = SpanStyle(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colors.secondary
            )
        )
    }
}

@Composable
private fun SelfieCameraViewFinder(
    imageAlpha: Float,
    cameraManager: IdentityCameraManager,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(SELFIE_VIEW_FINDER_ASPECT_RATIO)
            .padding(
                horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
            )
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.stripe_view_finder_corner_radius)))
            .testTag(SCAN_VIEW_TAG)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                CameraView(
                    it,
                    CameraView.ViewFinderType.Fill
                )
            },
            update = {
                cameraManager.onCameraViewUpdate(it)
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(imageAlpha)
                .background(colorResource(id = R.color.stripe_flash_mask_color))
        )
    }
}
