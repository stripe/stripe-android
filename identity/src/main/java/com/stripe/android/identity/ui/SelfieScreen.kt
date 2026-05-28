package com.stripe.android.identity.ui

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.stripe.android.camera.framework.image.mirrorHorizontally
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.CameraSource
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_SELFIE
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.camera.SelfieCameraManager
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.startScanning
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.SelfieScanViewModel
import com.stripe.android.uicore.text.dimensionResourceSp
import com.stripe.android.uicore.utils.collectAsState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val SELFIE_VIEW_FINDER_ASPECT_RATIO = 0.75f
internal const val SELFIE_SCAN_TITLE_TAG = "SelfieScanTitle"
internal const val SELFIE_SCAN_MESSAGE_TAG = "SelfieScanMessage"
internal const val SELFIE_SCAN_CONTINUE_BUTTON_TAG = "SelfieScanContinue"
internal const val SCAN_VIEW_TAG = "SelfieScanViewTag"
internal const val RESULT_VIEW_TAG = "SelfieResultViewTag"
internal const val RETAKE_SELFIE_BUTTON_TAG = "RetakeSelfieButtonTag"
internal const val CONSENT_CHECKBOX_TAG = "ConsentCheckboxTag"
internal const val SELFIE_SCAN_STATUS_TAG = "SelfieScanStatusTag"
internal const val SELFIE_HAVING_TROUBLE_TAG = "SelfieHavingTroubleTag"
internal const val SELFIE_CAPTURE_GUIDE_TAG = "SelfieCaptureGuideTag"
private const val FLASH_MAX_ALPHA = 0.5f
private const val FLASH_ANIMATION_TIME = 200
private const val CAPTURE_GUIDE_TICK_COUNT = 77
private const val CAPTURE_GUIDE_HORIZONTAL_DIAMETER_RATIO = 0.62f
private const val CAPTURE_GUIDE_VERTICAL_DIAMETER_RATIO = 0.56f
private const val CAPTURE_GUIDE_CENTER_Y_RATIO = 0.41f

@Composable
internal fun SelfieScanScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    selfieScanViewModel: SelfieScanViewModel,
    fallbackUrlLauncher: FallbackUrlLauncher
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember {
        SelfieCameraManager(context = context) { cause ->
            identityViewModel.identityAnalyticsRequestFactory.cameraError(
                scanType = IdentityScanState.ScanType.SELFIE,
                throwable = IllegalStateException(cause),
                screenName = SCREEN_NAME_SELFIE,
                cameraSource = CameraSource.CAMERA_SESSION
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
                    fallbackUrl = pageAndModelFiles.page.fallbackUrl,
                    identityViewModel = identityViewModel,
                    identityScanViewModel = selfieScanViewModel,
                    navController = navController,
                    lifecycleOwner = lifecycleOwner,
                    cameraManager = cameraManager,
                    fallbackUrlLauncher = fallbackUrlLauncher
                )
            }
        }
    }
}

@Composable
private fun SelfieCaptureScreen(
    selfieScannerState: IdentityScanViewModel.State,
    feedback: Int?,
    fallbackUrl: String,
    identityViewModel: IdentityViewModel,
    identityScanViewModel: IdentityScanViewModel,
    navController: NavController,
    lifecycleOwner: LifecycleOwner,
    cameraManager: SelfieCameraManager,
    fallbackUrlLauncher: FallbackUrlLauncher
) {
    LaunchedEffect(Unit) {
        startScanning(
            IdentityScanState.ScanType.SELFIE,
            identityViewModel = identityViewModel,
            identityScanViewModel = identityScanViewModel,
            lifecycleOwner = lifecycleOwner
        )
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

    val faceDetectorTransitioner =
        (selfieScannerState as? IdentityScanViewModel.State.Scanned)
            ?.result?.identityState?.transitioner as? FaceDetectorTransitioner

    LaunchedEffect(faceDetectorTransitioner) {
        faceDetectorTransitioner?.let {
            identityViewModel.collectDataForSelfieScreen(
                navController = navController,
                faceDetectorTransitioner = it
            )
        }
    }

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

            val isCheckingImages = faceDetectorTransitioner != null
            SelfieCameraViewFinder(
                imageAlpha = imageAlpha,
                cameraManager = cameraManager,
                identityViewModel = identityViewModel,
                status = selfieScannerState.status(),
                showCaptureGuideShadow = (selfieScannerState as? IdentityScanViewModel.State.Scanning)
                    ?.scanState is IdentityScanState.Found,
                showCaptureGuide = !isCheckingImages,
                capturedSelfie = faceDetectorTransitioner?.lastCapturedSelfie()
            )
            if (!isCheckingImages) {
                Text(
                    text = stringResource(id = R.string.stripe_having_trouble),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                        .testTag(SELFIE_HAVING_TROUBLE_TAG)
                        .clickable {
                            identityViewModel.screenTracker.screenTransitionStart(SCREEN_NAME_SELFIE)
                            fallbackUrlLauncher.launchFallbackUrl(fallbackUrl)
                        },
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.caption,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
private fun SelfieCameraViewFinder(
    imageAlpha: Float,
    cameraManager: IdentityCameraManager,
    identityViewModel: IdentityViewModel,
    status: SelfieStatus?,
    showCaptureGuideShadow: Boolean,
    showCaptureGuide: Boolean,
    capturedSelfie: Bitmap?
) {
    // Wait for camera adapter to be initialized before accessing lens model
    LaunchedEffect(cameraManager.cameraAdapter) {
        if (cameraManager.cameraAdapter != null) {
            // Camera is initialized, set the lens model
            identityViewModel.setSelfieCameraLensModel(cameraManager)
        }
    }

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
        if (capturedSelfie != null) {
            Image(
                bitmap = remember(capturedSelfie) {
                    capturedSelfie.mirrorHorizontally().asImageBitmap()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(id = R.string.stripe_selfie_item_description)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.16f))
            )
        } else {
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
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(imageAlpha)
                .background(colorResource(id = R.color.stripe_flash_mask_color))
        )
        if (showCaptureGuide) {
            CaptureGuide(showCaptureGuideShadow)
        }
        status?.let {
            SelfieStatusBadge(
                status = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            )
        }
    }
}

private enum class SelfieStatus(
    @param:StringRes val labelRes: Int,
    val showsActivityIndicator: Boolean
) {
    HoldStill(
        labelRes = R.string.stripe_hold_still_selfie,
        showsActivityIndicator = false
    ),
    CheckingImages(
        labelRes = R.string.stripe_selfie_checking_images,
        showsActivityIndicator = true
    )
}

private fun IdentityScanViewModel.State.status(): SelfieStatus? {
    return when (this) {
        is IdentityScanViewModel.State.Scanning ->
            if (scanState is IdentityScanState.Found) {
                SelfieStatus.HoldStill
            } else {
                null
            }
        is IdentityScanViewModel.State.Scanned -> SelfieStatus.CheckingImages
        else -> null
    }
}

private fun FaceDetectorTransitioner.lastCapturedSelfie(): Bitmap {
    return filteredFrames[FaceDetectorTransitioner.INDEX_LAST].first.cameraPreviewImage.image
}

@Composable
private fun SelfieStatusBadge(
    status: SelfieStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color(0x9921252C),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                testTag = SELFIE_SCAN_STATUS_TAG
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status.showsActivityIndicator) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
        Text(
            text = stringResource(id = status.labelRes),
            color = Color.White,
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
private fun CaptureGuide(showCenteredShadow: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SELFIE_CAPTURE_GUIDE_TAG)
    ) {
        if (size.width <= 0f || size.height <= 0f) {
            return@Canvas
        }

        val horizontalRadius = size.width * CAPTURE_GUIDE_HORIZONTAL_DIAMETER_RATIO / 2f
        val verticalRadius = size.height * CAPTURE_GUIDE_VERTICAL_DIAMETER_RATIO / 2f
        val center = Offset(
            x = size.width / 2f,
            y = size.height * CAPTURE_GUIDE_CENTER_Y_RATIO
        )
        val guideRect = Rect(
            left = center.x - horizontalRadius,
            top = center.y - verticalRadius,
            right = center.x + horizontalRadius,
            bottom = center.y + verticalRadius
        )

        if (showCenteredShadow) {
            val shadowPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addOval(guideRect)
            }
            drawPath(
                path = shadowPath,
                color = Color.Black.copy(alpha = 0.32f)
            )
        }

        val tickLength = 10.dp.toPx()
        val strokeWidth = 2.dp.toPx()
        repeat(CAPTURE_GUIDE_TICK_COUNT) { index ->
            val angle = (index.toFloat() / CAPTURE_GUIDE_TICK_COUNT.toFloat()) * Math.PI.toFloat() * 2f
            val cosAngle = cos(angle)
            val sinAngle = sin(angle)
            val tickCenter = Offset(
                x = center.x + cosAngle * horizontalRadius,
                y = center.y + sinAngle * verticalRadius
            )
            val normalX = cosAngle / horizontalRadius
            val normalY = sinAngle / verticalRadius
            val normalLength = sqrt((normalX * normalX) + (normalY * normalY))
            val unitNormalX = normalX / normalLength
            val unitNormalY = normalY / normalLength
            val halfTickLength = tickLength / 2f

            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(
                    x = tickCenter.x - unitNormalX * halfTickLength,
                    y = tickCenter.y - unitNormalY * halfTickLength
                ),
                end = Offset(
                    x = tickCenter.x + unitNormalX * halfTickLength,
                    y = tickCenter.y + unitNormalY * halfTickLength
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt
            )
        }
    }
}
