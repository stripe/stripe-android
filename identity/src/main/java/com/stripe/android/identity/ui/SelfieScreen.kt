package com.stripe.android.identity.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import android.graphics.Path as AndroidPath

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
internal const val SELFIE_CAPTURE_GUIDE_SHADOW_TAG = "SelfieCaptureGuideShadowTag"
internal const val SELFIE_SCAN_ACTIVITY_INDICATOR_TAG = "SelfieScanActivityIndicatorTag"
private const val CAPTURE_GUIDE_TICK_COUNT = 77
private const val CAPTURE_GUIDE_HORIZONTAL_DIAMETER_RATIO = 0.62f
private const val CAPTURE_GUIDE_VERTICAL_DIAMETER_RATIO = 0.56f

// FaceDetectorTransitioner validates centering against the normalized image center.
private const val CAPTURE_GUIDE_CENTER_Y_RATIO = 0.5f
private const val CAPTURE_GUIDE_SHADOW_ANIMATION_TIME = 600
private const val CAPTURE_GUIDE_SHADOW_FEATHER_DP = 46
private const val CAPTURE_GUIDE_SHADOW_FEATHER_LOCATION_MIN = 0.06f
private const val CAPTURE_GUIDE_SHADOW_FEATHER_LOCATION_MAX = 0.96f
private const val CAPTURE_GUIDE_SHADOW_INNER_LOCATION_FACTOR = 0.18f
private const val CAPTURE_GUIDE_SHADOW_MID_LOCATION_FACTOR = 0.58f
private const val CAPTURE_GUIDE_SHADOW_INNER_ALPHA = 0.12f
private const val CAPTURE_GUIDE_SHADOW_MID_ALPHA = 0.22f
private const val CAPTURE_GUIDE_SHADOW_RING_ALPHA = 0.3f
private const val CAPTURE_GUIDE_SHADOW_OUTER_ALPHA = 0.36f
private const val CAPTURE_GUIDE_TICK_ALPHA = 0.8f
private const val CAPTURE_GUIDE_ACTIVE_TICK_COLOR = 0xFF2DD36F
private const val CAPTURE_GUIDE_TICK_SHADOW_ALPHA = 0.3f
private const val CAPTURED_SELFIE_OVERLAY_ALPHA = 0.16f
private const val STATUS_PILL_TEXT_SHADOW_ALPHA = 0.35f
private const val STATUS_PILL_BACKGROUND_COLOR = 0x9921252C

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
            navController = navController,
            cameraManager = cameraManager
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
    lifecycleOwner: LifecycleOwner,
    cameraManager: SelfieCameraManager,
    fallbackUrlLauncher: FallbackUrlLauncher
) {
    if (selfieScannerState is IdentityScanViewModel.State.Scanning) {
        LaunchedEffect(Unit) {
            identityViewModel.clearSelfieUploadedState()
            startScanning(
                IdentityScanState.ScanType.SELFIE,
                identityViewModel = identityViewModel,
                identityScanViewModel = identityScanViewModel,
                lifecycleOwner = lifecycleOwner
            )
        }
    }

    val faceDetectorTransitioner =
        (selfieScannerState as? IdentityScanViewModel.State.Scanned)
            ?.result?.identityState?.transitioner as? FaceDetectorTransitioner

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

            val scanState = (selfieScannerState as? IdentityScanViewModel.State.Scanning)?.scanState
            val isCheckingImages = faceDetectorTransitioner != null
            SelfieCameraViewFinder(
                cameraManager = cameraManager,
                identityViewModel = identityViewModel,
                status = selfieScannerState.status(),
                showCaptureGuideShadow = (selfieScannerState as? IdentityScanViewModel.State.Scanning)
                    ?.scanState is IdentityScanState.Found,
                captureGuideTone = scanState.captureGuideTone(),
                showCaptureGuide = !isCheckingImages,
                capturedSelfie = faceDetectorTransitioner?.lastCapturedSelfie()
            )
            if (!isCheckingImages) {
                Text(
                    text = stringResource(id = R.string.stripe_having_trouble),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp)
                        .testTag(SELFIE_HAVING_TROUBLE_TAG)
                        .clickable {
                            identityViewModel.screenTracker.screenTransitionStart(SCREEN_NAME_SELFIE)
                            fallbackUrlLauncher.launchFallbackUrl(fallbackUrl)
                        },
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
private fun SelfieCameraViewFinder(
    cameraManager: IdentityCameraManager,
    identityViewModel: IdentityViewModel,
    status: SelfieStatus?,
    showCaptureGuideShadow: Boolean,
    captureGuideTone: CaptureGuideTone,
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

    AnnounceSelfieStatus(status)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(SELFIE_VIEW_FINDER_ASPECT_RATIO)
            .padding(
                horizontal = dimensionResource(id = R.dimen.stripe_page_horizontal_margin)
            )
            .clip(RoundedCornerShape(20.dp))
            .testTag(SCAN_VIEW_TAG)
    ) {
        SelfieCameraViewFinderContent(
            capturedSelfie = capturedSelfie,
            cameraManager = cameraManager
        )
        if (showCaptureGuide) {
            CaptureGuide(showCaptureGuideShadow, captureGuideTone)
            if (showCaptureGuideShadow) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(SELFIE_CAPTURE_GUIDE_SHADOW_TAG)
                )
            }
        }
        status?.let {
            val statusModifier = when (it) {
                SelfieStatus.PlaceFace,
                SelfieStatus.HoldStill,
                SelfieStatus.CapturedFront,
                SelfieStatus.CapturedLeft,
                SelfieStatus.CapturedRight ->
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                SelfieStatus.LookLeft,
                SelfieStatus.LookRight,
                SelfieStatus.CheckingImages ->
                    Modifier
                        .align(Alignment.Center)
            }
            SelfieStatusBadge(
                status = it,
                modifier = statusModifier
            )
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun AnnounceSelfieStatus(status: SelfieStatus?) {
    val view = LocalView.current
    val checkingImagesText = stringResource(id = R.string.stripe_selfie_checking_images)

    LaunchedEffect(status) {
        if (status == SelfieStatus.CheckingImages) {
            view.announceForAccessibility(checkingImagesText)
        }
    }
}

@Composable
private fun SelfieCameraViewFinderContent(
    capturedSelfie: Bitmap?,
    cameraManager: IdentityCameraManager
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
                .background(Color.Black.copy(alpha = CAPTURED_SELFIE_OVERLAY_ALPHA))
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
}

private enum class SelfieStatus(
    @param:StringRes val labelRes: Int,
    val showsActivityIndicator: Boolean
) {
    PlaceFace(
        labelRes = R.string.stripe_selfie_place_face,
        showsActivityIndicator = false
    ),
    LookLeft(
        labelRes = R.string.stripe_selfie_look_left,
        showsActivityIndicator = false
    ),
    LookRight(
        labelRes = R.string.stripe_selfie_look_right,
        showsActivityIndicator = false
    ),
    HoldStill(
        labelRes = R.string.stripe_hold_still_selfie,
        showsActivityIndicator = false
    ),
    CapturedFront(
        labelRes = R.string.stripe_captured_front_selfie,
        showsActivityIndicator = false
    ),
    CapturedLeft(
        labelRes = R.string.stripe_captured_left_selfie,
        showsActivityIndicator = false
    ),
    CapturedRight(
        labelRes = R.string.stripe_captured_right_selfie,
        showsActivityIndicator = false
    ),
    CheckingImages(
        labelRes = R.string.stripe_selfie_checking_images,
        showsActivityIndicator = true
    )
}

private fun IdentityScanViewModel.State.status(): SelfieStatus? {
    return when (this) {
        is IdentityScanViewModel.State.Scanning -> scanState.status()
        is IdentityScanViewModel.State.Scanned -> SelfieStatus.CheckingImages
        else -> null
    }
}

private fun IdentityScanState?.status(): SelfieStatus? {
    val transitioner = this?.transitioner as? FaceDetectorTransitioner
    return when (this) {
        null -> SelfieStatus.PlaceFace
        is IdentityScanState.Initial -> when (transitioner?.activeCapture) {
            FaceDetectorTransitioner.Capture.LEFT -> SelfieStatus.LookLeft
            FaceDetectorTransitioner.Capture.RIGHT -> SelfieStatus.LookRight
            FaceDetectorTransitioner.Capture.FRONT,
            null -> SelfieStatus.PlaceFace
        }
        is IdentityScanState.Found -> SelfieStatus.HoldStill
        is IdentityScanState.Satisfied -> when (transitioner?.completedCapture) {
            FaceDetectorTransitioner.Capture.LEFT -> SelfieStatus.CapturedLeft
            FaceDetectorTransitioner.Capture.RIGHT -> SelfieStatus.CapturedRight
            FaceDetectorTransitioner.Capture.FRONT,
            null -> SelfieStatus.CapturedFront
        }
        is IdentityScanState.Finished -> SelfieStatus.CapturedRight
        is IdentityScanState.TimeOut,
        is IdentityScanState.Unsatisfied -> null
    }
}

private enum class CaptureGuideTone {
    Default,
    Active
}

private fun IdentityScanState?.captureGuideTone(): CaptureGuideTone {
    return when (this) {
        is IdentityScanState.Found,
        is IdentityScanState.Satisfied -> CaptureGuideTone.Active
        else -> CaptureGuideTone.Default
    }
}

private fun FaceDetectorTransitioner.lastCapturedSelfie(): Bitmap {
    return filteredFrames[FaceDetectorTransitioner.INDEX_LAST].first.cameraPreviewImage.image
}

private fun DrawScope.drawCenteredGuideShadow(
    center: Offset,
    horizontalRadius: Float,
    verticalRadius: Float,
    opacity: Float
) {
    val scaleX = horizontalRadius / verticalRadius
    val maxXDistance = max(center.x, size.width - center.x) / scaleX
    val maxYDistance = max(center.y, size.height - center.y)
    val outerRadius = hypot(maxXDistance, maxYDistance)
    val clearRadius = verticalRadius
    if (outerRadius <= clearRadius) {
        return
    }

    val featherRadius = min(verticalRadius + CAPTURE_GUIDE_SHADOW_FEATHER_DP.dp.toPx(), outerRadius)
    val gradientStops = centeredGuideShadowGradientStops(
        clearRadius = clearRadius,
        featherRadius = featherRadius,
        outerRadius = outerRadius
    )
    val shadowOpacity = opacity.coerceIn(0f, 1f)

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        nativeCanvas.save()
        nativeCanvas.translate(center.x, center.y)
        nativeCanvas.scale(scaleX, 1f)

        nativeCanvas.drawPath(
            centeredGuideShadowPath(outerRadius, clearRadius),
            centeredGuideShadowPaint(outerRadius, gradientStops, shadowOpacity)
        )
        nativeCanvas.restore()
    }
}

private data class CenteredGuideShadowGradientStops(
    val clear: Float,
    val inner: Float,
    val mid: Float,
    val ring: Float
)

private fun centeredGuideShadowGradientStops(
    clearRadius: Float,
    featherRadius: Float,
    outerRadius: Float
): CenteredGuideShadowGradientStops {
    val shadowSpan = outerRadius - clearRadius
    val featherLocation = min(
        max(
            (featherRadius - clearRadius) / shadowSpan,
            CAPTURE_GUIDE_SHADOW_FEATHER_LOCATION_MIN
        ),
        CAPTURE_GUIDE_SHADOW_FEATHER_LOCATION_MAX
    )

    fun absoluteStop(relativeLocation: Float): Float {
        return (clearRadius + (shadowSpan * relativeLocation)) / outerRadius
    }

    return CenteredGuideShadowGradientStops(
        clear = clearRadius / outerRadius,
        inner = absoluteStop(featherLocation * CAPTURE_GUIDE_SHADOW_INNER_LOCATION_FACTOR),
        mid = absoluteStop(featherLocation * CAPTURE_GUIDE_SHADOW_MID_LOCATION_FACTOR),
        ring = absoluteStop(featherLocation)
    )
}

private fun centeredGuideShadowPaint(
    outerRadius: Float,
    gradientStops: CenteredGuideShadowGradientStops,
    shadowOpacity: Float
): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            0f,
            0f,
            outerRadius,
            intArrayOf(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
                Color.Black.copy(alpha = CAPTURE_GUIDE_SHADOW_INNER_ALPHA * shadowOpacity).toArgb(),
                Color.Black.copy(alpha = CAPTURE_GUIDE_SHADOW_MID_ALPHA * shadowOpacity).toArgb(),
                Color.Black.copy(alpha = CAPTURE_GUIDE_SHADOW_RING_ALPHA * shadowOpacity).toArgb(),
                Color.Black.copy(alpha = CAPTURE_GUIDE_SHADOW_OUTER_ALPHA * shadowOpacity).toArgb()
            ),
            floatArrayOf(
                0f,
                gradientStops.clear,
                gradientStops.inner,
                gradientStops.mid,
                gradientStops.ring,
                1f
            ),
            Shader.TileMode.CLAMP
        )
    }
}

private fun centeredGuideShadowPath(
    outerRadius: Float,
    clearRadius: Float
): AndroidPath {
    return AndroidPath().apply {
        fillType = AndroidPath.FillType.EVEN_ODD
        addCircle(0f, 0f, outerRadius, AndroidPath.Direction.CW)
        addCircle(0f, 0f, clearRadius, AndroidPath.Direction.CW)
    }
}

@Composable
private fun SelfieStatusBadge(
    status: SelfieStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color(STATUS_PILL_BACKGROUND_COLOR),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
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
                    .size(18.dp)
                    .testTag(SELFIE_SCAN_ACTIVITY_INDICATOR_TAG),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
        Text(
            text = stringResource(id = status.labelRes),
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            style = MaterialTheme.typography.body2.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = STATUS_PILL_TEXT_SHADOW_ALPHA),
                    offset = Offset(x = 0f, y = 1f),
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
private fun CaptureGuide(
    showCenteredShadow: Boolean,
    captureGuideTone: CaptureGuideTone
) {
    val centeredShadowAlpha = remember { Animatable(0f) }

    LaunchedEffect(showCenteredShadow) {
        centeredShadowAlpha.animateTo(
            targetValue = if (showCenteredShadow) 1f else 0f,
            animationSpec = tween(
                durationMillis = CAPTURE_GUIDE_SHADOW_ANIMATION_TIME,
                easing = FastOutSlowInEasing
            )
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SELFIE_CAPTURE_GUIDE_TAG)
    ) {
        if (size.width <= 0f || size.height <= 0f) {
            return@Canvas
        }

        val geometry = captureGuideGeometry()
        if (centeredShadowAlpha.value > 0f) {
            drawCenteredGuideShadow(
                center = geometry.center,
                horizontalRadius = geometry.horizontalRadius,
                verticalRadius = geometry.verticalRadius,
                opacity = centeredShadowAlpha.value
            )
        }

        drawCaptureGuideTicks(geometry, captureGuideTone)
    }
}

private data class CaptureGuideGeometry(
    val center: Offset,
    val horizontalRadius: Float,
    val verticalRadius: Float
)

private fun DrawScope.captureGuideGeometry(): CaptureGuideGeometry {
    return CaptureGuideGeometry(
        center = Offset(
            x = size.width / 2f,
            y = size.height * CAPTURE_GUIDE_CENTER_Y_RATIO
        ),
        horizontalRadius = size.width * CAPTURE_GUIDE_HORIZONTAL_DIAMETER_RATIO / 2f,
        verticalRadius = size.height * CAPTURE_GUIDE_VERTICAL_DIAMETER_RATIO / 2f
    )
}

private fun DrawScope.drawCaptureGuideTicks(
    geometry: CaptureGuideGeometry,
    captureGuideTone: CaptureGuideTone
) {
    with(geometry) {
        val tickLength = 10.dp.toPx()
        val strokeWidth = 2.dp.toPx()
        val shadowOffsetY = 1.dp.toPx()
        val shadowBlur = 4.dp.toPx()
        drawIntoCanvas { canvas ->
            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (captureGuideTone) {
                    CaptureGuideTone.Default -> Color.White.copy(alpha = CAPTURE_GUIDE_TICK_ALPHA)
                    CaptureGuideTone.Active -> Color(CAPTURE_GUIDE_ACTIVE_TICK_COLOR)
                }.toArgb()
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.BUTT
                this.strokeWidth = strokeWidth
                setShadowLayer(
                    shadowBlur,
                    0f,
                    shadowOffsetY,
                    Color.Black.copy(alpha = CAPTURE_GUIDE_TICK_SHADOW_ALPHA).toArgb()
                )
            }
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

                canvas.nativeCanvas.drawLine(
                    tickCenter.x - unitNormalX * halfTickLength,
                    tickCenter.y - unitNormalY * halfTickLength,
                    tickCenter.x + unitNormalX * halfTickLength,
                    tickCenter.y + unitNormalY * halfTickLength,
                    tickPaint
                )
            }
        }
    }
}
