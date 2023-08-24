package com.stripe.android.customersheet.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultCustomerSheetEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSheetEventReporter {
    override fun onScreenPresented(screen: CustomerSheetEventReporter.Screen) {
        fireEvent(
            CustomerSheetEvent.ScreenPresented(
                screen = screen
            )
        )
    }

    override fun onConfirmPaymentMethodSucceeded(type: PaymentMethod.Type) {
        fireEvent(
            CustomerSheetEvent.ConfirmPaymentMethodSucceeded(
                type = type
            )
        )
    }

    override fun onConfirmPaymentMethodFailed(type: PaymentMethod.Type) {
        fireEvent(
            CustomerSheetEvent.ConfirmPaymentMethodFailed(
                type = type
            )
        )
    }

    override fun onEditTapped() {
        fireEvent(
            CustomerSheetEvent.EditTapped()
        )
    }

    override fun onEditCompleted() {
        fireEvent(
            CustomerSheetEvent.EditCompleted()
        )
    }

    override fun onRemovePaymentMethodSucceeded() {
        fireEvent(
            CustomerSheetEvent.RemovePaymentMethodSucceeded()
        )
    }

    override fun onRemovePaymentMethodFailed() {
        fireEvent(
            CustomerSheetEvent.RemovePaymentMethodFailed()
        )
    }

    override fun onAttachPaymentMethodSucceeded(
        style: CustomerSheetEventReporter.AddPaymentMethodStyle
    ) {
        fireEvent(
            CustomerSheetEvent.AttachPaymentMethodSucceeded(
                style = style
            )
        )
    }

    override fun onAttachPaymentMethodCanceled(
        style: CustomerSheetEventReporter.AddPaymentMethodStyle
    ) {
        if (style == CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent) {
            fireEvent(
                CustomerSheetEvent.AttachPaymentMethodCanceled()
            )
        }
    }

    override fun onAttachPaymentMethodFailed(
        style: CustomerSheetEventReporter.AddPaymentMethodStyle
    ) {
        fireEvent(
            CustomerSheetEvent.AttachPaymentMethodFailed(
                style = style
            )
        )
    }

    private fun fireEvent(event: CustomerSheetEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event,
                    event.additionalParams,
                )
            )
        }
    }
}
