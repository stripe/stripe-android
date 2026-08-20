package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
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
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.utils.applicationIsTaskOwner
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeFormInsets
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class EmbeddedSheetActivity : AppCompatActivity() {
    private val hostArgs: SheetActivityArgs? by lazy {
        SheetActivityArgs.fromIntent(intent)
    }

    private lateinit var embeddedArgs: EmbeddedActivityArgs
    private var shouldReportDismiss = false

    private val embeddedSheetViewModel: EmbeddedSheetViewModel by viewModels {
        EmbeddedSheetViewModel.Factory { requireNotNull(hostArgs) }
    }

    @Inject
    lateinit var eventReporter: EventReporter

    @Inject
    lateinit var customerStateHolder: CustomerStateHolder

    @Inject
    lateinit var embeddedNavigator: EmbeddedNavigator

    @Inject
    lateinit var selectionHolder: EmbeddedSelectionHolder

    @Inject
    lateinit var sheetActivityRegistrar: SheetActivityRegistrar

    @Inject
    lateinit var sheetActivityStateHolder: SheetActivityStateHolder

    @Inject
    lateinit var linkAccountHolder: LinkAccountHolder

    @Inject
    lateinit var resultHandler: SheetActivityResultHandler

    @Inject
    lateinit var walletsHeader: SheetWalletsHeader

    @Inject
    lateinit var paymentSheetLinkEagerLauncher: PaymentSheetLinkEagerLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = hostArgs
        if (args == null) {
            finish()
            return
        }

        if (args is SheetActivityArgs.PaymentSheet) {
            try {
                args.args.initializationMode.validate()
                args.args.config.asCommonConfiguration().validate(
                    initializationMode = args.args.initializationMode,
                    isLiveMode = PaymentConfiguration.getInstance(this).isLiveMode(),
                    callbackIdentifier = args.args.paymentElementCallbackIdentifier,
                )
            } catch (e: IllegalArgumentException) {
                setPaymentSheetResult(PaymentSheetResult.Failed(e))
                finish()
                return
            }
        }

        renderEdgeToEdge()
        lifecycleScope.launch {
            embeddedSheetViewModel.state.collect(::handleActivityState)
        }
    }

    private fun handleActivityState(state: EmbeddedSheetViewModel.State) {
        when (state) {
            is EmbeddedSheetViewModel.State.Loading -> Unit
            is EmbeddedSheetViewModel.State.Failed -> {
                setPaymentSheetResult(PaymentSheetResult.Failed(state.error))
                finish()
            }
            is EmbeddedSheetViewModel.State.Ready -> createEmbeddedSheet(state)
        }
    }

    private fun createEmbeddedSheet(state: EmbeddedSheetViewModel.State.Ready) {
        if (::embeddedArgs.isInitialized) return

        val args = state.args
        embeddedArgs = args
        state.component.inject(this)

        if (!applicationIsTaskOwner()) {
            eventReporter.onCannotProperlyReturnFromLinkAndOtherLPMs()
        }

        sheetActivityRegistrar.registerAndBootstrap(
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "EmbeddedSheetActivity",
                registryOwner = this,
            ),
            lifecycleOwner = this,
        )

        lifecycleScope.launch {
            sheetActivityStateHolder.result.collect {
                shouldReportDismiss = resultHandler.shouldReportDismiss
                setActivityResult(it)
                finish()
            }
        }

        onBackPressedDispatcher.addCallback {
            if (!embeddedNavigator.screen.value.isPerformingNetworkOperation().value) {
                embeddedNavigator.performAction(EmbeddedNavigator.Action.Back)
            }
        }

        setContent {
            PaymentElementTheme(appearance = args.configuration.appearance) {
                EventReporterProvider(eventReporter) {
                    SheetContent()
                }
            }
        }

        paymentSheetLinkEagerLauncher.launchIfNeeded()
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun SheetContent() {
        val screen by embeddedNavigator.screen.collectAsState()
        val bottomSheetState = rememberStripeBottomSheetState(
            confirmValueChange = { !screen.isPerformingNetworkOperation().value }
        )
        ElementsBottomSheetLayout(
            state = bottomSheetState,
            onDismissed = ::dismissAndFinish,
        ) {
            var hasResult by remember { mutableStateOf(false) }
            if (!hasResult) {
                Box(modifier = Modifier.padding(bottom = 20.dp)) {
                    ScreenContent(embeddedNavigator, screen)
                }
                LaunchedEffect(Unit) {
                    embeddedNavigator.result.collect { result ->
                        hasResult = true
                        shouldReportDismiss = true
                        when (embeddedArgs.launchMode) {
                            is EmbeddedLaunchMode.Form -> dismissAndFinish()
                            is EmbeddedLaunchMode.PaymentOptions,
                            is EmbeddedLaunchMode.Complete -> {
                                setCancelledPaymentOptionsResult()
                                finish()
                            }
                            is EmbeddedLaunchMode.Manage -> {
                                setManageResult(shouldInvokeSelectionCallback = result == true)
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ScreenContent(
        navigator: EmbeddedNavigator,
        screen: EmbeddedNavigator.Screen
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
                    handleBackPressed = { embeddedNavigator.performAction(EmbeddedNavigator.Action.Back) },
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

                walletsHeader(screen)

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

    override fun finish() {
        super.finish()
        fadeOut()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing && shouldReportDismiss) {
            if (::eventReporter.isInitialized) {
                eventReporter.onDismiss()
            }
        }
    }

    private fun dismissAndFinish() {
        shouldReportDismiss = true
        when (val launchMode = embeddedArgs.launchMode) {
            is EmbeddedLaunchMode.Form -> {
                setActivityResult(
                    EmbeddedActivityResult.Cancelled(
                        customerState = customerStateHolder.customer.value,
                        launchMode = launchMode,
                    )
                )
            }
            is EmbeddedLaunchMode.Manage -> {
                setManageResult(shouldInvokeSelectionCallback = false)
            }
            is EmbeddedLaunchMode.PaymentOptions,
            is EmbeddedLaunchMode.Complete -> {
                setCancelledPaymentOptionsResult()
            }
        }
        finish()
    }

    private fun setManageResult(
        shouldInvokeSelectionCallback: Boolean,
    ) {
        setActivityResult(
            EmbeddedActivityResult.Complete(
                selection = selectionHolder.selection.value,
                previousNewSelections = selectionHolder.previousNewSelections,
                hasBeenConfirmed = false,
                customerState = customerStateHolder.customer.value,
                checkoutSessionResponse = null,
                shouldInvokeSelectionCallback = shouldInvokeSelectionCallback,
                launchMode = embeddedArgs.launchMode,
            )
        )
    }

    private fun setCancelledPaymentOptionsResult() {
        setActivityResult(
            EmbeddedActivityResult.Cancelled(
                customerState = customerStateHolder.customer.value,
                launchMode = embeddedArgs.launchMode,
            )
        )
    }

    private fun setActivityResult(result: EmbeddedActivityResult) {
        val activityResult = resultHandler.createResult(result, intent)
        setResult(activityResult.resultCode, activityResult.data)
    }

    private fun setPaymentSheetResult(result: PaymentSheetResult) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(PaymentSheetContract.Result(result).toBundle()),
        )
    }
}
