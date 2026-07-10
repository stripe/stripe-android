package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlin.test.Test

internal class EmbeddedFormHelperFactoryTest {

    @Test
    fun `shouldLaunchCardScanAutomatically is true for an empty card form when configured`() {
        val helper = createCardScanHelper(
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code,
            selection = null,
            openCardScanAutomatically = true,
        )

        assertThat(helper.shouldLaunchCardScanAutomatically).isTrue()
    }

    @Test
    fun `shouldLaunchCardScanAutomatically is false when the card form is being reopened with entered details`() {
        val helper = createCardScanHelper(
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code,
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            openCardScanAutomatically = true,
        )

        assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
    }

    @Test
    fun `shouldLaunchCardScanAutomatically is false for a non-card form`() {
        val helper = createCardScanHelper(
            selectedPaymentMethodCode = PaymentMethod.Type.CashAppPay.code,
            selection = null,
            openCardScanAutomatically = true,
        )

        assertThat(helper.shouldLaunchCardScanAutomatically).isFalse()
    }

    private fun createCardScanHelper(
        selectedPaymentMethodCode: PaymentMethodCode,
        selection: PaymentSelection?,
        openCardScanAutomatically: Boolean,
    ): AutomaticallyLaunchedCardScanFormDataHelper {
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())
        selectionHolder.set(selection)
        val factory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = SavedStateHandle(),
        )
        return factory.createAutomaticallyLaunchedCardScanFormDataHelper(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                openCardScanAutomatically = openCardScanAutomatically,
            ),
        )
    }
}
