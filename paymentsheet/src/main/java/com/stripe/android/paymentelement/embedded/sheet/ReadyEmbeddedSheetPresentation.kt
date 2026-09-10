package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.addCallback
import androidx.activity.result.ActivityResultCaller
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeFormInsets
import com.stripe.android.uicore.utils.collectAsState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class ReadyEmbeddedSheetPresentation @AssistedInject constructor(
    @Assisted private val activity: EmbeddedSheetActivity,
    @Assisted private val args: EmbeddedActivityArgs,
    @Assisted private val activityResultCaller: ActivityResultCaller,
    private val eventReporter: EventReporter,
    private val customerStateHolder: CustomerStateHolder,
    private val embeddedNavigator: EmbeddedNavigator,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val sheetActivityRegistrar: SheetActivityRegistrar,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
    private val resultHandler: SheetActivityResultHandler,
    private val walletsHeader: SheetWalletsHeader,
    private val paymentSheetLinkEagerLauncher: PaymentSheetLinkEagerLauncher,
) : EmbeddedSheetPresentation {
    private var shouldReportDismiss = false

    override fun register() {
        sheetActivityRegistrar.registerAndBootstrap(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = activity,
        )

        activity.lifecycleScope.launch {
            sheetActivityStateHolder.result.collect { result ->
                shouldReportDismiss = resultHandler.shouldReportDismiss
                finishWithResult(result)
            }
        }

        activity.onBackPressedDispatcher.addCallback {
            if (!embeddedNavigator.screen.value.isPerformingNetworkOperation().value) {
                embeddedNavigator.performAction(EmbeddedNavigator.Action.Back)
            }
        }

        paymentSheetLinkEagerLauncher.launchIfNeeded()
    }

    override fun canDismiss(): Boolean {
        return !embeddedNavigator.screen.value.isPerformingNetworkOperation().value
    }

    override fun onDismissed() {
        shouldReportDismiss = true
        finishWithResult(createDismissalResult())
    }

    @Composable
    override fun Content() {
        EventReporterProvider(eventReporter) {
            EmbeddedSheetReadyContent(
                navigator = embeddedNavigator,
                onResult = { result ->
                    shouldReportDismiss = true
                    finishWithResult(createNavigatorResult(result))
                },
            )
        }
    }

    override fun onDestroy() {
        if (activity.isFinishing && shouldReportDismiss) {
            eventReporter.onDismiss()
        }
    }

    private fun finishWithResult(result: EmbeddedActivityResult) {
        val activityResult = resultHandler.createResult(result, activity.intent)
        activity.setResult(activityResult.resultCode, activityResult.data)
        activity.finish()
    }

    private fun createDismissalResult(): EmbeddedActivityResult {
        return when (val launchMode = args.launchMode) {
            is EmbeddedLaunchMode.Form -> EmbeddedActivityResult.Cancelled(
                customerState = customerStateHolder.customer.value,
                launchMode = launchMode,
            )
            is EmbeddedLaunchMode.Manage -> createManageResult(
                shouldInvokeSelectionCallback = false,
                launchMode = launchMode,
            )
            is EmbeddedLaunchMode.Complete,
            is EmbeddedLaunchMode.PaymentOptions -> createPaymentOptionsCancellationResult()
        }
    }

    private fun createNavigatorResult(result: Boolean?): EmbeddedActivityResult {
        return when (val launchMode = args.launchMode) {
            is EmbeddedLaunchMode.Form -> createDismissalResult()
            is EmbeddedLaunchMode.Manage -> createManageResult(
                shouldInvokeSelectionCallback = result == true,
                launchMode = launchMode,
            )
            is EmbeddedLaunchMode.Complete,
            is EmbeddedLaunchMode.PaymentOptions -> createPaymentOptionsCancellationResult()
        }
    }

    private fun createManageResult(
        shouldInvokeSelectionCallback: Boolean,
        launchMode: EmbeddedLaunchMode.Manage,
    ): EmbeddedActivityResult {
        return EmbeddedActivityResult.Complete(
            selection = selectionHolder.selection.value,
            previousNewSelections = selectionHolder.previousNewSelections,
            hasBeenConfirmed = false,
            customerState = customerStateHolder.customer.value,
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = shouldInvokeSelectionCallback,
            launchMode = launchMode,
        )
    }

    private fun createPaymentOptionsCancellationResult(): EmbeddedActivityResult {
        return EmbeddedActivityResult.Cancelled(
            customerState = customerStateHolder.customer.value,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
    }

    @Composable
    private fun EmbeddedSheetReadyContent(
        navigator: EmbeddedNavigator,
        onResult: (Boolean?) -> Unit,
    ) {
        val screen by navigator.screen.collectAsState()
        var hasResult by remember { mutableStateOf(false) }
        if (!hasResult) {
            Box(modifier = Modifier.padding(bottom = 20.dp)) {
                EmbeddedSheetScreenContent(
                    navigator = navigator,
                    screen = screen,
                    walletsHeader = { walletsHeader(screen) },
                )
            }
            LaunchedEffect(navigator) {
                navigator.result.collect { result ->
                    hasResult = true
                    onResult(result)
                }
            }
        }
    }

    @AssistedFactory
    interface Factory : EmbeddedSheetPresentation.Factory {
        override fun create(
            activity: EmbeddedSheetActivity,
            args: EmbeddedActivityArgs,
            activityResultCaller: ActivityResultCaller,
        ): ReadyEmbeddedSheetPresentation
    }
}

@Composable
internal fun EmbeddedSheetScreenContent(
    navigator: EmbeddedNavigator,
    screen: EmbeddedNavigator.Screen,
    walletsHeader: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var contentHeight by remember { mutableStateOf(0.dp) }
    val scrollState = rememberScrollState()
    BottomSheetScaffold(
        topBar = {
            val topBarState by remember(screen) {
                screen.topBarState()
            }.collectAsState()
            val isPerformingNetworkOperation by remember(screen) {
                screen.isPerformingNetworkOperation()
            }.collectAsState()
            PaymentSheetTopBar(
                state = topBarState,
                canNavigateBack = navigator.canGoBack,
                isEnabled = !isPerformingNetworkOperation,
                handleBackPressed = { navigator.performAction(EmbeddedNavigator.Action.Back) },
            )
        },
        content = {
            val horizontalPadding = MaterialTheme.stripeFormInsets.getOuterFormInsets()
            val headerText by remember(screen) {
                screen.title()
            }.collectAsState()
            headerText?.let { text ->
                H4Text(
                    text = text.resolve(),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(horizontalPadding),
                )
            }

            walletsHeader()

            Column(modifier = Modifier.animateContentSize()) {
                screen.Content()
            }
        },
        modifier = Modifier.onGloballyPositioned {
            contentHeight = with(density) { it.size.height.toDp() }
        },
        scrollState = scrollState,
    )
}
