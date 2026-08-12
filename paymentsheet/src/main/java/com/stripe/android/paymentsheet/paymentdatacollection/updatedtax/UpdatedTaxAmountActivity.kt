package com.stripe.android.paymentsheet.paymentdatacollection.updatedtax

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut

internal class UpdatedTaxAmountActivity : AppCompatActivity() {
    private val args: UpdatedTaxAmountContract.Args by lazy {
        requireNotNull(UpdatedTaxAmountContract.Args.fromIntent(intent)) {
            "Cannot start updated tax amount flow without args"
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (UpdatedTaxAmountContract.Args.fromIntent(intent) == null) {
            finish()
            return
        }

        args.appearance.parseAppearance()
        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()

                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = { finishWithResult(UpdatedTaxAmountResult.Cancelled) },
                ) {
                    UpdatedTaxAmountScreen(
                        displayItems = args.displayItems,
                        currency = args.currency,
                        onConfirm = { finishWithResult(UpdatedTaxAmountResult.Confirmed) },
                        onDismiss = { finishWithResult(UpdatedTaxAmountResult.Cancelled) },
                    )
                }
            }
        }
    }

    private fun finishWithResult(result: UpdatedTaxAmountResult) {
        setResult(
            Activity.RESULT_OK,
            UpdatedTaxAmountResult.toIntent(intent, result),
        )
        finish()
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}
