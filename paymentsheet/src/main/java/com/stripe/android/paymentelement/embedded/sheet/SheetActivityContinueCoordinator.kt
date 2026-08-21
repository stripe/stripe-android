package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import javax.inject.Inject

internal interface SheetActivityContinueCoordinator {
    fun onContinue()
}

internal class DefaultSheetActivityContinueCoordinator @Inject constructor(
    private val stateHolder: SheetActivityStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val launchMode: EmbeddedLaunchMode,
) : SheetActivityContinueCoordinator {

    override fun onContinue() {
        stateHolder.setResult(
            EmbeddedActivityResult.Complete(
                selection = selectionHolder.selection.value,
                previousNewSelections = selectionHolder.previousNewSelections,
                hasBeenConfirmed = false,
                customerState = customerStateHolder.customer.value,
                shouldInvokeSelectionCallback = false,
                launchMode = launchMode,
            )
        )
    }
}
