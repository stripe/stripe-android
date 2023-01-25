package com.stripe.android.paymentsheet.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.stripeColors

@Composable
internal fun PaymentSheetTopBar(
    args: PaymentSheetContract.Args,
    elevation: Dp = 0.dp,
) {
    val viewModel = viewModel<PaymentSheetViewModel>(
        factory = PaymentSheetViewModel.Factory { args }
    )
    PaymentSheetTopBar(
        viewModel = viewModel,
        elevation = elevation,
    )
}

@Composable
internal fun PaymentSheetTopBar(
    args: PaymentOptionContract.Args,
    elevation: Dp = 0.dp,
) {
    val viewModel = viewModel<PaymentOptionsViewModel>(
        factory = PaymentOptionsViewModel.Factory { args }
    )
    PaymentSheetTopBar(
        viewModel = viewModel,
        elevation = elevation,
    )
}

@Composable
internal fun PaymentSheetTopBar(
    viewModel: BaseSheetViewModel,
    elevation: Dp,
) {
    val screen by viewModel.currentScreen.collectAsState()
    val stripeIntent by viewModel.stripeIntent.collectAsState()
    val isProcessing by viewModel.processing.collectAsState()
    val isEditing by viewModel.editing.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()

    val state = rememberPaymentSheetTopBarState(
        screen = screen,
        paymentMethods = paymentMethods,
        isLiveMode = stripeIntent?.isLiveMode ?: true,
        isProcessing = isProcessing,
        isEditing = isEditing,
    )

    PaymentSheetTopBar(
        state = state,
        elevation = elevation,
        onNavigationIconPressed = viewModel::handleBackPressed,
        onEditIconPressed = viewModel::toggleEditing,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PaymentSheetTopBar(
    state: PaymentSheetTopBarState,
    elevation: Dp,
    onNavigationIconPressed: () -> Unit,
    onEditIconPressed: () -> Unit,
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
                enabled = state.isEnabled,
                onClick = {
                    keyboardController?.hide()
                    onNavigationIconPressed()
                },
            ) {
                Icon(
                    painter = painterResource(state.icon),
                    contentDescription = stringResource(state.contentDescription),
                    tint = tintColor,
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = elevation,
        actions = {
            if (state.showEditMenu) {
                IconButton(onClick = onEditIconPressed) {
                    val text = stringResource(state.editMenuLabel)
                    Text(
                        text = text.uppercase(),
                        color = tintColor,
                    )
                }
            }
        },
    )
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
            icon = R.drawable.stripe_ic_paymentsheet_back_enabled,
            contentDescription = R.string.back,
            showTestModeLabel = true,
            showEditMenu = true,
            editMenuLabel = R.string.edit,
            isEnabled = true,
        )

        PaymentSheetTopBar(
            state = state,
            elevation = 0.dp,
            onNavigationIconPressed = {},
            onEditIconPressed = {},
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
