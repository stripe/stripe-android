package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CollectMissingLinkBillingDetailsPreview
import com.stripe.android.LinkDisallowFundingSourceCreationPreview
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

@OptIn(
    CheckoutSessionPreview::class,
    CollectMissingLinkBillingDetailsPreview::class,
    LinkDisallowFundingSourceCreationPreview::class,
)
internal class ExpressCheckoutElementLinkConfigurationMapperTest {
    @Test
    fun `asPaymentSheet maps Link configuration`() {
        val configuration = ExpressCheckoutElement.Configuration.LinkConfiguration()
            .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.WalletButtonHidden)
            .collectMissingBillingDetailsForExistingPaymentMethods(false)
            .disallowFundingSourceCreation(setOf("card", "bank_account"))
            .build()

        val mapped = configuration.asPaymentSheet()

        assertThat(mapped.display).isEqualTo(PaymentSheet.LinkConfiguration.Display.WalletButtonHidden)
        assertThat(mapped.collectMissingBillingDetailsForExistingPaymentMethods).isFalse()
        assertThat(mapped.allowUserEmailEdits).isTrue()
        assertThat(mapped.allowLogOut).isTrue()
        assertThat(mapped.disallowFundingSourceCreation).containsExactly("card", "bank_account")
    }
}
