package com.stripe.android.paymentsheet.paymentdatacollection.ach

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class UsBankAccountFormArgumentsKtxTest {
    @Test
    fun `primary button uses the supplied icon independently of the flow type`(
        @TestParameter isCompleteFlow: Boolean,
        @TestParameter primaryButtonIcon: PrimaryButtonIcon,
    ) = runTest {
        val updates = Turbine<(PrimaryButton.UIState?) -> PrimaryButton.UIState?>()
        val arguments = USBankAccountFormArgumentsFactory.create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selectedPaymentMethodCode = PaymentMethod.Type.USBankAccount.code,
            hostedSurface = "test_surface",
            integrationInputs = USBankAccountFormArgumentsFactory.IntegrationInputs(
                isCompleteFlow = isCompleteFlow,
                primaryButtonIcon = primaryButtonIcon,
                shippingDetails = null,
                draftPaymentSelection = null,
                autocompleteAddressInteractorFactory = null,
                setAsDefaultMatchesSaveForFutureUse = false,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
                onAnalyticsEvent = {},
                onMandateTextChanged = { _, _ -> },
                onLinkedBankAccountChanged = {},
                onUpdatePrimaryButtonUIState = updates::add,
                onUpdatePrimaryButtonState = {},
                onError = {},
                onFormCompleted = {},
            ),
        )

        arguments.handleScreenStateChanged(
            screenState = BankFormScreenState(isPaymentFlow = true),
            enabled = true,
            canClickWhileDisabled = true,
            onPrimaryButtonClick = {},
            onDisabledClick = {},
        )

        val buttonState = updates.awaitItem()(null)
        assertThat(buttonState?.icon).isEqualTo(primaryButtonIcon)
        updates.ensureAllEventsConsumed()
    }
}
