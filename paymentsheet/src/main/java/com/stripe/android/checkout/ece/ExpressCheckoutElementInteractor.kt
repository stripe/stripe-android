@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.ece

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.collections.emptyList

internal interface ExpressCheckoutElementInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val expressButtons: List<ExpressButton>,
    )

    sealed class ViewAction {
        data object OnDisplayed : ViewAction()

        data class OnWalletTapped(
            val expressButton: ExpressButton,
        ) : ViewAction()
    }
}

internal class DefaultExpressCheckoutElementInteractor @Inject constructor(
    linkAccountHolder: LinkAccountHolder,
    stateHolder: CheckoutControllerStateHolder,
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: ExpressCheckoutElementEventReporter,
    private val expressCheckoutElementConfirmationPerformer: ExpressCheckoutElementConfirmationPerformer,
) : ExpressCheckoutElementInteractor {

    private var hasReportedDisplayed: Boolean
        get() = savedStateHandle[KEY_ECE_DISPLAYED] ?: false
        set(value) {
            savedStateHandle[KEY_ECE_DISPLAYED] = value
        }

    override val state: StateFlow<ExpressCheckoutElementInteractor.State> = combineAsStateFlow(
        linkAccountHolder.linkAccountInfo,
        stateHolder.stateFlow,
        stateHolder.checkoutSession,
    ) { linkAccountInfo, state, checkoutSession, ->
        if (state == null || checkoutSession == null) {
            return@combineAsStateFlow ExpressCheckoutElementInteractor.State(expressButtons = emptyList())
        }

        ExpressCheckoutElementInteractor.State(
            expressButtons = checkoutSession.availableExpressButtonTypes.map { expressButtonType ->
                when (expressButtonType) {
                    ExpressButtonType.Link -> ExpressButton.Link.create(
                        paymentMethodMetadata = state.paymentMethodMetadata,
                        linkAccountInfo = linkAccountInfo,
                    )
                    is ExpressButtonType.GooglePay -> ExpressButton.GooglePay.create(
                        paymentMethodMetadata = state.paymentMethodMetadata,
                        googlePayConfiguration = expressButtonType.googlePayConfiguration,
                    )
                }
            },
        )
    }

    override fun handleViewAction(viewAction: ExpressCheckoutElementInteractor.ViewAction) {
        when (viewAction) {
            ExpressCheckoutElementInteractor.ViewAction.OnDisplayed -> {
                if (!hasReportedDisplayed) {
                    hasReportedDisplayed = true
                    eventReporter.onEceDisplayed()
                }
            }
            is ExpressCheckoutElementInteractor.ViewAction.OnWalletTapped -> {
                eventReporter.onEceWalletTapped()

                expressCheckoutElementConfirmationPerformer.confirm(viewAction.expressButton)
            }
        }
    }

    private companion object {
        const val KEY_ECE_DISPLAYED = "express_checkout_element_displayed"
    }
}
