package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import javax.inject.Inject

internal fun interface EmbeddedFormScreenFactory {
    fun createFormScreen(code: PaymentMethodCode): EmbeddedNavigator.Screen.Form
}

internal class DefaultEmbeddedFormScreenFactory @Inject constructor(
    private val embeddedFormInteractorFactory: EmbeddedFormInteractorFactory,
    private val eventReporter: EventReporter,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
    private val confirmationHelper: SheetActivityConfirmationHelper,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val savedPaymentMethodConfirmInteractorFactory: SavedPaymentMethodConfirmInteractor.Factory,
    private val customerStateHolder: CustomerStateHolder,
    private val launchMode: EmbeddedLaunchMode,
) : EmbeddedFormScreenFactory {
    override fun createFormScreen(code: PaymentMethodCode): EmbeddedNavigator.Screen.Form {
        val hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.any { it.type?.code == code }
        val formInteractor = embeddedFormInteractorFactory.create(code, hasSavedPaymentMethods)
        return EmbeddedNavigator.Screen.Form(
            formInteractor = formInteractor,
            eventReporter = eventReporter,
            sheetActivityStateHolder = sheetActivityStateHolder,
            confirmationHelper = confirmationHelper,
            embeddedSelectionHolder = embeddedSelectionHolder,
            savedPaymentMethodConfirmInteractorFactory = savedPaymentMethodConfirmInteractorFactory,
            customerStateHolder = customerStateHolder,
            launchMode = launchMode,
        )
    }
}
