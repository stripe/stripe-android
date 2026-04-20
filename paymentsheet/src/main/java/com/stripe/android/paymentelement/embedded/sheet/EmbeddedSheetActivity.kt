package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
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
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.manage.EmbeddedNavigator
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut
import javax.inject.Inject

internal class EmbeddedSheetActivity : AppCompatActivity(), EmbeddedNavigator.ScreenResultHandler {
    private val args: EmbeddedSheetContract.Args? by lazy {
        EmbeddedSheetContract.Args.fromIntent(intent)
    }

    private val viewModel: EmbeddedSheetViewModel by viewModels {
        EmbeddedSheetViewModel.Factory {
            requireNotNull(args)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = args
        if (args == null) {
            setSheetResult(EmbeddedSheetResult.Error)
            finish()
            return
        }

        renderEdgeToEdge()
        viewModel.component.inject(this)
        bindNavigator()
        setupBackPressHandler()

        setContent {
            StripeTheme {
                SheetContent(args.mode)
            }
        }
    }

    private fun bindNavigator() {
        embeddedNavigator.bindToActivity(
            EmbeddedNavigator.ScreenActivityContext(
                activityResultCaller = this,
                lifecycleOwner = this,
                coroutineScope = lifecycleScope,
                resultHandler = this,
            )
        )
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback {
            if (!embeddedNavigator.screen.value.isPerformingNetworkOperation()) {
                embeddedNavigator.performAction(EmbeddedNavigator.Action.Back)
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun SheetContent(mode: EmbeddedSheetContract.Mode) {
        val screen by embeddedNavigator.screen.collectAsState()
        val bottomSheetState = rememberStripeBottomSheetState(
            confirmValueChange = { !screen.isPerformingNetworkOperation() }
        )
        ElementsBottomSheetLayout(
            state = bottomSheetState,
            onDismissed = { handleDismiss(mode) }
        ) {
            var hasResult by remember { mutableStateOf(false) }
            if (!hasResult) {
                when (screen) {
                    is EmbeddedNavigator.Screen.Form -> screen.Content()
                    else -> {
                        Box(modifier = Modifier.padding(bottom = 20.dp)) {
                            ManageScreenContent(embeddedNavigator, screen)
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    embeddedNavigator.result.collect { result ->
                        hasResult = true
                        when (mode) {
                            EmbeddedSheetContract.Mode.Form -> {
                                setCancelAndFinish()
                            }
                            EmbeddedSheetContract.Mode.Manage -> {
                                setManageResult(shouldInvokeSelectionCallback = result == true)
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleDismiss(mode: EmbeddedSheetContract.Mode) {
        when (mode) {
            EmbeddedSheetContract.Mode.Form -> setCancelAndFinish()
            EmbeddedSheetContract.Mode.Manage -> {
                setManageResult(shouldInvokeSelectionCallback = false)
                finish()
            }
        }
    }

    @Composable
    private fun ManageScreenContent(
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
                PaymentSheetTopBar(
                    state = topBarState,
                    canNavigateBack = navigator.canGoBack,
                    isEnabled = true,
                    handleBackPressed = { embeddedNavigator.performAction(EmbeddedNavigator.Action.Back) },
                )
            },
            content = {
                val horizontalPadding = StripeTheme.getOuterFormInsets()
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

                Box(modifier = Modifier.animateContentSize()) {
                    screen.Content()
                }
            },
            modifier = Modifier.onGloballyPositioned {
                contentHeight = with(density) { it.size.height.toDp() }
            },
            scrollState = scrollState,
        )
    }

    override fun onProcessingCompleted() = setCompletedResultAndDismiss()

    override fun onDismissed() = setCancelAndFinish()

    override fun onFormResult(result: FormResult) {
        setFormResult(result)
        finish()
    }

    private fun setCompletedResultAndDismiss() {
        setSheetResult(
            EmbeddedSheetResult.Complete(
                selection = null,
                hasBeenConfirmed = true,
                customerState = getCustomerState(),
                shouldInvokeSelectionCallback = false,
            )
        )
        finish()
    }

    private fun setCancelAndFinish() {
        setSheetResult(
            EmbeddedSheetResult.Cancelled(
                customerState = getCustomerState(),
            )
        )
        finish()
    }

    private fun setFormResult(result: FormResult) {
        val sheetResult = when (result) {
            is FormResult.Complete -> EmbeddedSheetResult.Complete(
                selection = result.selection,
                hasBeenConfirmed = result.hasBeenConfirmed,
                customerState = result.customerState,
                shouldInvokeSelectionCallback = false,
            )
            is FormResult.Cancelled -> EmbeddedSheetResult.Cancelled(
                customerState = result.customerState,
            )
        }
        setSheetResult(sheetResult)
    }

    private fun setManageResult(shouldInvokeSelectionCallback: Boolean) {
        setSheetResult(
            EmbeddedSheetResult.Complete(
                selection = selectionHolder.selection.value,
                hasBeenConfirmed = false,
                customerState = customerStateHolder.customer.value,
                shouldInvokeSelectionCallback = shouldInvokeSelectionCallback,
            )
        )
    }

    private fun getCustomerState(): CustomerState? {
        return if (::customerStateHolder.isInitialized) {
            customerStateHolder.customer.value
        } else {
            null
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            CheckoutInstances.markIntegrationDismissed(args?.paymentMethodMetadata)
            if (::eventReporter.isInitialized) {
                eventReporter.onDismiss()
            }
        }
    }

    private fun setSheetResult(result: EmbeddedSheetResult) {
        setResult(
            Activity.RESULT_OK,
            EmbeddedSheetResult.toIntent(intent, result)
        )
    }
}
