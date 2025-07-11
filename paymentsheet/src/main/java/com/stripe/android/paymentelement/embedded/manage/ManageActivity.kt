package com.stripe.android.paymentelement.embedded.manage

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
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
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
import kotlin.getValue

internal class ManageActivity : AppCompatActivity() {
    private val args: ManageContract.Args? by lazy {
        ManageContract.Args.fromIntent(intent)
    }

    private val viewModel: ManageViewModel by viewModels {
        ManageViewModel.Factory {
            requireNotNull(args)
        }
    }

    @Inject
    lateinit var customerStateHolder: CustomerStateHolder

    @Inject
    lateinit var manageNavigator: ManageNavigator

    @Inject
    lateinit var selectionHolder: EmbeddedSelectionHolder

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (args == null) {
            finish()
            return
        }

        renderEdgeToEdge()

        viewModel.component.inject(this)

        onBackPressedDispatcher.addCallback {
            if (!manageNavigator.screen.value.isPerformingNetworkOperation()) {
                manageNavigator.performAction(ManageNavigator.Action.Back)
            }
        }

        setContent {
            StripeTheme {
                val screen by manageNavigator.screen.collectAsState()
                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { !screen.isPerformingNetworkOperation() }
                )
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = {
                        setManageResult(false)
                        finish()
                    }
                ) {
                    var hasResult by remember { mutableStateOf(false) }
                    if (!hasResult) {
                        Box(modifier = Modifier.padding(bottom = 20.dp)) {
                            ScreenContent(manageNavigator, screen)
                        }
                        LaunchedEffect(screen) {
                            manageNavigator.result.collect { result ->
                                setManageResult(result == true)
                                finish()
                                hasResult = true
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ScreenContent(
        navigator: ManageNavigator,
        screen: ManageNavigator.Screen
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
                    handleBackPressed = { manageNavigator.performAction(ManageNavigator.Action.Back) },
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

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun setManageResult(shouldInvokeSelectionCallback: Boolean) {
        val result = ManageResult.Complete(
            customerState = requireNotNull(customerStateHolder.customer.value),
            selection = selectionHolder.selection.value,
            shouldInvokeSelectionCallback = shouldInvokeSelectionCallback
        )
        setResult(
            RESULT_OK,
            ManageResult.toIntent(intent, result)
        )
    }
}
