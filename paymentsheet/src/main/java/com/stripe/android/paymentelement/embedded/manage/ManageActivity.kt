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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
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

        viewModel.component.inject(this)

        onBackPressedDispatcher.addCallback {
            manageNavigator.performAction(ManageNavigator.Action.Back)
        }

        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = {
                        setManageResult(ManageResult.Cancelled(customerStateHolder.customer.value))
                        finish()
                    }
                ) {
                    val screen by manageNavigator.screen.collectAsState()
                    Box(modifier = Modifier.padding(bottom = 20.dp)) {
                        ScreenContent(manageNavigator, screen)
                    }
                    LaunchedEffect(screen) {
                        manageNavigator.result.collect { result ->
                            if (result.maintainPaymentSelection) {
                                setManageResult(
                                    ManageResult.Complete(
                                        customerState = requireNotNull(customerStateHolder.customer.value),
                                        selection = selectionHolder.selection.value,
                                    )
                                )
                            } else {
                                setManageResult(ManageResult.Cancelled(customerStateHolder.customer.value))
                            }
                            finish()
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
                val topBarState by screen.topBarState().collectAsState()
                PaymentSheetTopBar(
                    state = topBarState,
                    canNavigateBack = navigator.canGoBack,
                    isEnabled = true,
                    handleBackPressed = { manageNavigator.performAction(ManageNavigator.Action.Back) },
                )
            },
            content = {
                val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)
                val headerText by screen.title().collectAsState()
                headerText?.let { text ->
                    H4Text(
                        text = text.resolve(),
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .padding(horizontal = horizontalPadding),
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

    private fun setManageResult(result: ManageResult) {
        setResult(
            RESULT_OK,
            ManageResult.toIntent(intent, result)
        )
    }
}
