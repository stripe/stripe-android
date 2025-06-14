package com.stripe.android.paymentsheet.model

import android.content.Context
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.ui.wallet.paymentOptionLabel
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.ui.createCardLabel

internal object PaymentOptionLabelsFactory {

    fun create(
        context: Context,
        selection: PaymentSelection,
    ): PaymentOption.Labels {
        val label = selection.label.resolve(context)
        val fallback = PaymentOption.Labels(
            label = label,
            sublabel = null,
        )

        return when (selection) {
            is PaymentSelection.CustomPaymentMethod,
            is PaymentSelection.ExternalPaymentMethod,
            is PaymentSelection.GooglePay,
            is PaymentSelection.New.GenericPaymentMethod -> {
                fallback
            }
            is PaymentSelection.New.Card -> {
                newCard(context, selection)
            }
            is PaymentSelection.New.LinkInline -> {
                newCard(context, selection)
            }
            is PaymentSelection.New.USBankAccount -> {
                newUSBankAccount(selection)
            }
            is PaymentSelection.Saved -> {
                if (selection.paymentMethod.isLinkPaymentMethod) {
                    savedLink(context, selection)
                } else if (selection.paymentMethod.card != null) {
                    savedCard(context, selection.paymentMethod.card!!)
                } else if (selection.paymentMethod.usBankAccount != null) {
                    savedUSBankAccount(selection.paymentMethod.usBankAccount!!, label)
                } else {
                    fallback
                }
            }
            is PaymentSelection.Link -> {
                link(context, selection.selectedPayment)
            }
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

    private fun newCard(
        context: Context,
        selection: PaymentSelection.New.LinkInline,
    ): PaymentOption.Labels {
        return PaymentOption.Labels(
            label = selection.brand.displayName,
            sublabel = selection.label.resolve(context),
        )
    }

    private fun newUSBankAccount(
        selection: PaymentSelection.New.USBankAccount,
    ): PaymentOption.Labels {
        val originalLabel = selection.label
        val potentialLabel = selection.screenState.linkedBankAccount?.bankName
        return PaymentOption.Labels(
            label = potentialLabel ?: originalLabel,
            sublabel = originalLabel.takeUnless { it == potentialLabel },
        )
    }

    private fun savedLink(
        context: Context,
        selection: PaymentSelection.Saved,
    ): PaymentOption.Labels {
        return PaymentOption.Labels(
            label = R.string.stripe_link.resolvableString.resolve(context),
            sublabel = selection.paymentMethod.linkPaymentDetails?.paymentOptionLabel?.resolve(context),
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
        paymentMethod: LinkPaymentMethod?,
    ): PaymentOption.Labels {
        val sublabel = paymentMethod?.details?.paymentOptionLabel?.resolve(context)
        return PaymentOption.Labels(
            label = R.string.stripe_link.resolvableString.resolve(context),
            sublabel = sublabel,
        )
    }
}
