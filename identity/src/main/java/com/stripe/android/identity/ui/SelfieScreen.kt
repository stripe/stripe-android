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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.composethemeadapter.MdcTheme
import com.stripe.android.camera.framework.image.mirrorHorizontally
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.uicore.text.Html
import com.stripe.android.uicore.text.dimensionResourceSp

internal const val SELFIE_VIEW_FINDER_ASPECT_RATIO = 1f
internal const val SELFIE_SCAN_TITLE_TAG = "SelfieScanTitle"
internal const val SELFIE_SCAN_MESSAGE_TAG = "SelfieScanMessage"
internal const val SELFIE_SCAN_CONTINUE_BUTTON_TAG = "SelfieScanContinue"
internal const val SCAN_VIEW_TAG = "SelfieScanViewTag"
internal const val RESULT_VIEW_TAG = "SelfieResultViewTag"
internal const val CONSENT_CHECKBOX_TAG = "ConsentCheckboxTag"
private const val FLASH_MAX_ALPHA = 0.5f
private const val FLASH_ANIMATION_TIME = 200

@Composable
internal fun SelfieScanScreen(
    title: String,
    message: String,
    verificationPageState: Resource<VerificationPage>,
    onError: (Throwable) -> Unit,
    newDisplayState: IdentityScanState?,
    onCameraViewCreated: (CameraView) -> Unit,
    onContinueClicked: (Boolean) -> Unit
) {
    MdcTheme {
        CheckVerificationPageAndCompose(
            verificationPageResource = verificationPageState,
            onError = onError
        ) {
            val successSelfieCapturePage =
                remember {
                    requireNotNull(it.selfieCapture) {
                        onError(IllegalStateException("VerificationPage.selfieCapture is null"))
                    }
                }

            var loadingButtonState by remember(newDisplayState) {
                mutableStateOf(
                    if (newDisplayState is IdentityScanState.Finished) {
                        LoadingButtonState.Idle
                    } else {
                        LoadingButtonState.Disabled
                    }
                )
            }

            var allowImageCollection by remember {
                mutableStateOf(false)
            }

            var allowImageCollectionCheckboxEnabled by remember {
                mutableStateOf(true)
            }

            var flashed by remember {
                mutableStateOf(false)
            }

            val imageAlpha: Float by animateFloatAsState(
                targetValue = if (!flashed && newDisplayState is IdentityScanState.Found) FLASH_MAX_ALPHA else 0f,
                animationSpec = tween(
                    durationMillis = FLASH_ANIMATION_TIME,
                    easing = LinearEasing,
                ),
                finishedListener = {
                    flashed = true
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        vertical = dimensionResource(id = R.dimen.page_vertical_margin)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(id = R.dimen.page_horizontal_margin)
                            )
                            .semantics {
                                testTag = SELFIE_SCAN_TITLE_TAG
                            },
                        fontSize = dimensionResourceSp(id = R.dimen.scan_title_text_size),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(
                                top = 20.dp,
                                bottom = dimensionResource(id = R.dimen.item_vertical_margin),
                                start = dimensionResource(id = R.dimen.page_horizontal_margin),
                                end = dimensionResource(id = R.dimen.page_horizontal_margin)
                            )
                            .semantics {
                                testTag = SELFIE_SCAN_MESSAGE_TAG
                            },
                        maxLines = 3
                    )

                    if (newDisplayState is IdentityScanState.Finished) {
                        ResultView(
                            displayState = newDisplayState,
                            allowImageCollectionHtml = successSelfieCapturePage.consentText,
                            allowImageCollectionCheckboxEnabled = allowImageCollectionCheckboxEnabled,
                            allowImageCollection = allowImageCollection,
                        ) {
                            allowImageCollection = it
                        }
                    } else {
                        SelfieCameraViewFinder(onCameraViewCreated, imageAlpha)
                    }
                }
                LoadingButton(
                    modifier = Modifier
                        .testTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG)
                        .padding(dimensionResource(id = R.dimen.page_horizontal_margin)),
                    text = stringResource(id = R.string.kontinue).uppercase(),
                    state = loadingButtonState
                ) {
                    loadingButtonState = LoadingButtonState.Loading
                    allowImageCollectionCheckboxEnabled = false
                    onContinueClicked(allowImageCollection)
                }
            }
        }
    }
}

@Composable
private fun ResultView(
    displayState: IdentityScanState,
    allowImageCollectionHtml: String,
    allowImageCollectionCheckboxEnabled: Boolean,
    allowImageCollection: Boolean,
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
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen.view_finder_corner_radius))),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(id = R.string.selfie_item_description)
            )
        }
    }

    Row(
        modifier = Modifier.padding(
            start = dimensionResource(id = R.dimen.page_horizontal_margin),
            end = dimensionResource(id = R.dimen.page_horizontal_margin),
            top = 20.dp
        )
    ) {
        Checkbox(
            modifier = Modifier.testTag(CONSENT_CHECKBOX_TAG),
            checked = allowImageCollection,
            onCheckedChange = {
                onAllowImageCollectionChanged(!allowImageCollection)
            },
            enabled = allowImageCollectionCheckboxEnabled
        )

        Html(
            html = allowImageCollectionHtml,
            urlSpanStyle = SpanStyle(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colors.secondary
            )
        )
    }
}

@Composable
private fun SelfieCameraViewFinder(
    onCameraViewCreated: (CameraView) -> Unit,
    imageAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(SELFIE_VIEW_FINDER_ASPECT_RATIO)
            .padding(
                horizontal = dimensionResource(id = R.dimen.page_horizontal_margin)
            )
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.view_finder_corner_radius)))
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
                onCameraViewCreated(it)
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(imageAlpha)
                .background(colorResource(id = R.color.flash_mask_color))
        )
    }
}
