package com.stripe.android.paymentelement.embedded.manage

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut
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

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val localArgs = args
        if (localArgs == null) {
            finish()
            return
        }

        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = {
                        setManageResult(ManageResult.Cancelled(viewModel.customerStateHolder.customer.value))
                        finish()
                    }
                ) {
                    Text("Manage Screen")
                }
            }
        }
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
