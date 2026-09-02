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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.utils.EventReporterProvider
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
    private var args: EmbeddedActivityArgs? = null
    private var isReadyInitialized = false

    private val viewModel: EmbeddedSheetViewModel by viewModels {
        EmbeddedSheetViewModel.Factory {
            requireNotNull(args).also {
                require(it.launchMode !is EmbeddedLaunchMode.Loading)
            }
        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityArgs = EmbeddedActivityArgs.fromIntent(intent) ?: run {
            finish()
            return
        }
        args = activityArgs

        renderEdgeToEdge()

        onBackPressedDispatcher.addCallback {
            if (!isReadyInitialized) {
                dismissAndFinish()
            } else if (!embeddedNavigator.screen.value.isPerformingNetworkOperation().value) {
                embeddedNavigator.performAction(EmbeddedNavigator.Action.Back)
            }
        }

        if (activityArgs.launchMode is EmbeddedLaunchMode.Loading) {
            showLoading(activityArgs)
        } else {
            initializeReady(activityArgs)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNewIntent(intent)
    }

    internal fun handleNewIntent(intent: Intent) {
        if (
            isFinishing ||
            isReadyInitialized ||
            args?.launchMode !is EmbeddedLaunchMode.Loading
        ) {
            return
        }

        val readyArgs = EmbeddedActivityArgs.fromIntent(intent) ?: return
        if (readyArgs.launchMode !is EmbeddedLaunchMode.PaymentOptions) return

        setIntent(intent)
        args = readyArgs
        initializeReady(readyArgs)
    }

    private fun initializeReady(activityArgs: EmbeddedActivityArgs) {
        if (isReadyInitialized || isFinishing) return
        isReadyInitialized = true

        viewModel.component.inject(this)
        sheetActivityRegistrar.registerAndBootstrap(
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "EmbeddedSheetActivity_${activityArgs.paymentElementCallbackIdentifier}",
                registryOwner = this,
            ),
            lifecycleOwner = this,
        )
        lifecycleScope.launch {
            sheetActivityStateHolder.result.collect {
                setActivityResult(it)
                finish()
            }
        }

        setContent {
            PaymentElementTheme(appearance = activityArgs.configuration.appearance) {
                EventReporterProvider(eventReporter) {
                    SheetContent()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    private fun showLoading(activityArgs: EmbeddedActivityArgs) {
        setContent {
            PaymentElementTheme(appearance = activityArgs.configuration.appearance) {
                val bottomSheetState = rememberStripeBottomSheetState()
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = ::dismissAndFinish,
                ) {
                    BottomSheetLoadingIndicator(
                        modifier = Modifier.testTag(EMBEDDED_SHEET_LOADING_TEST_TAG)
                    )
                }
            }
        }
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
                    EmbeddedSheetScreenContent(embeddedNavigator, screen)
                }
                LaunchedEffect(Unit) {
                    embeddedNavigator.result.collect { result ->
                        hasResult = true
                        when (args?.launchMode) {
                            is EmbeddedLaunchMode.Loading -> {
                                error("Loading does not initialize the embedded sheet component.")
                            }
                            is EmbeddedLaunchMode.Form -> dismissAndFinish()
                            is EmbeddedLaunchMode.PaymentOptions -> {
                                setCancelledPaymentOptionsResult()
                                finish()
                            }
                            is EmbeddedLaunchMode.Manage, null -> {
                                setManageResult(shouldInvokeSelectionCallback = result == true)
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            if (::eventReporter.isInitialized) {
                eventReporter.onDismiss()
            }
        }
    }

    private fun dismissAndFinish() {
        when (val launchMode = args?.launchMode) {
            is EmbeddedLaunchMode.Loading -> {
                setActivityResult(
                    EmbeddedActivityResult.Cancelled(
                        customerState = null,
                        launchMode = launchMode,
                    )
                )
            }
            is EmbeddedLaunchMode.Form -> {
                setActivityResult(
                    EmbeddedActivityResult.Cancelled(
                        customerState = customerStateHolder.customer.value,
                        launchMode = launchMode,
                    )
                )
            }
            is EmbeddedLaunchMode.Manage, null -> {
                setManageResult(shouldInvokeSelectionCallback = false)
            }
            is EmbeddedLaunchMode.PaymentOptions -> {
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
                launchMode = args?.launchMode ?: EmbeddedLaunchMode.Manage,
            )
        )
    }

    private fun setCancelledPaymentOptionsResult() {
        setActivityResult(
            EmbeddedActivityResult.Cancelled(
                customerState = customerStateHolder.customer.value,
                launchMode = EmbeddedLaunchMode.PaymentOptions,
            )
        )
    }

    private fun setActivityResult(result: EmbeddedActivityResult) {
        setResult(
            Activity.RESULT_OK,
            EmbeddedActivityResult.toIntent(intent, result)
        )
    }
}

@Composable
internal fun EmbeddedSheetScreenContent(
    navigator: EmbeddedNavigator,
    screen: EmbeddedNavigator.Screen,
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

internal const val EMBEDDED_SHEET_LOADING_TEST_TAG = "embedded_sheet_loading"
