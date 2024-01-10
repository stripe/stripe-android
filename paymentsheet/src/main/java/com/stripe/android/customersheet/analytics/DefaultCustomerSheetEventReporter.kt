package com.stripe.android.customersheet.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.model.CardBrand
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

    override fun onScreenHidden(screen: CustomerSheetEventReporter.Screen) {
        when (screen) {
            CustomerSheetEventReporter.Screen.EditPaymentMethod -> {
                fireEvent(
                    CustomerSheetEvent.ScreenHidden(
                        screen = screen
                    )
                )
            }
            else -> Unit
        }
    }

    override fun onConfirmPaymentMethodSucceeded(type: String) {
        fireEvent(
            CustomerSheetEvent.ConfirmPaymentMethodSucceeded(
                type = type
            )
        )
    }

    override fun onConfirmPaymentMethodFailed(type: String) {
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

    override fun onShowPaymentOptionBrands(
        source: CustomerSheetEventReporter.CardBrandChoiceEventSource,
        selectedBrand: CardBrand
    ) {
        fireEvent(
            CustomerSheetEvent.ShowPaymentOptionBrands(
                source = when (source) {
                    CustomerSheetEventReporter.CardBrandChoiceEventSource.Add ->
                        CustomerSheetEvent.ShowPaymentOptionBrands.Source.Add
                    CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit ->
                        CustomerSheetEvent.ShowPaymentOptionBrands.Source.Edit
                },
                selectedBrand = selectedBrand
            )
        )
    }

    override fun onHidePaymentOptionBrands(
        source: CustomerSheetEventReporter.CardBrandChoiceEventSource,
        selectedBrand: CardBrand?
    ) {
        fireEvent(
            CustomerSheetEvent.HidePaymentOptionBrands(
                source = when (source) {
                    CustomerSheetEventReporter.CardBrandChoiceEventSource.Add ->
                        CustomerSheetEvent.HidePaymentOptionBrands.Source.Add
                    CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit ->
                        CustomerSheetEvent.HidePaymentOptionBrands.Source.Edit
                },
                selectedBrand = selectedBrand
            )
        )
    }

    override fun onUpdatePaymentMethodSucceeded(
        selectedBrand: CardBrand,
    ) {
        fireEvent(
            CustomerSheetEvent.UpdatePaymentOptionSucceeded(
                selectedBrand = selectedBrand
            )
        )
    }

    override fun onUpdatePaymentMethodFailed(
        selectedBrand: CardBrand,
        error: Throwable,
    ) {
        fireEvent(
            CustomerSheetEvent.UpdatePaymentOptionFailed(
                selectedBrand = selectedBrand,
                error = error
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
