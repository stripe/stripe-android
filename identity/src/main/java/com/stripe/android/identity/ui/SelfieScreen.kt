package com.stripe.android.identity.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
internal const val SELFIE_CAPTURED_CHECK_TAG = "SelfieCapturedCheckTag"
private const val CAPTURE_GUIDE_TICK_COUNT = 68
private const val CAPTURE_GUIDE_TICK_HORIZONTAL_DIAMETER_RATIO = 0.57f
private const val CAPTURE_GUIDE_TICK_VERTICAL_DIAMETER_RATIO = 0.57f
private const val CAPTURE_GUIDE_SHADOW_HORIZONTAL_DIAMETER_RATIO = 0.62f
private const val CAPTURE_GUIDE_SHADOW_VERTICAL_DIAMETER_RATIO = 0.56f

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
private const val CAPTURE_GUIDE_TICK_ALPHA = 0.9f
private const val CAPTURE_GUIDE_BASE_TICK_LENGTH_DP = 10.25f
private const val CAPTURE_GUIDE_ACCEPTED_TICK_LENGTH_DP = 18.75f
private const val CAPTURE_GUIDE_BASE_TICK_STROKE_WIDTH_DP = 2.09f
private const val CAPTURE_GUIDE_ACCEPTED_TICK_STROKE_WIDTH_DP = 4.025f
private const val CAPTURE_GUIDE_ACTIVE_TICK_COLOR = 0xFF31C95F
private const val CAPTURE_GUIDE_TICK_SHADOW_ALPHA = 0.3f
private const val CAPTURED_SELFIE_OVERLAY_ALPHA = 0.16f
private const val STATUS_PILL_TEXT_SHADOW_ALPHA = 0.35f
private const val STATUS_PILL_BACKGROUND_COLOR = 0x9921252C
private const val STATUS_PILL_FADE_IN_DURATION = 180
private const val STATUS_PILL_FADE_OUT_DURATION = 600
private const val LIVE_PREVIEW_BLUR_IN_DURATION = 300
private const val LIVE_PREVIEW_BLUR_OUT_DURATION = 600
private const val TURN_PROMPT_ARROW_DURATION = 450
private const val CAPTURE_CHECKMARK_GROW_DURATION = 420
private const val CAPTURE_CHECKMARK_FADE_DURATION = 340
private const val CAPTURE_CHECKMARK_TOTAL_DURATION =
    CAPTURE_CHECKMARK_GROW_DURATION + CAPTURE_CHECKMARK_FADE_DURATION
private val HALF_PI_RADIANS = (PI / 2.0).toFloat()
private val PI_RADIANS = PI.toFloat()
private val THREE_HALVES_PI_RADIANS = (PI * 1.5).toFloat()
private val TWO_PI_RADIANS = (PI * 2.0).toFloat()

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
    val scanState = (selfieScannerState as? IdentityScanViewModel.State.Scanning)?.scanState
    val showPreCameraInstruction =
        selfieScannerState is IdentityScanViewModel.State.Scanning && scanState == null

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
            if (showPreCameraInstruction) {
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
            }

            val isCheckingImages = faceDetectorTransitioner != null
            val status = selfieScannerState.status()
            val isShowingSideCapturePrompt = scanState.isWaitingForSideCapturePrompt()
            SelfieCameraViewFinder(
                cameraManager = cameraManager,
                identityViewModel = identityViewModel,
                status = status,
                showCaptureGuideShadow = (selfieScannerState as? IdentityScanViewModel.State.Scanning)
                    ?.scanState is IdentityScanState.Found,
                captureGuideState = scanState.captureGuideState(),
                showCaptureGuide = !isCheckingImages,
                capturedSelfie = faceDetectorTransitioner?.lastCapturedSelfie(),
                isShowingSideCapturePrompt = isShowingSideCapturePrompt
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
    captureGuideState: CaptureGuideState,
    showCaptureGuide: Boolean,
    capturedSelfie: Bitmap?,
    isShowingSideCapturePrompt: Boolean
) {
    // Wait for camera adapter to be initialized before accessing lens model
    LaunchedEffect(cameraManager.cameraAdapter) {
        if (cameraManager.cameraAdapter != null) {
            // Camera is initialized, set the lens model
            identityViewModel.setSelfieCameraLensModel(cameraManager)
        }
    }

    AnnounceSelfieStatus(status)
    var retainedStatus by remember { mutableStateOf<SelfieStatus?>(null) }
    val statusOpacity = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status != null) {
            retainedStatus = status
        }
    }
    LaunchedEffect(status != null) {
        if (status != null) {
            statusOpacity.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = STATUS_PILL_FADE_IN_DURATION)
            )
        } else if (retainedStatus != null) {
            statusOpacity.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = STATUS_PILL_FADE_OUT_DURATION)
            )
            retainedStatus = null
        }
    }
    val displayedStatus = status ?: retainedStatus

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
            cameraManager = cameraManager,
            blurCamera = isShowingSideCapturePrompt || status?.isSideCaptured == true
        )
        if (showCaptureGuide) {
            CaptureGuide(showCaptureGuideShadow, captureGuideState)
            if (showCaptureGuideShadow) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(SELFIE_CAPTURE_GUIDE_SHADOW_TAG)
                )
            }
        }
        if (status?.isCaptured == true) {
            CapturedSelfieCheckmark(
                modifier = Modifier.align(Alignment.Center)
            )
        }
        displayedStatus?.let {
            val statusModifier = when {
                it == SelfieStatus.CheckingImages ||
                    (it.isSideCaptureInstruction && isShowingSideCapturePrompt) ->
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { alpha = statusOpacity.value }

                else ->
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                        .graphicsLayer { alpha = statusOpacity.value }
            }
            SelfieStatusBadge(
                status = it,
                showDirectionalArrow = it.isSideCaptureInstruction && isShowingSideCapturePrompt,
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
    cameraManager: IdentityCameraManager,
    blurCamera: Boolean
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
        val blurRadius by animateDpAsState(
            targetValue = if (blurCamera) 16.dp else 0.dp,
            animationSpec = tween(
                durationMillis = if (blurCamera) {
                    LIVE_PREVIEW_BLUR_IN_DURATION
                } else {
                    LIVE_PREVIEW_BLUR_OUT_DURATION
                }
            ),
            label = "selfie_live_preview_blur"
        )
        val cameraModifier = Modifier
            .fillMaxSize()
        val blurRadiusPx = with(LocalDensity.current) { blurRadius.toPx() }
        AndroidView(
            modifier = cameraModifier,
            factory = {
                CameraView(
                    it,
                    CameraView.ViewFinderType.Fill
                )
            },
            update = {
                cameraManager.onCameraViewUpdate(it)
                it.setLivePreviewBlur(blurRadiusPx)
            }
        )
    }
}

private fun CameraView.setLivePreviewBlur(radiusPx: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return
    }
    setRenderEffect(
        if (radiusPx > 0f) {
            RenderEffect.createBlurEffect(
                radiusPx,
                radiusPx,
                Shader.TileMode.CLAMP
            )
        } else {
            null
        }
    )
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

private val SelfieStatus.isCaptured: Boolean
    get() = when (this) {
        SelfieStatus.CapturedFront,
        SelfieStatus.CapturedLeft,
        SelfieStatus.CapturedRight -> true

        else -> false
    }

private val SelfieStatus.isSideCaptureInstruction: Boolean
    get() = this == SelfieStatus.LookLeft || this == SelfieStatus.LookRight

private val SelfieStatus.isSideCaptured: Boolean
    get() = this == SelfieStatus.CapturedLeft || this == SelfieStatus.CapturedRight

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
        null -> null
        is IdentityScanState.Initial -> when (transitioner?.activeCapture) {
            FaceDetectorTransitioner.Capture.LEFT -> SelfieStatus.LookLeft
            FaceDetectorTransitioner.Capture.RIGHT -> SelfieStatus.LookRight
            FaceDetectorTransitioner.Capture.FRONT,
            null -> SelfieStatus.PlaceFace
        }
        is IdentityScanState.Found -> SelfieStatus.HoldStill
        is IdentityScanState.Satisfied,
        is IdentityScanState.Finished -> when (transitioner?.completedCapture) {
            FaceDetectorTransitioner.Capture.LEFT -> SelfieStatus.CapturedLeft
            FaceDetectorTransitioner.Capture.RIGHT -> SelfieStatus.CapturedRight
            FaceDetectorTransitioner.Capture.FRONT,
            null -> SelfieStatus.CapturedFront
        }
        is IdentityScanState.TimeOut,
        is IdentityScanState.Unsatisfied -> null
    }
}

private data class CaptureGuideState(
    val uses3DCaptureAnimations: Boolean,
    val target: CaptureGuideTarget = CaptureGuideTarget.None,
    val targetProgress: Float = 0f,
    val highlight: CaptureGuideHighlight = CaptureGuideHighlight.None
)

private enum class CaptureGuideTarget {
    None,
    Left,
    Right
}

private enum class CaptureGuideHighlight {
    None,
    Front,
    Left,
    Right
}

private fun IdentityScanState?.captureGuideState(): CaptureGuideState {
    val transitioner = this?.transitioner as? FaceDetectorTransitioner
    val uses3DCaptureAnimations = transitioner?.uses3DFaceCapture == true
    val activeTarget = transitioner?.activeCapture?.toCaptureGuideTarget()
        ?: CaptureGuideTarget.None

    return when (this) {
        is IdentityScanState.Initial -> CaptureGuideState(
            uses3DCaptureAnimations = uses3DCaptureAnimations,
            target = if (uses3DCaptureAnimations) activeTarget else CaptureGuideTarget.None,
            targetProgress = if (uses3DCaptureAnimations) {
                transitioner?.captureGuideProgress ?: 0f
            } else {
                0f
            }
        )
        is IdentityScanState.Found -> CaptureGuideState(
            uses3DCaptureAnimations = uses3DCaptureAnimations,
            target = if (uses3DCaptureAnimations) activeTarget else CaptureGuideTarget.None,
            targetProgress = if (uses3DCaptureAnimations &&
                activeTarget != CaptureGuideTarget.None
            ) {
                1f
            } else {
                0f
            }
        )
        is IdentityScanState.Satisfied -> CaptureGuideState(
            uses3DCaptureAnimations = uses3DCaptureAnimations,
            highlight = transitioner?.completedCapture.toCaptureGuideHighlight()
        )
        else -> CaptureGuideState(uses3DCaptureAnimations = uses3DCaptureAnimations)
    }
}

private fun FaceDetectorTransitioner.Capture.toCaptureGuideTarget(): CaptureGuideTarget {
    return when (this) {
        FaceDetectorTransitioner.Capture.LEFT -> CaptureGuideTarget.Left
        FaceDetectorTransitioner.Capture.RIGHT -> CaptureGuideTarget.Right
        FaceDetectorTransitioner.Capture.FRONT -> CaptureGuideTarget.None
    }
}

private fun FaceDetectorTransitioner.Capture?.toCaptureGuideHighlight(): CaptureGuideHighlight {
    return when (this) {
        FaceDetectorTransitioner.Capture.FRONT -> CaptureGuideHighlight.Front
        FaceDetectorTransitioner.Capture.LEFT -> CaptureGuideHighlight.Left
        FaceDetectorTransitioner.Capture.RIGHT -> CaptureGuideHighlight.Right
        null -> CaptureGuideHighlight.None
    }
}

private fun IdentityScanState?.isWaitingForSideCapturePrompt(): Boolean {
    return (this?.transitioner as? FaceDetectorTransitioner)?.isWaitingForSideCapturePrompt == true
}

private fun FaceDetectorTransitioner.lastCapturedSelfie(): Bitmap {
    val frame = when (completedCapture) {
        FaceDetectorTransitioner.Capture.LEFT -> frameForSelfie(FaceDetectorTransitioner.Selfie.LEFT)
        FaceDetectorTransitioner.Capture.RIGHT -> frameForSelfie(FaceDetectorTransitioner.Selfie.RIGHT)
        FaceDetectorTransitioner.Capture.FRONT,
        null -> filteredFrames[FaceDetectorTransitioner.INDEX_LAST]
    }
    return frame.first.cameraPreviewImage.image
}

@Composable
private fun CapturedSelfieCheckmark(
    modifier: Modifier = Modifier
) {
    val timeline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        timeline.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = CAPTURE_CHECKMARK_TOTAL_DURATION,
                easing = LinearEasing
            )
        )
    }
    val elapsedMillis = timeline.value * CAPTURE_CHECKMARK_TOTAL_DURATION
    val growProgress = if (elapsedMillis <= CAPTURE_CHECKMARK_GROW_DURATION) {
        FastOutSlowInEasing.transform(elapsedMillis / CAPTURE_CHECKMARK_GROW_DURATION)
    } else {
        1f
    }
    val opacity = if (elapsedMillis <= CAPTURE_CHECKMARK_GROW_DURATION) {
        growProgress
    } else {
        1f - (
            (elapsedMillis - CAPTURE_CHECKMARK_GROW_DURATION) /
                CAPTURE_CHECKMARK_FADE_DURATION
            ).coerceIn(0f, 1f)
    }
    val scale = 0.72f + (0.28f * growProgress)

    Canvas(
        modifier = modifier
            .size(28.dp)
            .graphicsLayer {
                alpha = opacity
                scaleX = scale
                scaleY = scale
            }
            .testTag(SELFIE_CAPTURED_CHECK_TAG)
    ) {
        drawCircle(
            color = Color.White,
            radius = (size.minDimension / 2f) - 1.dp.toPx()
        )
        val checkPath = Path().apply {
            moveTo(size.width * 0.31f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.65f)
            lineTo(size.width * 0.70f, size.height * 0.38f)
        }
        drawPath(
            path = checkPath,
            color = Color(CAPTURE_GUIDE_ACTIVE_TICK_COLOR),
            style = Stroke(
                width = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
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
    showDirectionalArrow: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color(STATUS_PILL_BACKGROUND_COLOR),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                testTag = SELFIE_SCAN_STATUS_TAG
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDirectionalArrow && status == SelfieStatus.LookLeft) {
            DirectionalPromptArrow(status)
        }
        if (status.showsActivityIndicator) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(14.dp)
                    .testTag(SELFIE_SCAN_ACTIVITY_INDICATOR_TAG),
                color = Color.White,
                strokeWidth = 1.5f.dp
            )
        }
        Text(
            text = stringResource(id = status.labelRes),
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.body2.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = STATUS_PILL_TEXT_SHADOW_ALPHA),
                    offset = Offset(x = 0f, y = 1f),
                    blurRadius = 4f
                )
            )
        )
        if (showDirectionalArrow && status == SelfieStatus.LookRight) {
            DirectionalPromptArrow(status)
        }
    }
}

@Composable
private fun DirectionalPromptArrow(status: SelfieStatus) {
    val isLeft = status == SelfieStatus.LookLeft
    val transition = rememberInfiniteTransition(label = "selfie_turn_prompt_arrow")
    val outwardOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 5.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = TURN_PROMPT_ARROW_DURATION,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "selfie_turn_prompt_arrow_offset"
    )
    Text(
        text = if (isLeft) "←" else "→",
        modifier = Modifier
            .padding(
                start = if (isLeft) 0.dp else 4.dp,
                end = if (isLeft) 4.dp else 0.dp
            )
            .graphicsLayer {
                translationX = if (isLeft) {
                    -outwardOffset.dp.toPx()
                } else {
                    outwardOffset.dp.toPx()
                }
            },
        color = Color.White,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun CaptureGuide(
    showCenteredShadow: Boolean,
    captureGuideState: CaptureGuideState
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

        val tickGeometry = captureGuideGeometry(
            horizontalDiameterRatio = CAPTURE_GUIDE_TICK_HORIZONTAL_DIAMETER_RATIO,
            verticalDiameterRatio = CAPTURE_GUIDE_TICK_VERTICAL_DIAMETER_RATIO
        )
        if (centeredShadowAlpha.value > 0f) {
            val shadowGeometry = captureGuideGeometry(
                horizontalDiameterRatio = CAPTURE_GUIDE_SHADOW_HORIZONTAL_DIAMETER_RATIO,
                verticalDiameterRatio = CAPTURE_GUIDE_SHADOW_VERTICAL_DIAMETER_RATIO
            )
            drawCenteredGuideShadow(
                center = shadowGeometry.center,
                horizontalRadius = shadowGeometry.horizontalRadius,
                verticalRadius = shadowGeometry.verticalRadius,
                opacity = centeredShadowAlpha.value
            )
        }

        drawCaptureGuideTicks(
            geometry = tickGeometry,
            captureGuideState = captureGuideState
        )
    }
}

private data class CaptureGuideGeometry(
    val center: Offset,
    val horizontalRadius: Float,
    val verticalRadius: Float
)

private fun DrawScope.captureGuideGeometry(
    horizontalDiameterRatio: Float,
    verticalDiameterRatio: Float
): CaptureGuideGeometry {
    return CaptureGuideGeometry(
        center = Offset(
            x = size.width / 2f,
            y = size.height * CAPTURE_GUIDE_CENTER_Y_RATIO
        ),
        horizontalRadius = size.width * horizontalDiameterRatio / 2f,
        verticalRadius = size.height * verticalDiameterRatio / 2f
    )
}

private fun DrawScope.drawCaptureGuideTicks(
    geometry: CaptureGuideGeometry,
    captureGuideState: CaptureGuideState
) {
    with(geometry) {
        val baseTickLength = CAPTURE_GUIDE_BASE_TICK_LENGTH_DP.dp.toPx()
        val acceptedTickLength = CAPTURE_GUIDE_ACCEPTED_TICK_LENGTH_DP.dp.toPx()
        val baseStrokeWidth = CAPTURE_GUIDE_BASE_TICK_STROKE_WIDTH_DP.dp.toPx()
        val acceptedStrokeWidth = CAPTURE_GUIDE_ACCEPTED_TICK_STROKE_WIDTH_DP.dp.toPx()
        val shadowOffsetY = 1.dp.toPx()
        val shadowBlur = 4.dp.toPx()
        drawIntoCanvas { canvas ->
            val baseTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = baseStrokeWidth
                setShadowLayer(
                    shadowBlur,
                    0f,
                    shadowOffsetY,
                    Color.Black.copy(alpha = CAPTURE_GUIDE_TICK_SHADOW_ALPHA).toArgb()
                )
            }
            val acceptedTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color(CAPTURE_GUIDE_ACTIVE_TICK_COLOR).toArgb()
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = acceptedStrokeWidth
            }

            repeat(CAPTURE_GUIDE_TICK_COUNT) { index ->
                val tick = captureGuideTick(index)
                val isAcceptedByTarget = captureGuideState.uses3DCaptureAnimations &&
                    captureGuideState.target.isProgressRevealedTick(
                        angle = tick.angle,
                        targetProgress = captureGuideState.targetProgress
                    )
                val isAcceptedByHighlight =
                    captureGuideState.highlight.isHighlightTick(tick.angle)

                if (!isAcceptedByTarget && !isAcceptedByHighlight) {
                    baseTickPaint.color = Color.White.copy(
                        alpha = captureGuideState.baseTickAlpha(
                            angle = tick.angle
                        )
                    ).toArgb()
                    drawGuideTick(
                        canvas = canvas.nativeCanvas,
                        tick = tick,
                        length = baseTickLength,
                        baseLength = baseTickLength,
                        growsOutward = false,
                        paint = baseTickPaint
                    )
                }

                if (isAcceptedByTarget || isAcceptedByHighlight) {
                    drawGuideTick(
                        canvas = canvas.nativeCanvas,
                        tick = tick,
                        length = acceptedTickLength,
                        baseLength = baseTickLength,
                        growsOutward = captureGuideState.uses3DCaptureAnimations ||
                            isAcceptedByTarget,
                        paint = acceptedTickPaint
                    )
                }
            }
        }
    }
}

private data class CaptureGuideTick(
    val angle: Float,
    val center: Offset,
    val unitNormalX: Float,
    val unitNormalY: Float
)

private fun CaptureGuideGeometry.captureGuideTick(index: Int): CaptureGuideTick {
    val angle = (index.toFloat() / CAPTURE_GUIDE_TICK_COUNT.toFloat()) * TWO_PI_RADIANS
    val cosAngle = cos(angle)
    val sinAngle = sin(angle)
    val tickCenter = Offset(
        x = center.x + cosAngle * horizontalRadius,
        y = center.y + sinAngle * verticalRadius
    )
    val normalX = cosAngle / horizontalRadius
    val normalY = sinAngle / verticalRadius
    val normalLength = sqrt((normalX * normalX) + (normalY * normalY))

    return CaptureGuideTick(
        angle = angle,
        center = tickCenter,
        unitNormalX = normalX / normalLength,
        unitNormalY = normalY / normalLength
    )
}

private fun drawGuideTick(
    canvas: android.graphics.Canvas,
    tick: CaptureGuideTick,
    length: Float,
    baseLength: Float,
    growsOutward: Boolean,
    paint: Paint
) {
    val innerLength = if (growsOutward) {
        baseLength / 2f
    } else {
        length / 2f
    }
    val outerLength = if (growsOutward) {
        length - (baseLength / 2f)
    } else {
        length / 2f
    }

    canvas.drawLine(
        tick.center.x - tick.unitNormalX * innerLength,
        tick.center.y - tick.unitNormalY * innerLength,
        tick.center.x + tick.unitNormalX * outerLength,
        tick.center.y + tick.unitNormalY * outerLength,
        paint
    )
}

private fun CaptureGuideState.baseTickAlpha(angle: Float): Float {
    if (!uses3DCaptureAnimations || target == CaptureGuideTarget.None) {
        return CAPTURE_GUIDE_TICK_ALPHA
    }

    val oppositePoleAngle = when (target) {
        CaptureGuideTarget.Left -> 0f
        CaptureGuideTarget.Right -> PI_RADIANS
        CaptureGuideTarget.None -> return CAPTURE_GUIDE_TICK_ALPHA
    }
    val wrongSideStrength = max(0f, cos(angle - oppositePoleAngle)).pow(0.25f)
    val dimmingMultiplier = 1f - (0.98f * wrongSideStrength)

    return CAPTURE_GUIDE_TICK_ALPHA *
        dimmingMultiplier.coerceIn(0f, 1f)
}

private fun CaptureGuideTarget.isTargetHalfTick(angle: Float): Boolean {
    return when (this) {
        CaptureGuideTarget.Left -> angle >= HALF_PI_RADIANS && angle <= THREE_HALVES_PI_RADIANS
        CaptureGuideTarget.Right -> angle <= HALF_PI_RADIANS || angle >= THREE_HALVES_PI_RADIANS
        CaptureGuideTarget.None -> false
    }
}

private fun CaptureGuideTarget.isProgressRevealedTick(
    angle: Float,
    targetProgress: Float
): Boolean {
    if (targetProgress <= 0f || !isTargetHalfTick(angle)) {
        return false
    }

    val poleAngle = when (this) {
        CaptureGuideTarget.Left -> PI_RADIANS
        CaptureGuideTarget.Right -> 0f
        CaptureGuideTarget.None -> return false
    }
    val distanceFromPole = abs(atan2(sin(angle - poleAngle), cos(angle - poleAngle)))
    val hiddenAngle = (1f - targetProgress.coerceIn(0f, 1f)) * HALF_PI_RADIANS

    return hiddenAngle <= distanceFromPole && distanceFromPole <= HALF_PI_RADIANS
}

private fun CaptureGuideHighlight.isHighlightTick(angle: Float): Boolean {
    return when (this) {
        CaptureGuideHighlight.Front -> true
        CaptureGuideHighlight.Left -> angle > HALF_PI_RADIANS && angle < THREE_HALVES_PI_RADIANS
        CaptureGuideHighlight.Right -> angle < HALF_PI_RADIANS || angle > THREE_HALVES_PI_RADIANS
        CaptureGuideHighlight.None -> false
    }
}
