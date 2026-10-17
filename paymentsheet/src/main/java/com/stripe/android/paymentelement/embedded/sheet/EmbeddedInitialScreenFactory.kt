package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.manage.InitialManageScreenFactory
import javax.inject.Inject

internal class EmbeddedInitialScreenFactory @Inject constructor(
    private val launchMode: EmbeddedLaunchMode,
    private val formScreenFactory: EmbeddedNavigator.Screen.Form.Factory,
    private val initialManageScreenFactory: InitialManageScreenFactory,
    private val initialPaymentOptionsScreenFactory: InitialPaymentOptionsScreenFactory,
) {
    fun create(): List<EmbeddedNavigator.Screen> {
        return when (launchMode) {
            is EmbeddedLaunchMode.Form -> listOf(formScreenFactory.create(launchMode))
            is EmbeddedLaunchMode.Manage -> listOf(initialManageScreenFactory.createInitialScreen())
            is EmbeddedLaunchMode.PaymentOptions -> initialPaymentOptionsScreenFactory.createInitialScreen()
        }
    }
}
