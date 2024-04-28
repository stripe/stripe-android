@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripeFragmentPaymentOptionsPrimaryButtonBinding
import com.stripe.android.paymentsheet.databinding.StripeFragmentPaymentSheetPrimaryButtonBinding
import com.stripe.android.paymentsheet.model.MandateText
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.topContentPadding
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType.Complete
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType.Custom
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.CircularProgressIndicator
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.strings.resolve
import kotlinx.coroutines.delay

@Composable
internal fun PaymentSheetScreen(
    viewModel: BaseSheetViewModel,
    type: PaymentSheetFlowType,
    modifier: Modifier = Modifier,
) {
    val contentVisible by viewModel.contentVisible.collectAsState()
    val processing by viewModel.processing.collectAsState()

    val topBarState by viewModel.topBarState.collectAsState()
    val walletsProcessingState by viewModel.walletsProcessingState.collectAsState()

    val density = LocalDensity.current
    var contentHeight by remember { mutableStateOf(0.dp) }

    DismissKeyboardOnProcessing(processing)

    PaymentSheetScaffold(
        topBar = {
            PaymentSheetTopBar(
                state = topBarState,
                handleBackPressed = viewModel::handleBackPressed,
                toggleEditing = viewModel::toggleEditing,
            )
        },
        content = {
            AnimatedVisibility(visible = contentVisible) {
                PaymentSheetScreenContent(viewModel, type)
            }
        },
        modifier = modifier.onGloballyPositioned {
            contentHeight = with(density) { it.size.height.toDp() }
        },
    )

    AnimatedVisibility(
        visible = walletsProcessingState != null &&
            walletsProcessingState !is WalletsProcessingState.Idle &&
            contentVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .requiredHeight(contentHeight)
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface.copy(alpha = 0.9f)),
        ) {
            ProgressOverlay(walletsProcessingState)
        }
    }
}

@Composable
private fun DismissKeyboardOnProcessing(processing: Boolean) {
    val keyboardController = LocalTextInputService.current

    if (processing) {
        LaunchedEffect(Unit) {
            @Suppress("DEPRECATION")
            keyboardController?.hideSoftwareKeyboard()
        }
    }
}

@Composable
internal fun PaymentSheetScreenContent(
    viewModel: BaseSheetViewModel,
    type: PaymentSheetFlowType,
    modifier: Modifier = Modifier,
) {
    val headerText by viewModel.headerText.collectAsState()
    val walletsState by viewModel.walletsState.collectAsState()
    val walletsProcessingState by viewModel.walletsProcessingState.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val mandateText by viewModel.mandateText.collectAsState()

    Column(modifier.animateContentSize()) {
        PaymentSheetContent(
            viewModel = viewModel,
            type = type,
            headerText = headerText,
            walletsState = walletsState,
            walletsProcessingState = walletsProcessingState,
            error = error,
            currentScreen = currentScreen,
            mandateText = mandateText,
        )

        PaymentSheetContentPadding()
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun BoxScope.ProgressOverlay(walletsProcessingState: WalletsProcessingState?) {
    AnimatedContent(
        targetState = walletsProcessingState,
        label = "AnimatedProcessingState"
    ) { processingState ->
        when (processingState) {
            is WalletsProcessingState.Processing -> CircularProgressIndicator(
                color = MaterialTheme.colors.onSurface,
                strokeWidth = dimensionResource(R.dimen.stripe_paymentsheet_loading_indicator_stroke_width),
                modifier = Modifier.requiredSize(48.dp),
            )
            is WalletsProcessingState.Completed -> {
                LaunchedEffect(processingState) {
                    delay(POST_SUCCESS_ANIMATION_DELAY)
                    processingState.onComplete()
                }

                Icon(
                    painter = painterResource(R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_checkmark),
                    tint = MaterialTheme.colors.onSurface,
                    contentDescription = null,
                    modifier = Modifier.requiredSize(48.dp)
                )
            }
            null,
            is WalletsProcessingState.Idle -> Unit
        }
    }
}

@Composable
private fun PaymentSheetContent(
    viewModel: BaseSheetViewModel,
    type: PaymentSheetFlowType,
    headerText: Int?,
    walletsState: WalletsState?,
    walletsProcessingState: WalletsProcessingState?,
    error: String?,
    currentScreen: PaymentSheetScreen,
    mandateText: MandateText?,
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    headerText?.let { text ->
        H4Text(
            text = stringResource(text),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .padding(horizontal = horizontalPadding),
        )
    }

    walletsState?.let { state ->
        val bottomSpacing = WalletDividerSpacing - currentScreen.topContentPadding
        Wallet(
            state = state,
            processingState = walletsProcessingState,
            onGooglePayPressed = state.onGooglePayPressed,
            onLinkPressed = state.onLinkPressed,
            modifier = Modifier.padding(bottom = bottomSpacing),
        )
    }

    Box {
        currentScreen.Content(
            viewModel = viewModel,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    if (mandateText?.showAbovePrimaryButton == true) {
        Mandate(
            mandateText = mandateText.text,
            modifier = Modifier.padding(horizontal = horizontalPadding),
        )
    }

    error?.let {
        ErrorMessage(
            error = it,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = horizontalPadding),
        )
    }

    PrimaryButton(viewModel, type)

    if (mandateText?.showAbovePrimaryButton == false) {
        Mandate(
            mandateText = mandateText.text,
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = horizontalPadding),
        )
    }
}

@Composable
internal fun Wallet(
    state: WalletsState,
    processingState: WalletsProcessingState?,
    onGooglePayPressed: () -> Unit,
    onLinkPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val padding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(modifier = modifier.padding(horizontal = padding)) {
        state.googlePay?.let { googlePay ->
            GooglePayButton(
                state = PrimaryButton.State.Ready,
                allowCreditCards = googlePay.allowCreditCards,
                buttonType = googlePay.buttonType,
                billingAddressParameters = googlePay.billingAddressParameters,
                isEnabled = state.buttonsEnabled,
                onPressed = onGooglePayPressed,
            )
        }

        state.link?.let {
            if (state.googlePay != null) {
                Spacer(modifier = Modifier.requiredHeight(8.dp))
            }

            LinkButton(
                email = it.email,
                enabled = state.buttonsEnabled,
                onClick = onLinkPressed,
            )
        }

        when (processingState) {
            is WalletsProcessingState.Idle -> processingState.error?.let { error ->
                ErrorMessage(
                    error = error.resolve(),
                    modifier = Modifier.padding(vertical = 3.dp, horizontal = 1.dp),
                )
            }
            else -> Unit
        }

        Spacer(modifier = Modifier.requiredHeight(WalletDividerSpacing))

        val text = stringResource(state.dividerTextResource)
        WalletsDivider(text)
    }
}

@Composable
private fun PrimaryButton(viewModel: BaseSheetViewModel, type: PaymentSheetFlowType) {
    val uiState = viewModel.primaryButtonUiState.collectAsState()

    val modifier = Modifier
        .testTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)
        .semantics {
            role = Role.Button

            val currentState = uiState.value

            if (currentState == null || !currentState.enabled) {
                disabled()
            }
        }

    when (type) {
        Complete -> {
            AndroidViewBinding(
                factory = StripeFragmentPaymentSheetPrimaryButtonBinding::inflate,
                modifier = modifier,
            )
        }
        Custom -> {
            AndroidViewBinding(
                factory = StripeFragmentPaymentOptionsPrimaryButtonBinding::inflate,
                modifier = modifier,
            )
        }
    }
}

const val PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG = "PRIMARY_BUTTON"
private const val POST_SUCCESS_ANIMATION_DELAY = 1500L
