package com.stripe.android.paymentelement.embedded

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut

internal class FormActivity : AppCompatActivity() {
    private val formArgs: FormContract.Args? by lazy {
        FormContract.Args.fromIntent(intent)
    }

    private val viewModel: FormActivityViewModel by viewModels {
        FormActivityViewModel.Factory()
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (formArgs == null) {
            setFormResult(FormResult.Cancelled)
            finish()
            return
        }

        formArgs?.let { args ->
            args.paymentMethodMetadata?. let { metadata ->
                viewModel.initializeFormInteractor(
                    metadata,
                    args.selectedPaymentMethodCode,
                )
            }
        }

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
                    VerticalModeFormUI(
                        viewModel.formInteractor,
                        false,
                    )
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
