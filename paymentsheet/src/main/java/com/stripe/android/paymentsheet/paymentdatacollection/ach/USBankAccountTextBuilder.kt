package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.annotation.VisibleForTesting
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.plus
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.strings.transformations.Replace
import com.stripe.android.paymentsheet.R

/**
 * Temporary hack to get mandate text to display properly until translations are fixed
 */
internal object USBankAccountTextBuilder {

    fun buildMandateAndMicrodepositsText(
        merchantName: String,
        sellerBusinessName: String?,
        forceSetupFutureUseBehavior: Boolean,
        isVerifyingMicrodeposits: Boolean,
        isSaveForFutureUseSelected: Boolean,
        isInstantDebits: Boolean,
        isSetupFlow: Boolean,
    ): ResolvableString {
        val mandateText = buildMandateText(
            merchantName = merchantName,
            sellerBusinessName = sellerBusinessName,
            forceSetupFutureUseBehavior = forceSetupFutureUseBehavior,
            isSaveForFutureUseSelected = isSaveForFutureUseSelected,
            isInstantDebits = isInstantDebits,
            isSetupFlow = isSetupFlow,
        )

        val microdepositsText = if (isVerifyingMicrodeposits) {
            resolvableString(R.string.stripe_paymentsheet_microdeposit, merchantName)
        } else {
            null
        }

        return if (microdepositsText != null) {
            microdepositsText + " ".resolvableString + mandateText
        } else {
            mandateText
        }
    }

    @VisibleForTesting
    fun buildMandateText(
        merchantName: String,
        sellerBusinessName: String?,
        forceSetupFutureUseBehavior: Boolean,
        isSaveForFutureUseSelected: Boolean,
        isInstantDebits: Boolean,
        isSetupFlow: Boolean,
    ): ResolvableString {
        val transforms = listOf(
            Replace(
                original = "<terms>",
                replacement = "<a href=\"${getTermsLink(isInstantDebits)}\">",
            ),
            Replace(
                original = "</terms>",
                replacement = "</a>",
            ),
        )

        val text = if (forceSetupFutureUseBehavior && sellerBusinessName != null) {
            resolvableString(
                R.string.stripe_wallet_bank_account_terms_merchant_and_seller,
                merchantName,
                sellerBusinessName,
                merchantName,
                transformations = transforms
            )
        } else if (sellerBusinessName != null) {
            resolvableString(
                R.string.stripe_wallet_bank_account_terms_seller,
                sellerBusinessName,
                transformations = transforms
            )
        } else if (isSaveForFutureUseSelected || isSetupFlow) {
            resolvableString(R.string.stripe_paymentsheet_ach_save_mandate, merchantName, transformations = transforms)
        } else {
            resolvableString(R.string.stripe_paymentsheet_ach_continue_mandate, transformations = transforms)
        }

        return text
    }

    private fun getTermsLink(isInstantDebits: Boolean) = when (isInstantDebits) {
        true -> "https://link.com/terms/ach-authorization"
        false -> "https://stripe.com/ach-payments/authorization"
    }
}
