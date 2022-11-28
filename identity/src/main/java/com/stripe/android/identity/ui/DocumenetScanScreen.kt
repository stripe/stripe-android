package com.stripe.android.identity.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
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
import com.google.android.material.composethemeadapter.MdcTheme
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.R
import com.stripe.android.identity.states.IdentityScanState

internal const val CONTINUE_BUTTON_TAG = "Continue"
internal const val SCAN_TITLE_TAG = "Title"
internal const val SCAN_MESSAGE_TAG = "Message"
internal const val CHECK_MARK_TAG = "CheckMark"
internal const val VIEW_FINDER_ASPECT_RATIO = 1.5f

@Composable
internal fun DocumentScanScreen(
    title: String,
    message: String,
    newDisplayState: IdentityScanState?,
    onCameraViewCreated: (CameraView) -> Unit,
    onContinueClicked: () -> Unit
) {
    MdcTheme {
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

                CameraViewFinder(onCameraViewCreated, newDisplayState)
            }
            LoadingButton(
                modifier = Modifier.testTag(CONTINUE_BUTTON_TAG),
                text = stringResource(id = R.string.kontinue).uppercase(),
                state = loadingButtonState
            ) {
                loadingButtonState = LoadingButtonState.Loading
                onContinueClicked()
            }
        }
    }
}

@Composable
private fun CameraViewFinder(
    onCameraViewCreated: (CameraView) -> Unit,
    newScanState: IdentityScanState?
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
            update = {
                onCameraViewCreated(it)
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
                    contentDescription = stringResource(id = R.string.check_mark),
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                )
            }
        }
    }
}
