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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun TapToAddLayout(
    screen: TapToAddNavigator.Screen,
    onCancel: () -> Unit,
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
                                .windowInsetsPadding(WindowInsets.safeDrawing),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (screen.isCentered) {
                                Spacer(Modifier.weight(1f))
                            }

                            Box(Modifier.padding(20.dp)) {
                                screen.Content()
                            }

                            Spacer(Modifier.weight(1f))

                            CancelButton(
                                button = screen.cancelButton,
                                onClick = onCancel,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.CancelButton(
    button: TapToAddNavigator.CancelButton,
    onClick: () -> Unit,
) {
    val modifier = Modifier
        .padding(
            top = 20.dp,
            bottom = 50.dp,
            start = 20.dp,
            end = 20.dp,
        )
        .align(Alignment.CenterHorizontally)

    when (button) {
        TapToAddNavigator.CancelButton.None -> Unit
        TapToAddNavigator.CancelButton.Invisible -> {
            Spacer(modifier.size(40.dp))
        }
        TapToAddNavigator.CancelButton.Visible -> {
            Image(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_tta_close),
                contentDescription = stringResource(com.stripe.android.R.string.stripe_close),
                modifier = modifier
                    .size(40.dp)
                    .clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = onClick,
                    )
            )
        }
    }
}

internal val LocalTapToAddMaxContentHeight = staticCompositionLocalOf { 0.dp }

private const val ANIMATION_DURATION = 300
