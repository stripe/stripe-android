package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgsHolder
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedActivityArgsUpdater @Inject constructor(
    private val argsHolder: EmbeddedActivityArgsHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val initialScreenFactory: EmbeddedInitialScreenFactory,
    private val navigator: EmbeddedNavigator,
) {
    fun update(args: EmbeddedActivityArgs): Boolean {
        val currentMode = argsHolder.args.value.launchMode as? EmbeddedLaunchMode.PaymentOptions
        val updatedMode = args.launchMode as? EmbeddedLaunchMode.PaymentOptions
        if (currentMode?.isLoading != true || updatedMode?.isLoading != false) {
            return false
        }

        argsHolder.update(args)
        customerStateHolder.setCustomerState(args.customerState)
        selectionHolder.setPreviousNewSelections(args.previousNewSelections)
        selectionHolder.setSelection(args.selection)
        navigator.resetTo(initialScreenFactory.create(updatedMode))
        return true
    }
}
