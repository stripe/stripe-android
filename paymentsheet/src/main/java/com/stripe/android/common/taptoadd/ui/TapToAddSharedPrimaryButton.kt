package com.stripe.android.common.taptoadd.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun TapToAddSharedPrimaryButton(
    content: @Composable (modifier: Modifier) -> Unit,
) {
    val sharedElementScope = LocalSharedElementScope.current

    sharedElementScope?.let {
        with(sharedElementScope.sharedTransitionScope) {
            content(
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(SHARED_PRIMARY_BUTTON_ELEMENT_KEY),
                    animatedVisibilityScope = sharedElementScope.animatedVisibilityScope,
                )
            )
        }
    } ?: run {
        content(Modifier)
    }
}

private const val SHARED_PRIMARY_BUTTON_ELEMENT_KEY = "STRIPE_TTA_PRIMARY_BUTTON"
