package com.stripe.android.paymentelement.embedded.form

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.CustomerState
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
    lateinit var formInteractor: DefaultVerticalModeFormInteractor

    @Inject
    lateinit var eventReporter: EventReporter

    @Inject
    lateinit var formActivityStateHelper: FormActivityStateHelper

    @Inject
    lateinit var confirmationHelper: FormActivityConfirmationHelper

    @Inject
    lateinit var customerStateHolder: CustomerStateHolder

    @Inject
    lateinit var savedPaymentMethodConfirmInteractorFactory: SavedPaymentMethodConfirmInteractor.Factory

    @Inject
    lateinit var embeddedSelectionHolder: EmbeddedSelectionHolder

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (args == null) {
            setCancelAndFinish()
            return
        }

        renderEdgeToEdge()

        viewModel.component.subcomponentFactory.build(
            activityResultCaller = this,
            lifecycleOwner = this,
        ).inject(this)

        lifecycleScope.launch {
            formActivityStateHelper.result.collect {
                setFormResult(it)
                finish()
            }
        }

        setContent {
            StripeTheme {
                val state by formActivityStateHelper.state.collectAsState()
                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { !state.isProcessing }
                )
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = ::setCancelAndFinish
                ) {
                    FormActivityUI(
                        interactor = formInteractor,
                        savedPaymentMethodConfirmInteractorFactory = savedPaymentMethodConfirmInteractorFactory,
                        eventReporter = eventReporter,
                        onClick = {
                            confirmationHelper.confirm()
                        },
                        onProcessingCompleted = ::setCompletedResultAndDismiss,
                        state = state,
                        onDismissed = ::setCancelAndFinish,
                        updateSelection = embeddedSelectionHolder::set,
                    )
                }
            }
        }
    }

    private fun setCompletedResultAndDismiss() {
        setFormResult(
            FormResult.Complete(
                selection = null,
                hasBeenConfirmed = true,
                customerState = getCustomerState(),
            )
        )
        finish()
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

        if (isFinishing && ::eventReporter.isInitialized) {
            eventReporter.onDismiss()
        }
    }

    private fun setFormResult(result: FormResult) {
        setResult(
            Activity.RESULT_OK,
            FormResult.toIntent(intent, result)
        )
    }
}
