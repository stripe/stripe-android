package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.orEmpty
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethod

internal interface SavedCardConfirmInteractor {
    val isLiveMode: Boolean
    val paymentMethod: DisplayableSavedPaymentMethod

    val linkConfigurationCoordinator: LinkConfigurationCoordinator

    val linkConfiguration: LinkConfiguration?

    fun handleViewAction(viewAction: ViewAction)

    sealed interface ViewAction {
        data class CheckLinkInlineSignup(
            val inlineSignupViewAction: InlineSignupViewState
        ) : ViewAction
    }

    interface Factory {
        fun create(
            paymentMethod: PaymentMethod,
            onUserInputChanged: (UserInput?) -> Unit,
        ) : SavedCardConfirmInteractor
    }
}

internal class DefaultSavedCardConfirmInteractor(
    override val isLiveMode: Boolean,
    paymentMethod: PaymentMethod,
    paymentMethodMetadata: PaymentMethodMetadata,
    override val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    override val linkConfiguration: LinkConfiguration?,
    val onUserInputChanged: (UserInput?) -> Unit,
) : SavedCardConfirmInteractor {
    override val paymentMethod = DisplayableSavedPaymentMethod(
        displayName = paymentMethodMetadata.supportedPaymentMethodForCode(PaymentMethod.Type.Card.code)?.displayName.orEmpty(),
        paymentMethod = paymentMethod,
        savedPaymentMethod = SavedPaymentMethod.Card(
            card = paymentMethod.card!!,
            billingDetails = null,
        ),
        isCbcEligible = false,
        shouldShowDefaultBadge = false,
    )

    override fun handleViewAction(viewAction: SavedCardConfirmInteractor.ViewAction) {
        when (viewAction) {
            is SavedCardConfirmInteractor.ViewAction.CheckLinkInlineSignup -> {
                val viewState = viewAction.inlineSignupViewAction
                onUserInputChanged(viewState.userInput?.takeIf {
                    viewState.useLink
                })
            }
        }
    }

    class Factory(
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val linkConfiguration: LinkConfiguration?,
        private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    ): SavedCardConfirmInteractor.Factory {
        override fun create(
            paymentMethod: PaymentMethod,
            onUserInputChanged: (UserInput?) -> Unit,
        ): SavedCardConfirmInteractor {
            return create(
                paymentMethodMetadata = paymentMethodMetadata,
                linkConfiguration = linkConfiguration,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                paymentMethod = paymentMethod,
                onUserInputChanged = onUserInputChanged,
            )
        }

    }

    companion object {
        fun create(
            paymentMethodMetadata: PaymentMethodMetadata,
            paymentMethod: PaymentMethod,
            linkConfiguration: LinkConfiguration?,
            linkConfigurationCoordinator: LinkConfigurationCoordinator,
            onUserInputChanged: (UserInput?) -> Unit,
        ): DefaultSavedCardConfirmInteractor {
            return DefaultSavedCardConfirmInteractor(
                isLiveMode = false,
                paymentMethod = paymentMethod,
                paymentMethodMetadata = paymentMethodMetadata,
                linkConfiguration = linkConfiguration,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                onUserInputChanged = onUserInputChanged,
            )
        }
    }
}