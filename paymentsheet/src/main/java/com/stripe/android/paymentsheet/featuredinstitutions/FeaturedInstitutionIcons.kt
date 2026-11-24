package com.stripe.android.paymentsheet.featuredinstitutions

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay

private val IconSize = 24.dp
private val IconCornerRadius = 6.dp
private val IconSpacing = 4.dp
private const val FIXED_ICON_COUNT = 3
private val FullWidth = IconSize * (FIXED_ICON_COUNT + 1) + IconSpacing * FIXED_ICON_COUNT

private const val FADE_IN_DURATION_MILLIS = 250
private const val ROTATION_INTERVAL_MILLIS = 2000L
private const val ROTATION_DURATION_MILLIS = 500
private val RotationEasing = CubicBezierEasing(0.2f, 1f, 0.25f, 1f)

/**
 * Featured bank institution icons: 3 fixed icons followed by a slot that rotates through the remaining ones.
 *
 * Shows nothing until the icons are loaded. With less room than all 4 slots, only the rotating slot shows and it
 * cycles through every icon. With less room than one icon, nothing shows.
 */
@Composable
internal fun FeaturedInstitutionIcons(
    featuredInstitutions: FeaturedInstitutions,
    modifier: Modifier = Modifier,
) {
    val icons by featuredInstitutions.icons.collectAsState()

    // Icons that are already loaded when this is first composed show immediately, later ones fade in.
    AnimatedVisibility(
        visible = icons.isNotEmpty(),
        enter = fadeIn(tween(FADE_IN_DURATION_MILLIS)),
        exit = ExitTransition.None,
        modifier = modifier.clearAndSetSemantics {},
    ) {
        BoxWithConstraints {
            when {
                maxWidth >= FullWidth -> {
                    ReportShown(featuredInstitutions)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(IconSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        icons.take(FIXED_ICON_COUNT).forEach { icon ->
                            InstitutionIcon(icon)
                        }
                        RotatingInstitutionIcon(icons.drop(FIXED_ICON_COUNT))
                    }
                }
                maxWidth >= IconSize -> {
                    ReportShown(featuredInstitutions)
                    RotatingInstitutionIcon(icons)
                }
            }
        }
    }
}

@Composable
private fun ReportShown(featuredInstitutions: FeaturedInstitutions) {
    LaunchedEffect(featuredInstitutions) {
        featuredInstitutions.reportShown()
    }
}

@Composable
private fun RotatingInstitutionIcon(icons: List<FeaturedInstitutionIcon>) {
    if (icons.isEmpty()) {
        return
    }

    var index by remember { mutableIntStateOf(0) }
    val canRotate = icons.size > 1
    val lifecycleOwner = LocalLifecycleOwner.current

    // Only rotates while composed and the screen is started, so nothing runs while hidden or detached.
    LaunchedEffect(canRotate, lifecycleOwner) {
        if (canRotate) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(ROTATION_INTERVAL_MILLIS)
                    index++
                }
            }
        }
    }

    val reduceMotion = areAnimationsReduced()

    AnimatedContent(
        targetState = icons[index % icons.size],
        transitionSpec = { rotationTransform(reduceMotion) },
        contentKey = { it.id },
        modifier = Modifier
            .size(IconSize)
            .clipToBounds(),
        label = "FeaturedInstitutionRotation",
    ) { icon ->
        InstitutionIcon(icon)
    }
}

private fun rotationTransform(reduceMotion: Boolean): ContentTransform {
    val fadeIn = fadeIn(tween(ROTATION_DURATION_MILLIS, easing = RotationEasing))
    val fadeOut = fadeOut(tween(ROTATION_DURATION_MILLIS, easing = RotationEasing))

    if (reduceMotion) {
        return fadeIn togetherWith fadeOut
    }

    val slideIn = slideInVertically(tween(ROTATION_DURATION_MILLIS, easing = RotationEasing)) { height -> height }
    val slideOut = slideOutVertically(tween(ROTATION_DURATION_MILLIS, easing = RotationEasing)) { height -> -height }
    return (slideIn + fadeIn) togetherWith (slideOut + fadeOut)
}

@Composable
private fun InstitutionIcon(icon: FeaturedInstitutionIcon) {
    Image(
        bitmap = icon.bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(IconSize)
            .clip(RoundedCornerShape(IconCornerRadius)),
    )
}

@Composable
private fun areAnimationsReduced(): Boolean {
    val context = LocalContext.current

    val animationScale = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }

    return LocalInspectionMode.current || animationScale <= 0f
}
