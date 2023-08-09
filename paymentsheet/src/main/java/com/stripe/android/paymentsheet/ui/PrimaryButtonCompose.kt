package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.PrimaryButtonStyle
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBackgroundColor
import com.stripe.android.uicore.getBorderStrokeColor
import com.stripe.android.uicore.getComposeTextStyle
import com.stripe.android.utils.rememberActivity
import kotlinx.coroutines.delay

internal const val PrimaryButtonAnimationDuration = 800L

@Composable
internal fun PrimaryButtonForGooglePay(
    modifier: Modifier = Modifier,
    onPressed: () -> Unit,
) {
    PrimaryButton(
        label = stringResource(R.string.stripe_paymentsheet_primary_button_processing),
        isEnabled = false,
        modifier = modifier,
        isLoading = true,
        onButtonClick = onPressed,
        backgroundColor = colorResource(R.color.stripe_paymentsheet_googlepay_primary_button_background_color),
        defaultLabelColor = colorResource(R.color.stripe_paymentsheet_googlepay_primary_button_tint_color),
        lockIconDrawable = ImageVector.vectorResource(
            R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_lock
        ),
        confirmedIconDrawable = ImageVector.vectorResource(
            R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_checkmark
        ),
        indicatorColor = colorResource(R.color.stripe_paymentsheet_googlepay_primary_button_tint_color),
    )
}

@Composable
internal fun PrimaryButton(
    label: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onButtonClick: () -> Unit,
    backgroundColor: Color? = null,
    defaultLabelColor: Color? = null,
    lockIconDrawable: ImageVector? = null,
    confirmedIconDrawable: ImageVector? = null,
    indicatorColor: Color? = null,
) {
    val activity = rememberActivity {
        "Primary button must be created in the context of an Activity"
    }

    PrimaryButton(
        uiState = PrimaryButton.UIState(
            label = label,
            enabled = isEnabled,
            lockVisible = false,
            onClick = onButtonClick,
        ),
        state = if (isLoading) {
            PrimaryButton.State.StartProcessing
        } else {
            PrimaryButton.State.Ready
        },
        style = StripeTheme.primaryButtonStyle,
        background = backgroundColor ?: Color(StripeTheme.primaryButtonStyle.getBackgroundColor(activity.baseContext)),
        modifier = modifier,
        defaultLabelColor = defaultLabelColor,
        lockIconDrawable = lockIconDrawable,
        confirmedIconDrawable = confirmedIconDrawable,
        indicatorColor = indicatorColor,
    )
}

@Composable
internal fun PrimaryButton(
    uiState: PrimaryButton.UIState,
    state: PrimaryButton.State,
    style: PrimaryButtonStyle,
    background: Color,
    modifier: Modifier = Modifier,
    defaultLabelColor: Color? = null,
    lockIconDrawable: ImageVector? = null,
    confirmedIconDrawable: ImageVector? = null,
    indicatorColor: Color? = null,
) {
    val context = LocalContext.current

    val height = dimensionResource(R.dimen.stripe_paymentsheet_primary_button_height)
    val successColor = colorResource(R.color.stripe_paymentsheet_primary_button_success_background)

    val isButtonEnabled = remember(state, uiState) {
        when (state) {
            is PrimaryButton.State.Ready -> uiState.enabled
            is PrimaryButton.State.StartProcessing,
            is PrimaryButton.State.FinishProcessing -> false
        }
    }

    val targetAlpha = remember(state, isButtonEnabled) {
        when (state) {
            is PrimaryButton.State.Ready -> {
                if (isButtonEnabled) 1f else 0.5f
            }
            is PrimaryButton.State.StartProcessing,
            is PrimaryButton.State.FinishProcessing -> {
                1f
            }
        }
    }

    val stripeColors = if (isSystemInDarkTheme()) {
        style.colorsDark
    } else {
        style.colorsLight
    }

    if (state is PrimaryButton.State.FinishProcessing) {
        LaunchedEffect(state) {
            delay(PrimaryButtonAnimationDuration)
            state.onComplete()
        }
    }

    val targetBackground = remember(state) {
        if (state is PrimaryButton.State.FinishProcessing) {
            successColor
        } else {
            background
        }
    }

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        label = "PrimaryButtonAlpha",
    )

    val animatedBackground by animateColorAsState(
        targetValue = targetBackground,
        label = "PrimaryButtonBackground",
    )

    val label = remember(uiState, state) {
        if (state is PrimaryButton.State.StartProcessing) {
            context.getString(R.string.stripe_paymentsheet_primary_button_processing)
        } else {
            uiState.label
        }
    }

    val constraintSet = ConstraintSet {
        val buttonRef = createRefFor("button")
        val iconRef = createRefFor("icon")

        constrain(buttonRef) {
            centerTo(parent)
        }

        constrain(iconRef) {
            centerVerticallyTo(parent)

            if (state is PrimaryButton.State.FinishProcessing) {
                centerHorizontallyTo(parent)
            } else {
                end.linkTo(parent.end, margin = 16.dp)
            }
        }
    }

    ConstraintLayout(
        constraintSet = constraintSet,
        animateChanges = true,
        modifier = modifier
            .requiredHeight(height)
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(style.shape.cornerRadius.dp))
            .background(color = animatedBackground)
            .border(
                width = style.shape.borderStrokeWidth.dp,
                color = Color(color = style.getBorderStrokeColor(context)),
                shape = RoundedCornerShape(style.shape.cornerRadius.dp),
            )
            .alpha(alpha)
            .clickable(enabled = isButtonEnabled) { uiState.onClick() },
    ) {
        val iconSize = dimensionResource(R.dimen.stripe_paymentsheet_primary_button_icon_size)

        val checkmarkIcon = confirmedIconDrawable
            ?: ImageVector.vectorResource(R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_checkmark)

        if (state !is PrimaryButton.State.FinishProcessing) {
            LabelUI(
                label = label,
                color = defaultLabelColor,
                modifier = Modifier.layoutId("button"),
            )
        }

        Box(
            modifier = Modifier.layoutId("icon"),
        ) {
            when (state) {
                is PrimaryButton.State.Ready -> {
                    if (uiState.lockVisible) {
                        Icon(
                            imageVector = lockIconDrawable ?: Icons.Default.Lock,
                            contentDescription = null,
                            tint = stripeColors.onBackground,
                            modifier = Modifier.requiredSize(iconSize),
                        )
                    }
                }
                is PrimaryButton.State.StartProcessing -> {
                    CircularProgressIndicator(
                        color = indicatorColor ?: stripeColors.onBackground,
                        strokeWidth = 2.dp,
                        modifier = Modifier.requiredSize(iconSize),
                    )
                }
                is PrimaryButton.State.FinishProcessing -> {
                    Icon(
                        imageVector = checkmarkIcon,
                        contentDescription = null,
                        tint = stripeColors.onBackground,
                        modifier = Modifier.requiredSize(iconSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelUI(
    label: String,
    color: Color?,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        textAlign = TextAlign.Center,
        color = color ?: Color.Unspecified,
        style = StripeTheme.primaryButtonStyle.getComposeTextStyle(),
        modifier = modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 5.dp),
    )
}
