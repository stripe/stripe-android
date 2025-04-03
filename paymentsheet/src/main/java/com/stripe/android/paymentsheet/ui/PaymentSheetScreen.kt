@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.stripe.android.CardBrandFilter
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripeFragmentPrimaryButtonContainerBinding
import com.stripe.android.paymentsheet.model.MandateText
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType.Complete
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType.Custom
import com.stripe.android.paymentsheet.utils.DismissKeyboardOnProcessing
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.CircularProgressIndicator
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBackgroundColor
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun PaymentSheetScreen(
    viewModel: PaymentSheetViewModel,
) {
    val contentVisible by viewModel.contentVisible.collectAsState()
    val scrollState = rememberScrollState()
    PaymentSheetScreen(viewModel, scrollState) {
        AnimatedVisibility(visible = contentVisible, modifier = Modifier.fillMaxWidth()) {
            PaymentSheetScreenContent(viewModel, type = Complete, scrollState = scrollState)
        }
    }
}

@Composable
internal fun PaymentSheetScreen(
    viewModel: PaymentOptionsViewModel,
) {
    val scrollState = rememberScrollState()
    PaymentSheetScreen(viewModel, scrollState) {
        PaymentSheetScreenContent(viewModel, type = Custom, scrollState = scrollState)
    }
}

@Composable
internal fun PaymentSheetScreen(
    viewModel: BaseSheetViewModel,
    type: PaymentSheetFlowType,
) {
    val scrollState = rememberScrollState()
    PaymentSheetScreen(viewModel, scrollState) {
        PaymentSheetScreenContent(viewModel, type = type, scrollState = scrollState)
    }
}

@Composable
private fun PaymentSheetScreen(
    viewModel: BaseSheetViewModel,
    scrollState: ScrollState,
    content: @Composable () -> Unit,
) {
    val processing by viewModel.processing.collectAsState()

    val walletsProcessingState by viewModel.walletsProcessingState.collectAsState()

    val density = LocalDensity.current
    var contentHeight by remember { mutableStateOf(0.dp) }

    DismissKeyboardOnProcessing(processing)

    BottomSheetScaffold(
        topBar = {
            val currentScreen by viewModel.navigationHandler.currentScreen.collectAsState()
            val topBarState by remember(currentScreen) {
                currentScreen.topBarState()
            }.collectAsState()

            PaymentSheetTopBar(
                state = topBarState,
                canNavigateBack = viewModel.navigationHandler.canGoBack,
                isEnabled = !processing,
                handleBackPressed = viewModel::handleBackPressed,
            )
        },
        content = content,
        modifier = Modifier.onGloballyPositioned {
            contentHeight = with(density) { it.size.height.toDp() }
        },
        scrollState = scrollState,
    )

    AnimatedVisibility(
        visible = walletsProcessingState != null &&
            walletsProcessingState !is WalletsProcessingState.Idle,
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

internal data class PaymentSheetScreenContentState(
    val showsWalletsHeader: Boolean = false,
    val actualWalletsState: WalletsState? = null,
    val headerText: ResolvableString? = null,
    val currentScreen: PaymentSheetScreen = PaymentSheetScreen.Loading,
    val primaryButtonUiState: PrimaryButton.UIState? = null,
)

@Composable
internal fun PaymentSheetScreenContent(
    viewModel: BaseSheetViewModel,
    type: PaymentSheetFlowType,
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
) {
    val walletsState by viewModel.walletsState.collectAsState()
    val walletsProcessingState by viewModel.walletsProcessingState.collectAsState()
    val error by viewModel.error.collectAsState()
    val mandateText by viewModel.mandateHandler.mandateText.collectAsState()
    val currentScreen by viewModel.navigationHandler.currentScreen.collectAsState()
    val primaryButtonUiState by viewModel.primaryButtonUiState.collectAsState()

    val uiState by currentScreen.paymentSheetScreenContentState(
        type = type,
        walletsState = walletsState,
        primaryButtonUiState = primaryButtonUiState
    ).collectAsState()

    ResetScroll(scrollState = scrollState, currentScreen = uiState.currentScreen)

    Column(modifier) {
        PaymentSheetContent(
            viewModel = viewModel,
            uiState = uiState,
            walletsProcessingState = walletsProcessingState,
            error = error,
            mandateText = mandateText,
        )

        PaymentSheetContentPadding()
    }
}

@Composable
private fun ResetScroll(scrollState: ScrollState, currentScreen: PaymentSheetScreen) {
    var lastScreenClassName by rememberSaveable {
        mutableStateOf("")
    }
    val currentScreenClassName = currentScreen.javaClass.name
    if (currentScreenClassName != lastScreenClassName) {
        lastScreenClassName = currentScreenClassName
        LaunchedEffect(currentScreen) {
            scrollState.scrollTo(0)
        }
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
    uiState: PaymentSheetScreenContentState,
    walletsProcessingState: WalletsProcessingState?,
    error: ResolvableString?,
    mandateText: MandateText?,
) {
    @Composable
    fun Content(modifier: Modifier) {
        PaymentSheetContent(
            viewModel = viewModel,
            uiState = uiState,
            walletsProcessingState = walletsProcessingState,
            error = error,
            mandateText = mandateText,
            modifier = modifier
        )
    }

    when (uiState.currentScreen.animationStyle) {
        PaymentSheetScreen.AnimationStyle.PrimaryButtonAnchored -> {
            Content(modifier = Modifier.animateContentSize())
        }
        PaymentSheetScreen.AnimationStyle.FullPage -> {
            Column(modifier = Modifier.animateContentSize()) {
                Content(modifier = Modifier)
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun PaymentSheetContent(
    viewModel: BaseSheetViewModel,
    uiState: PaymentSheetScreenContentState,
    walletsProcessingState: WalletsProcessingState?,
    error: ResolvableString?,
    mandateText: MandateText?,
    modifier: Modifier
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)
    Column(modifier = modifier.padding(bottom = uiState.currentScreen.bottomContentPadding)) {
        uiState.headerText?.let { text ->
            H4Text(
                text = text.resolve(),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        uiState.actualWalletsState?.let { state ->
            val bottomSpacing = uiState.currentScreen.walletsDividerSpacing - uiState.currentScreen.topContentPadding
            Wallet(
                state = state,
                processingState = walletsProcessingState,
                onGooglePayPressed = state.onGooglePayPressed,
                onLinkPressed = state.onLinkPressed,
                dividerSpacing = uiState.currentScreen.walletsDividerSpacing,
                modifier = Modifier.padding(bottom = bottomSpacing),
                cardBrandFilter = PaymentSheetCardBrandFilter(viewModel.config.cardBrandAcceptance)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            EventReporterProvider(viewModel.eventReporter) {
                uiState.currentScreen.Content(
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        if (mandateText?.showAbovePrimaryButton == true && uiState.currentScreen.showsMandates) {
            Mandate(
                mandateText = mandateText.text?.resolve(),
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .padding(bottom = 8.dp)
                    .testTag(PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG),
            )
        }

        error?.let {
            ErrorMessage(
                error = it.resolve(),
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .padding(top = 2.dp, bottom = 8.dp)
                    .testTag(PAYMENT_SHEET_ERROR_TEXT_TEST_TAG),
            )
        }
    }

    PrimaryButton(viewModel, uiState.primaryButtonUiState)

    Box(modifier = modifier) {
        if (mandateText?.showAbovePrimaryButton == false && uiState.currentScreen.showsMandates) {
            Mandate(
                mandateText = mandateText.text?.resolve(),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = horizontalPadding)
                    .testTag(PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG),
            )
        }
    }
}

@Composable
internal fun Wallet(
    state: WalletsState,
    processingState: WalletsProcessingState?,
    onGooglePayPressed: () -> Unit,
    onLinkPressed: () -> Unit,
    dividerSpacing: Dp,
    modifier: Modifier = Modifier,
    cardBrandFilter: CardBrandFilter
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
                cardBrandFilter = cardBrandFilter
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

        Spacer(modifier = Modifier.requiredHeight(dividerSpacing))

        val text = stringResource(state.dividerTextResource)
        WalletsDivider(text)
    }
}

@Composable
private fun PrimaryButton(viewModel: BaseSheetViewModel, currentState: PrimaryButton.UIState?) {
    val modifier = Modifier
        .testTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)
        .semantics {
            role = Role.Button

            if (currentState == null || !currentState.enabled) {
                disabled()
            }
        }

    var button by remember {
        mutableStateOf<PrimaryButton?>(null)
    }

    val context = LocalContext.current

    AndroidViewBinding(
        factory = { inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean ->
            val binding = StripeFragmentPrimaryButtonContainerBinding.inflate(inflater, parent, attachToParent)
            val primaryButton = binding.primaryButton
            button = primaryButton
            @Suppress("DEPRECATION")
            primaryButton.setAppearanceConfiguration(
                StripeTheme.primaryButtonStyle,
                tintList = viewModel.config.primaryButtonColor ?: ColorStateList.valueOf(
                    StripeTheme.primaryButtonStyle.getBackgroundColor(context)
                )
            )
            binding
        },
        update = {
            button?.updateUiState(currentState)
        },
        modifier = modifier,
    )

    LaunchedEffect(viewModel, button) {
        (viewModel as? PaymentSheetViewModel)?.buyButtonState?.collect { state ->
            withContext(Dispatchers.Main) {
                button?.updateState(state?.convert())
            }
        }
    }
}

internal fun PaymentSheetViewState.convert(): PrimaryButton.State {
    return when (this) {
        is PaymentSheetViewState.Reset -> {
            PrimaryButton.State.Ready
        }
        is PaymentSheetViewState.StartProcessing -> {
            PrimaryButton.State.StartProcessing
        }
        is PaymentSheetViewState.FinishProcessing -> {
            PrimaryButton.State.FinishProcessing(this.onComplete)
        }
    }
}

const val PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG = "PRIMARY_BUTTON"
const val PAYMENT_SHEET_ERROR_TEXT_TEST_TAG = "PAYMENT_SHEET_ERROR"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG = "PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG"
private const val POST_SUCCESS_ANIMATION_DELAY = 1500L
