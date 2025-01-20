package com.stripe.android.paymentelement.embedded

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut
import java.util.UUID

internal class FormActivity : AppCompatActivity() {
    private val args: FormContract.Args? by lazy {
        FormContract.Args.fromIntent(intent)
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (args == null) {
            setFormResult(FormResult.Cancelled)
            finish()
        }

        setContent {

//            val component = remember() {  }
//
//            val uuid = rememberSaveable(
//                args?.selectedPaymentMethodCode, args?.paymentMethodMetadata
//            ) {
//                UUID.randomUUID().toString()
//            }
//
//            val viewModel: FormActivityViewModel = viewModel(
//                key = uuid,
//                factory = FormActivityViewModel.Factory(
//                    args.paymentMethodMetadata!!,
//                    args.selectedPaymentMethodCode!!,
//
//                )
//            )
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
                    Text(args?.selectedPaymentMethodCode ?: "Whoops")
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
