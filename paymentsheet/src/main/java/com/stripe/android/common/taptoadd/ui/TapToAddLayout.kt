package com.stripe.android.common.taptoadd.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun TapToAddLayout(
    screen: TapToAddNavigator.Screen,
    onCancel: (TapToAddNavigator.Action) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)
    ) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(ANIMATION_DURATION))
                        .togetherWith(fadeOut(animationSpec = tween(ANIMATION_DURATION)))
                },
            ) { screen ->
                val sharedTransitionScope = this@SharedTransitionLayout
                val animatedVisibilityScope = this

                val scope = remember(sharedTransitionScope, animatedVisibilityScope) {
                    SharedElementScope(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }

                BoxWithConstraints {
                    CompositionLocalProvider(
                        LocalSharedElementScope provides scope,
                        LocalTapToAddMaxContentHeight provides maxHeight,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp)
                                .windowInsetsPadding(WindowInsets.safeDrawing),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CancelButton(
                                buttonFlow = screen.cancelButton,
                                onClick = onCancel,
                            )

                            screen.ScreenContent(this)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelButton(
    buttonFlow: StateFlow<TapToAddNavigator.CancelButton>,
    onClick: (TapToAddNavigator.Action) -> Unit,
) {
    val button by buttonFlow.collectAsState()

    val sizeModifier = Modifier.size(50.dp)

    when (val currentButtonState = button) {
        is TapToAddNavigator.CancelButton.None -> Unit
        is TapToAddNavigator.CancelButton.Invisible -> {
            Spacer(sizeModifier)
            Spacer(Modifier.size(20.dp))
        }
        is TapToAddNavigator.CancelButton.Available -> {
            val sharedElementScope = LocalSharedElementScope.current

            sharedElementScope?.let {
                with(sharedElementScope.sharedTransitionScope) {
                    VisibleCancelButton(
                        modifier = Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(SHARED_CANCEL_BUTTON_KEY),
                                animatedVisibilityScope = sharedElementScope.animatedVisibilityScope
                            )
                            .then(sizeModifier),
                    ) {
                        onClick(currentButtonState.action)
                    }
                }
            } ?: run {
                VisibleCancelButton(sizeModifier) {
                    onClick(currentButtonState.action)
                }
            }

            Spacer(Modifier.size(20.dp))
        }
    }
}

@Composable
private fun VisibleCancelButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val closeResource = if (isSystemInDarkTheme()) {
        R.drawable.stripe_ic_paymentsheet_tta_close_dark
    } else {
        R.drawable.stripe_ic_paymentsheet_tta_close_light
    }

    Box(Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(closeResource),
            contentDescription = stringResource(com.stripe.android.R.string.stripe_close),
            modifier = modifier
                .clickable(
                    enabled = true,
                    role = Role.Button,
                    onClick = onClick,
                )
        )
    }
}

internal val LocalTapToAddMaxContentHeight = staticCompositionLocalOf { 0.dp }

private const val SHARED_CANCEL_BUTTON_KEY = "STRIPE_TTA_CANCEL_BUTTON"
private const val ANIMATION_DURATION = 300
