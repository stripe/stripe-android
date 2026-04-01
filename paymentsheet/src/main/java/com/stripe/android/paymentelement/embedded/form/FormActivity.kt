package com.stripe.android.paymentelement.embedded.form

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.uicore.StripeTheme
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
    lateinit var formScreen: FormScreen

    @Inject
    lateinit var eventReporter: EventReporter

    @Inject
    lateinit var formActivityStateHelper: FormActivityStateHelper

    @Inject
    lateinit var customerStateHolder: CustomerStateHolder

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
                formScreen.Content(
                    onProcessingCompleted = ::setCompletedResultAndDismiss,
                    onDismissed = ::setCancelAndFinish,
                )
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
