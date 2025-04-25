package com.stripe.android.paymentsheet.ui

import android.graphics.Typeface
import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeTypography
import com.stripe.android.ui.core.R as StripeUiCoreR

internal const val SHEET_NAVIGATION_BUTTON_TAG = "SHEET_NAVIGATION_BUTTON_TAG"

@Composable
internal fun PaymentSheetTopBar(
    state: PaymentSheetTopBarState?,
    canNavigateBack: Boolean,
    isEnabled: Boolean,
    handleBackPressed: () -> Unit,
    elevation: Dp = 0.dp,
) {
    if (state != null) {
        PaymentSheetTopBar(
            state = state,
            canNavigateBack = canNavigateBack,
            isEnabled = isEnabled,
            elevation = elevation,
            onNavigationIconPressed = handleBackPressed,
        )
    }
}

@Composable
internal fun PaymentSheetTopBar(
    state: PaymentSheetTopBarState,
    canNavigateBack: Boolean,
    isEnabled: Boolean,
    elevation: Dp,
    onNavigationIconPressed: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val tintColor = MaterialTheme.stripeColors.appBarIcon

    TopAppBar(
        title = {
            if (state.showTestModeLabel) {
                TestModeBadge()
            }
        },
        navigationIcon = {
            IconButton(
                enabled = isEnabled,
                onClick = {
                    keyboardController?.hide()
                    onNavigationIconPressed()
                },
                modifier = Modifier.testTag(SHEET_NAVIGATION_BUTTON_TAG)
            ) {
                val icon = if (canNavigateBack) {
                    R.drawable.stripe_ic_paymentsheet_back
                } else {
                    R.drawable.stripe_ic_paymentsheet_close
                }

                val contentDescription = if (canNavigateBack) {
                    StripeUiCoreR.string.stripe_back
                } else {
                    R.string.stripe_paymentsheet_close
                }
                Icon(
                    painter = painterResource(icon),
                    contentDescription = stringResource(contentDescription),
                    tint = tintColor,
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = elevation,
        actions = {
            if (state.showEditMenu) {
                EditButton(
                    labelResourceId = state.editMenuLabel,
                    isEnabled = isEnabled,
                    tintColor = tintColor,
                    onClick = state.onEditIconPressed,
                )
            }
        },
    )
}

@Composable
private fun EditButton(
    labelResourceId: Int,
    isEnabled: Boolean,
    tintColor: Color,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val typography = MaterialTheme.stripeTypography

    val editButtonTypeface = remember(typography) {
        typography.fontFamily?.let {
            ResourcesCompat.getFont(context, it)
        } ?: Typeface.DEFAULT
    }

    val editButtonFontSize = remember(typography) {
        with(density) {
            val sizeInPx = StripeThemeDefaults.typography.smallFontSize.value
            (sizeInPx.dp * typography.fontSizeMultiplier).toSp()
        }
    }

    IconButton(
        modifier = Modifier.testTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG).padding(end = 10.dp),
        enabled = isEnabled,
        onClick = onClick,
    ) {
        val text = stringResource(labelResourceId)
        Text(
            text = text.uppercase(),
            color = tintColor,
            fontSize = editButtonFontSize,
            fontFamily = FontFamily(editButtonTypeface),
        )
    }
}

@Composable
internal fun TestModeBadge() {
    val badgeColor = colorResource(R.color.stripe_paymentsheet_testmode_background)
    val textColor = colorResource(R.color.stripe_paymentsheet_testmode_text)

    Box(
        modifier = Modifier
            .background(badgeColor, shape = RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "TEST MODE",
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

@Preview
@Composable
internal fun PaymentSheetTopBar_Preview() {
    StripeTheme(colors = StripeThemeDefaults.colorsLight.copy(appBarIcon = Color.Red)) {
        val state = PaymentSheetTopBarState(
            showTestModeLabel = true,
            showEditMenu = true,
            isEditing = false,
            onEditIconPressed = {},
        )

        PaymentSheetTopBar(
            state = state,
            canNavigateBack = true,
            isEnabled = true,
            elevation = 0.dp,
            onNavigationIconPressed = {},
        )
    }
}

@Preview
@Composable
internal fun TestModeBadge_Preview() {
    StripeTheme {
        TestModeBadge()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG = "PaymentSheetEditButton"
