package com.stripe.android.paymentelement.embedded.form

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut
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

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (args == null) {
            setCancelAndFinish()
            return
        }

        viewModel.component.subcomponentBuilder
            .activityResultCaller(this)
            .lifecycleOwner(this)
            .build()
            .inject(this)


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
                        eventReporter = eventReporter,
                        onDismissed = ::setCancelAndFinish,
                        onClick = confirmationHelper::confirm,
                        onProcessingCompleted = ::setCompletedResultAndDismiss,
                        stateHelper = formActivityStateHelper
                    )
                }
            }
        }
    }

    private fun setCompletedResultAndDismiss() {
        setFormResult(FormResult.Complete(null))
        finish()
    }

    private fun setCancelAndFinish() {
        setFormResult(FormResult.Cancelled)
        finish()
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun setFormResult(result: FormResult) {
        setResult(
            Activity.RESULT_OK,
            FormResult.toIntent(intent, result)
        )
    }
}
