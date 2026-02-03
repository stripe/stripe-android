package com.stripe.android.common.taptoadd

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState

@OptIn(ExperimentalSharedTransitionApi::class)
internal sealed interface TapToAddScreen {
    val hasCancelButton: Boolean
    val isPerformingNetworkOperation: Boolean

    @Composable
    fun Content(
        sharedTransitionScope: SharedTransitionScope,
        animatedContentScope: AnimatedContentScope,
    )

    data object Collecting : TapToAddScreen {
        override val hasCancelButton: Boolean = false
        override val isPerformingNetworkOperation: Boolean = true

        @Composable
        override fun Content(
            sharedTransitionScope: SharedTransitionScope,
            animatedContentScope: AnimatedContentScope,
        ) {
            LoadingScreen()
        }
    }

    data class Error(
        val message: ResolvableString
    ) : TapToAddScreen {
        override val hasCancelButton: Boolean = true
        override val isPerformingNetworkOperation: Boolean = false

        @Composable
        override fun Content(
            sharedTransitionScope: SharedTransitionScope,
            animatedContentScope: AnimatedContentScope,
        ) {
            ErrorScreen(message)
        }
    }

    data class Collected(
        val brand: CardBrand,
        val last4: String?
    ) : TapToAddScreen {
        override val hasCancelButton: Boolean = false
        override val isPerformingNetworkOperation: Boolean = false

        @Composable
        override fun Content(
            sharedTransitionScope: SharedTransitionScope,
            animatedContentScope: AnimatedContentScope,
        ) {
            CollectedScreen(
                animatedContentScope = animatedContentScope,
                sharedTransitionScope = sharedTransitionScope,
                brand = brand,
                last4 = last4
            )
        }
    }

    data class Confirmation(
        val interactor: TapToAddConfirmationInteractor,
    ) : TapToAddScreen {
        override val hasCancelButton: Boolean = true
        override val isPerformingNetworkOperation: Boolean
            get() = interactor.state.value.state !is
                TapToAddConfirmationInteractor.State.ConfirmationState.Idle

        @Composable
        override fun Content(
            sharedTransitionScope: SharedTransitionScope,
            animatedContentScope: AnimatedContentScope,
        ) {
            val state by interactor.state.collectAsState()

            ConfirmationScreen(
                animatedContentScope = animatedContentScope,
                sharedTransitionScope = sharedTransitionScope,
                state = state,
                onClick = {
                    interactor.onAction(
                        action = TapToAddConfirmationInteractor.Action.OnPrimaryButtonPress,
                    )
                },
                onAnimationComplete = {
                    interactor.onAction(
                        action = TapToAddConfirmationInteractor.Action.OnPrimaryButtonAnimationComplete,
                    )
                }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize()) {
        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ErrorScreen(message: ResolvableString) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_ic_paymentsheet_tta_error),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier.size(50.dp)
        )

        Spacer(Modifier.size(40.dp))

        Text(
            text = "Error",
            style = MaterialTheme.typography.h4.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colors.onSurface,
        )

        Spacer(Modifier.size(10.dp))

        Text(
            text = message.resolve(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h4.copy(
                fontWeight = FontWeight.W300,
            ),
            color = MaterialTheme.colors.onSurface,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CollectedScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    brand: CardBrand,
    last4: String?,
) {
    CardLayout(
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
        brand = brand,
        last4 = last4,
    ) {
        Title(
            text = buildAnnotatedString {
                append(stringResource(R.string.stripe_tta_card_added_title))
                append(" ")
                appendInlineContent(CHECKMARK_ID)
            },
            inlineContent = mapOf(
                CHECKMARK_ID to InlineTextContent(
                    placeholder = Placeholder(
                        width = MaterialTheme.typography.h5.fontSize,
                        height = MaterialTheme.typography.h5.fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Icon(
                        painter = painterResource(com.stripe.android.uicore.R.drawable.stripe_ic_checkmark),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface
                    )
                }
            ),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ConfirmationScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    state: TapToAddConfirmationInteractor.State,
    onAnimationComplete: () -> Unit,
    onClick: () -> Unit,
) {
    CardLayout(
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
        brand = state.brand,
        last4 = state.last4,
    ) {
        Title(
            text = buildAnnotatedString {
                append(state.title.resolve())
            },
        )

        Spacer(Modifier.size(20.dp))

        PrimaryButton(
            label = state.buttonLabel.resolve(),
            locked = false,
            enabled = true,
            processingState = when (val confirmationState = state.state) {
                is TapToAddConfirmationInteractor.State.ConfirmationState.Idle -> {
                    PrimaryButtonProcessingState.Idle(confirmationState.message)
                }
                is TapToAddConfirmationInteractor.State.ConfirmationState.Processing -> {
                    PrimaryButtonProcessingState.Processing
                }
                is TapToAddConfirmationInteractor.State.ConfirmationState.Completed -> {
                    PrimaryButtonProcessingState.Completed
                }
            },
            onProcessingCompleted = onAnimationComplete,
            onClick = onClick,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CardLayout(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    brand: CardBrand,
    last4: String?,
    bottomContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        with(sharedTransitionScope) {
            Column(
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "tta_card_image"),
                    animatedVisibilityScope = animatedContentScope,
                )
            ) {
                Spacer(Modifier.size(50.dp))

                Card(brand, last4)

                Spacer(Modifier.size(20.dp))
            }
        }

        bottomContent()
    }
}

@Composable
private fun Title(
    text: AnnotatedString,
    inlineContent: Map<String, InlineTextContent> = emptyMap()
) {
    Text(
        text = text,
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.h4.copy(
            fontWeight = FontWeight.Normal,
        ),
        inlineContent = inlineContent,
    )
}

@Stable
@Composable
private fun Card(
    brand: CardBrand,
    last4: String?,
) {
    Box(
        Modifier
            .background(
                shape = RoundedCornerShape(10.dp),
                color = Color.Red,
            )
            .fillMaxWidth()
            .height(200.dp)
            .padding(10.dp)
    ) {
        Icon(
            painter = painterResource(brand.icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        Text(
            text = stringResource(
                id = R.string.stripe_paymentsheet_payment_method_item_card_number,
                last4 ?: ""
            ),
            style = MaterialTheme.typography.h5,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

private const val CHECKMARK_ID = "STRIPE_TTA_CARD_ADDED_CHECKMARK"
