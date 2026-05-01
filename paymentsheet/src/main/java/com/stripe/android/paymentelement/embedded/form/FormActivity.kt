package com.stripe.android.paymentelement.embedded.form

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class FormActivity : AppCompatActivity() {
    private val args: FormContract.Args? by lazy {
        FormContract.Args.fromIntent(intent)
    }

    private val viewModel: FormActivityViewModel by viewModels {
        FormActivityViewModel.Factory {
            requireNotNull(args)
        }
    }

    @Inject
    lateinit var formScreen: EmbeddedNavigator.Screen.Form

    @Inject
    lateinit var formInteractor: DefaultVerticalModeFormInteractor

    @Inject
    lateinit var eventReporter: EventReporter

    @Inject
    lateinit var formActivityStateHelper: FormActivityStateHelper

    @Inject
    lateinit var customerStateHolder: CustomerStateHolder

    @Inject
    lateinit var formActivityRegistrar: FormActivityRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (args == null) {
            setCancelAndFinish()
            return
        }

        renderEdgeToEdge()

        viewModel.component.inject(this)

        formActivityRegistrar.registerAndBootstrap(
            activityResultCaller = this,
            lifecycleOwner = this,
        )

        lifecycleScope.launch {
            formActivityStateHelper.result.collect {
                setFormResult(it)
                finish()
            }
        }

        setContent {
            StripeTheme {
                FormSheetContent(
                    formScreen = formScreen,
                )
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun FormSheetContent(formScreen: EmbeddedNavigator.Screen.Form) {
        val bottomSheetState = rememberStripeBottomSheetState(
            confirmValueChange = { !formScreen.isPerformingNetworkOperation() }
        )
        ElementsBottomSheetLayout(
            state = bottomSheetState,
            onDismissed = ::setCancelAndFinish,
        ) {
            val scrollState = rememberScrollState()
            BottomSheetScaffold(
                topBar = {
                    val topBarState by remember(formScreen) {
                        formScreen.topBarState()
                    }.collectAsState()
                    PaymentSheetTopBar(
                        state = topBarState,
                        canNavigateBack = false,
                        isEnabled = true,
                        handleBackPressed = ::setCancelAndFinish,
                    )
                },
                content = {
                    formScreen.Content()
                },
                scrollState = scrollState,
            )
        }
    }

    private fun setCancelAndFinish() {
        setFormResult(
            FormResult.Cancelled(
                customerState = getCustomerState(),
            )
        )
        finish()
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

    private fun setFormResult(result: FormResult) {
        setResult(
            Activity.RESULT_OK,
            FormResult.toIntent(intent, result)
        )
    }
}
