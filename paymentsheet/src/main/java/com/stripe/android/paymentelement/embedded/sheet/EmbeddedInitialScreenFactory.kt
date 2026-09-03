package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.manage.InitialManageScreenFactory
import javax.inject.Inject
import javax.inject.Provider

internal class EmbeddedInitialScreenFactory @Inject constructor(
    private val formScreenFactory: Provider<EmbeddedNavigator.Screen.Form.Factory>,
    private val initialManageScreenFactory: Provider<InitialManageScreenFactory>,
    private val initialPaymentOptionsScreenFactory: Provider<InitialPaymentOptionsScreenFactory>,
) {
    fun create(launchMode: EmbeddedLaunchMode): List<EmbeddedNavigator.Screen> {
        return when (launchMode) {
            is EmbeddedLaunchMode.Form -> listOf(formScreenFactory.get().create(launchMode))
            is EmbeddedLaunchMode.Manage -> listOf(initialManageScreenFactory.get().createInitialScreen())
            is EmbeddedLaunchMode.PaymentOptions -> {
                if (launchMode.isLoading) {
                    listOf(EmbeddedNavigator.Screen.Loading)
                } else {
                    initialPaymentOptionsScreenFactory.get().createInitialScreen()
                }
            }
        }
    }
}
