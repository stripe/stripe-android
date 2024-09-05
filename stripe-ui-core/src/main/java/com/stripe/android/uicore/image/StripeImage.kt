package com.stripe.android.uicore.image

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp.Companion.Infinity
import androidx.compose.ui.unit.IntSize.Companion.Zero
import com.stripe.android.uicore.image.StripeImageState.Error
import com.stripe.android.uicore.image.StripeImageState.Loading
import com.stripe.android.uicore.image.StripeImageState.Success

/**
 * A composable that executes an image request asynchronously using the
 * provided [StripeImageLoader] and renders the result.
 *
 * @param url to be requested and rendered.
 * @param contentDescription Text used by accessibility services to describe what this image
 *  represents. This should always be provided unless this image is used for decorative purposes,
 *  and does not represent a meaningful action that a user can take.
 * @param imageLoader The [StripeImageLoader] that will be used to execute the request.
 * @param debugPainter If provided, this painter will be render on Compose previews.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content.
 * @param disableAnimations If true, there will be no animations between icon states.
 * @param errorContent content to render when image loading fails.
 * @param loadingContent content to render when image loads.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *  used if the bounds are a different size from the intrinsic size of the painter.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun StripeImage(
    url: String,
    imageLoader: StripeImageLoader,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null,
    debugPainter: Painter? = null,
    alignment: Alignment = Alignment.Center,
    disableAnimations: Boolean = false,
    errorContent: @Composable BoxWithConstraintsScope.() -> Unit = {},
    loadingContent: @Composable BoxWithConstraintsScope.() -> Unit = {}
) {
    BoxWithConstraints {
        val debugMode = LocalInspectionMode.current
        val (width, height) = calculateBoxSize()
        val state: MutableState<StripeImageState> = remember {
            if (debugMode && debugPainter != null) {
                mutableStateOf(Success(debugPainter))
            } else {
                mutableStateOf(Loading)
            }
        }
        LaunchedEffect(url) {
            imageLoader
                .load(url, width, height)
                .onSuccess {
                    it?.let { bitmap ->
                        state.value = Success(BitmapPainter(bitmap.asImageBitmap()))
                    }
                }
                .onFailure {
                    state.value = Error
                }
        }
        AnimatedContent(
            targetState = state.value,
            label = "loading_image_animation",
            contentKey = { targetState ->
                if (disableAnimations) {
                    // Animations only occur when the content key changes, by setting the content key to a constant
                    // value here, we can effectively disable animations.
                    true
                } else {
                    targetState
                }
            }
        ) {
            when (it) {
                Error -> errorContent()
                Loading -> loadingContent()
                is Success -> Image(
                    modifier = modifier.testTag(TEST_TAG_IMAGE_FROM_URL),
                    colorFilter = colorFilter,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    alignment = alignment,
                    painter = it.painter
                )
            }
        }
    }
}

private fun BoxWithConstraintsScope.calculateBoxSize(): Pair<Int, Int> {
    var width =
        if (constraints.maxWidth > Zero.width &&
            constraints.maxWidth < Infinity.value.toInt()
        ) {
            constraints.maxWidth
        } else {
            -1
        }

    var height =
        if (constraints.maxHeight > Zero.height &&
            constraints.maxHeight < Infinity.value.toInt()
        ) {
            constraints.maxHeight
        } else {
            -1
        }

    // if height xor width not able to be determined, make image a square of the determined dimension
    if (width == -1) width = height
    if (height == -1) height = width
    return Pair(width, height)
}

private sealed class StripeImageState {
    object Loading : StripeImageState()
    data class Success(val painter: Painter) : StripeImageState()
    object Error : StripeImageState()
}

@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_IMAGE_FROM_URL = "StripeImageFromUrl"
