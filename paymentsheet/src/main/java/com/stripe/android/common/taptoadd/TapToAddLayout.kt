package com.stripe.android.common.taptoadd

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
    onCancel: () -> Unit,
    screen: TapToAddScreen,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .padding(20.dp)
                    .weight(1f)
            ) {
                SharedTransitionLayout {
                    AnimatedContent(
                        modifier = Modifier.fillMaxSize(),
                        targetState = screen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }
                    ) {
                        screen.Content(
                            animatedContentScope = this@AnimatedContent,
                            sharedTransitionScope = this@SharedTransitionLayout,
                        )
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .padding(
                        top = 20.dp,
                        bottom = 50.dp,
                        start = 20.dp,
                        end = 20.dp,
                    )
                    .align(Alignment.CenterHorizontally),
                visible = screen.hasCancelButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Image(
                    painter = painterResource(R.drawable.stripe_ic_paymentsheet_tta_close),
                    contentDescription = stringResource(com.stripe.android.R.string.stripe_close),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            enabled = true,
                            role = Role.Button,
                            onClick = onCancel,
                        )
                )
            }

        }
    }
}
