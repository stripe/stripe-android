package com.stripe.android.paymentsheet.model

import android.content.Context
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.wallet.paymentOptionLabel
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.ui.createCardLabel
import com.stripe.android.ui.core.R as StripeUiCoreR

internal object PaymentOptionLabelsFactory {

    fun create(
        context: Context,
        selection: PaymentSelection,
    ): PaymentOption.Labels {
        return createInternal(
            context = context,
            selection = selection,
            linkBrand = null,
        )
    }

    fun create(
        context: Context,
        selection: PaymentSelection,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): PaymentOption.Labels {
        return createInternal(
            context = context,
            selection = selection,
            linkBrand = paymentMethodMetadata.linkBrand,
        )
    }

    fun create(
        context: Context,
        selection: PaymentSelection,
        linkBrand: LinkBrand,
    ): PaymentOption.Labels {
        return createInternal(
            context = context,
            selection = selection,
            linkBrand = linkBrand,
        )
    }

    private fun createInternal(
        context: Context,
        selection: PaymentSelection,
        linkBrand: LinkBrand?,
    ): PaymentOption.Labels {
        val label = selection.resolveLabel(context, linkBrand)
        val fallback = PaymentOption.Labels(
            label = label,
            sublabel = null,
        )

        return when (selection) {
            is PaymentSelection.CustomPaymentMethod,
            is PaymentSelection.ExternalPaymentMethod,
            is PaymentSelection.GooglePay,
            is PaymentSelection.ShopPay,
            is PaymentSelection.New.GenericPaymentMethod -> {
                fallback
            }
            is PaymentSelection.New.Card -> {
                newCard(context, selection)
            }
            is PaymentSelection.New.USBankAccount -> {
                newUSBankAccount(context, selection)
            }
            is PaymentSelection.Saved -> {
                savedSelection(context, selection, label, linkBrand) ?: fallback
            }
            is PaymentSelection.Link -> {
                link(context, selection)
            }
        }
    }

    private fun PaymentSelection.resolveLabel(
        context: Context,
        linkBrand: LinkBrand?,
    ): String {
        val resolvableLabel = if (linkBrand == null) {
            label
        } else {
            label(linkBrand)
        }
        return resolvableLabel.resolve(context)
    }

    private fun savedSelection(
        context: Context,
        selection: PaymentSelection.Saved,
        label: String,
        linkBrand: LinkBrand?,
    ): PaymentOption.Labels? {
        return selection.paymentMethod.run {
            linkPaymentDetails?.let { savedLink(context, it, linkBrand ?: LinkBrand.Link) }
                ?: card?.let { savedCard(context, it) }
                ?: usBankAccount?.let { savedUSBankAccount(it, label) }
        }
    }

    private fun newCard(
        context: Context,
        selection: PaymentSelection.New.Card,
    ): PaymentOption.Labels {
        return PaymentOption.Labels(
            label = selection.brand.displayName,
            sublabel = selection.label.resolve(context),
        )
    }

    private fun newUSBankAccount(
        context: Context,
        selection: PaymentSelection.New.USBankAccount,
    ): PaymentOption.Labels {
        val bankLast4 = selection.label
        val bankName = selection.screenState.linkedBankAccount?.bankName
        return PaymentOption.Labels(
            label = bankName ?: StripeUiCoreR.string.stripe_payment_method_bank.resolvableString.resolve(context),
            sublabel = bankLast4,
        )
    }

    private fun savedLink(
        context: Context,
        linkDetails: LinkPaymentDetails,
        linkBrand: LinkBrand,
    ): PaymentOption.Labels {
        return PaymentOption.Labels(
            label = linkBrand.resolvableString.resolve(context),
            sublabel = linkDetails.paymentOptionLabel.resolve(context),
        )
    }

    private fun savedCard(
        context: Context,
        card: PaymentMethod.Card,
    ): PaymentOption.Labels {
        val brand = (card.displayBrand?.let { CardBrand.fromCode(it) } ?: card.brand).takeIf {
            it != CardBrand.Unknown
        }
        val last4 = createCardLabel(card.last4)?.resolve(context).orEmpty()

        return if (brand != null) {
            PaymentOption.Labels(
                label = brand.displayName,
                sublabel = last4,
            )
        } else {
            PaymentOption.Labels(
                label = last4,
                sublabel = null,
            )
        }
    }

    private fun savedUSBankAccount(
        usBankAccount: PaymentMethod.USBankAccount,
        label: String,
    ): PaymentOption.Labels {
        val bankName = usBankAccount.bankName

        return if (bankName != null) {
            PaymentOption.Labels(
                label = bankName,
                sublabel = label,
            )
        } else {
            PaymentOption.Labels(
                label = label,
                sublabel = null,
            )
        }
    }

    private fun link(
        context: Context,
        selection: PaymentSelection.Link,
    ): PaymentOption.Labels {
        val sublabel = selection.selectedPayment?.details?.paymentOptionLabel?.resolve(context)
        return PaymentOption.Labels(
            label = selection.linkBrand.resolvableString.resolve(context),
            sublabel = sublabel,
        )
    }
}
