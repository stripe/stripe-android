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
        isVerifyingMicrodeposits: Boolean,
        isSaveForFutureUseSelected: Boolean,
        isInstantDebits: Boolean,
        isSetupFlow: Boolean,
    ): ResolvableString {
        val mandateText = buildMandateText(
            merchantName = merchantName,
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
        val text = if (isSaveForFutureUseSelected || isSetupFlow) {
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
