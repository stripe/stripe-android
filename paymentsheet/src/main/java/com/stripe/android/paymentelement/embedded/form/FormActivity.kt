package com.stripe.android.paymentelement.embedded.form

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
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

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val localArgs = args
        if (localArgs == null) {
            setFormResult(FormResult.Cancelled)
            finish()
            return
        }

        viewModel.component.inject(this)

        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = {
                        setResult(
                            Activity.RESULT_OK,
                            FormResult.toIntent(intent, FormResult.Cancelled)
                        )
                        finish()
                    }
                ) {
                    EventReporterProvider(eventReporter) {
                        VerticalModeFormUI(
                            interactor = formInteractor,
                            showsWalletHeader = false
                        )
                    }
                }
            }
        }
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
