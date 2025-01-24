package com.stripe.android.paymentelement.embedded.form

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut

internal class FormActivity : AppCompatActivity() {
    private val args: FormContract.Args? by lazy {
        FormContract.Args.fromIntent(intent)
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val localArgs = args
        if (localArgs == null) {
            setFormResult(FormResult.Cancelled)
            finish()
            return
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
                    Text(localArgs.selectedPaymentMethodCode)
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
