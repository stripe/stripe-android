package com.stripe.android.paymentelement.embedded

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.AccountRange
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.darkThemeIconUrl
import com.stripe.android.paymentsheet.model.drawableResourceId
import com.stripe.android.paymentsheet.model.label
import com.stripe.android.paymentsheet.model.lightThemeIconUrl
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ExperimentalEmbeddedPaymentElementApi
internal class PaymentOptionDisplayDataFactory @Inject constructor(
    private val iconLoader: PaymentSelection.IconLoader,
    private val context: Context,
) {
    fun create(selection: PaymentSelection?, paymentMethodMetadata: PaymentMethodMetadata): EmbeddedPaymentElement.PaymentOptionDisplayData? {
        if (selection == null) {
            return null
        }

        val uiDefinitionArgumentsFactory: UiDefinitionFactory.Arguments.Factory= UiDefinitionFactory.Arguments.Factory.Default(
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            linkConfigurationCoordinator = null,
            onLinkInlineSignupStateChanged = {
                throw IllegalStateException("Not possible.")
            },
        )

        val mandate = when (selection) {
            is PaymentSelection.New -> {
                paymentMethodMetadata.formElementsForCode(selection.paymentMethodType, uiDefinitionArgumentsFactory)?.firstNotNullOfOrNull { it.mandateText }
            }
            is PaymentSelection.Saved -> {
                selection.mandateText("foobar", false)
            }
            is PaymentSelection.ExternalPaymentMethod -> null
            PaymentSelection.GooglePay -> null
            PaymentSelection.Link -> null
        }

        return EmbeddedPaymentElement.PaymentOptionDisplayData(
            label = selection.label.resolve(context),
            imageLoader = {
                iconLoader.load(
                    drawableResourceId = selection.drawableResourceId,
                    lightThemeIconUrl = selection.lightThemeIconUrl,
                    darkThemeIconUrl = selection.darkThemeIconUrl,
                )
            },
            billingDetails = null,
            paymentMethodType = selection.paymentMethodType,
            // TODO: Should this also check to see what the merchants' configuration is telling us to do?
            mandateText = if (mandate == null) null else AnnotatedString(mandate.resolve(context)),
        )
    }
}

private object NullCardAccountRangeRepositoryFactory : CardAccountRangeRepository.Factory {
    override fun create(): CardAccountRangeRepository {
        return NullCardAccountRangeRepository
    }

    override fun createWithStripeRepository(
        stripeRepository: StripeRepository,
        publishableKey: String
    ): CardAccountRangeRepository {
        return NullCardAccountRangeRepository
    }

    private object NullCardAccountRangeRepository : CardAccountRangeRepository {
        override suspend fun getAccountRange(cardNumber: CardNumber.Unvalidated): AccountRange? {
            return null
        }

        override suspend fun getAccountRanges(cardNumber: CardNumber.Unvalidated): List<AccountRange>? {
            return null
        }

        override val loading: StateFlow<Boolean> = stateFlowOf(false)
    }
}
