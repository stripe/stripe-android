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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalTextInputService
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
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.LinkButton
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
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.CircularProgressIndicator
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.LocalAutofillEventReporter
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
        AnimatedVisibility(visible = contentVisible) {
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
    contentVisible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val processing by viewModel.processing.collectAsState()

    val walletsProcessingState by viewModel.walletsProcessingState.collectAsState()

    val density = LocalDensity.current
    var contentHeight by remember { mutableStateOf(0.dp) }

    DismissKeyboardOnProcessing(processing)

    PaymentSheetScaffold(
        topBar = {
            val currentScreen by viewModel.navigationHandler.currentScreen.collectAsState()
            val topBarState by remember(currentScreen) {
                currentScreen.topBarState()
            }.collectAsState()

            PaymentSheetTopBar(
                state = topBarState,
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
    scrollState: ScrollState,
) {
    val walletsState by viewModel.walletsState.collectAsState()
    val walletsProcessingState by viewModel.walletsProcessingState.collectAsState()
    val error by viewModel.error.collectAsState()
    val mandateText by viewModel.mandateHandler.mandateText.collectAsState()
    val currentScreen by viewModel.navigationHandler.currentScreen.collectAsState()

    ResetScroll(scrollState = scrollState, currentScreen = currentScreen)

    val showsWalletsHeader by remember(currentScreen, type) {
        currentScreen.showsWalletsHeader(isCompleteFlow = type == Complete)
    }.collectAsState()

    val actualWalletsState = walletsState.takeIf { showsWalletsHeader }

    val headerText by remember(currentScreen, type, actualWalletsState != null) {
        currentScreen.title(isCompleteFlow = type == Complete, isWalletEnabled = actualWalletsState != null)
    }.collectAsState()

    Column(modifier) {
        PaymentSheetContent(
            viewModel = viewModel,
            headerText = headerText,
            walletsState = actualWalletsState,
            walletsProcessingState = walletsProcessingState,
            error = error,
            currentScreen = currentScreen,
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
    headerText: ResolvableString?,
    walletsState: WalletsState?,
    walletsProcessingState: WalletsProcessingState?,
    error: ResolvableString?,
    currentScreen: PaymentSheetScreen,
    mandateText: MandateText?,
) {
    @Composable
    fun Content(modifier: Modifier) {
        PaymentSheetContent(
            viewModel = viewModel,
            headerText = headerText,
            walletsState = walletsState,
            walletsProcessingState = walletsProcessingState,
            error = error,
            currentScreen = currentScreen,
            mandateText = mandateText,
            modifier = modifier
        )
    }

    when (currentScreen.animationStyle) {
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
private fun PaymentSheetContent(
    viewModel: BaseSheetViewModel,
    headerText: ResolvableString?,
    walletsState: WalletsState?,
    walletsProcessingState: WalletsProcessingState?,
    error: ResolvableString?,
    currentScreen: PaymentSheetScreen,
    mandateText: MandateText?,
    modifier: Modifier,
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(modifier = modifier) {
        headerText?.let { text ->
            H4Text(
                text = text.resolve(),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        walletsState?.let { state ->
            val bottomSpacing = currentScreen.walletsDividerSpacing - currentScreen.topContentPadding
            Wallet(
                state = state,
                processingState = walletsProcessingState,
                onGooglePayPressed = state.onGooglePayPressed,
                onLinkPressed = state.onLinkPressed,
                dividerSpacing = currentScreen.walletsDividerSpacing,
                modifier = Modifier.padding(bottom = bottomSpacing),
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            EventReporterProvider(viewModel) {
                currentScreen.Content(
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        if (mandateText?.showAbovePrimaryButton == true && currentScreen.showsMandates) {
            Mandate(
                mandateText = mandateText.text?.resolve(),
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .padding(bottom = 8.dp)
                    .testTag(PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG),
            )
        }

        Spacer(modifier = Modifier.height(currentScreen.bottomContentPadding))

        error?.let {
            ErrorMessage(
                error = it.resolve(),
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = horizontalPadding)
                    .testTag(PAYMENT_SHEET_ERROR_TEXT_TEST_TAG),
            )
        }
    }

    PrimaryButton(viewModel)

    Box(modifier = modifier) {
        if (mandateText?.showAbovePrimaryButton == false && currentScreen.showsMandates) {
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

        Spacer(modifier = Modifier.requiredHeight(dividerSpacing))

        val text = stringResource(state.dividerTextResource)
        WalletsDivider(text)
    }
}

@Composable
private fun EventReporterProvider(
    viewModel: BaseSheetViewModel,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAutofillEventReporter provides viewModel.eventReporter::onAutofill,
        LocalCardNumberCompletedEventReporter provides viewModel.eventReporter::onCardNumberCompleted,
    ) {
        content()
    }
}

@Composable
private fun PrimaryButton(viewModel: BaseSheetViewModel) {
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
        modifier = modifier,
    )

    LaunchedEffect(viewModel, button) {
        viewModel.primaryButtonUiState.collect { uiState ->
            button?.updateUiState(uiState)
        }
    }

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
internal const val PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG = "PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG"
private const val POST_SUCCESS_ANIMATION_DELAY = 1500L
