package com.stripe.android.link.ui.cardedit

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class CardEditViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `no paymentDetailsId in savedStateHandle exits screen and logs event`() = runTest {
        var goBackCalled = false
        val errorReporter = FakeErrorReporter()

        viewModel(
            paymentDetailsId = null,
            errorReporter = errorReporter,
            goBack = {
                goBackCalled = true
            }
        )

        assertThat(goBackCalled).isTrue()
        assertThat(errorReporter.getLoggedErrors())
            .containsExactly(
                ErrorReporter.UnexpectedErrorEvent.LINK_NATIVE_CARD_EDIT_PAYMENT_DETAILS_ID_NOT_FOUND.eventName
            )
    }

    @Test
    fun `failure to load payment details exits screen`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.listPaymentDetailsResult = Result.failure(Throwable())

        var exitCalled = false
        viewModel(
            paymentDetailsId = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id,
            linkAccountManager = linkAccountManager,
            goBack = {
                exitCalled = true
            }
        )

        assertThat(exitCalled).isTrue()
    }

    @Test
    fun `viewmodel loads with correct state`() = runTest {
        val viewModel = viewModel(
            paymentDetailsId = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id
        )

        assertThat(viewModel.cardEditState.value.cardDefaultState)
            .isEqualTo(CardDefaultState.CardIsDefault)
        assertThat(viewModel.cardEditState.value.isUpdating).isFalse()
        assertThat(viewModel.cardEditState.value.cardDetailsEntry).isNull()
        assertThat(viewModel.cardEditState.value.paymentDetail)
            .isEqualTo(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)
        assertThat(viewModel.cardEditState.value.errorMessage).isNull()
    }

    @Test
    fun `primary button is enabled when card details are entered`() = runTest {
        val viewModel = viewModel(
            paymentDetailsId = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id
        )

        viewModel.cardBillingAddressElement.setRawValue(
            rawValuesMap = mapOf(
                IdentifierSpec.PostalCode to "94107",
                IdentifierSpec.Country to "US",
            )
        )
        viewModel.cardBillingAddressElement.sectionFieldErrorController()

        viewModel.cvcExpiryElement.setRawValue(
            rawValuesMap = mapOf(
                IdentifierSpec.CardCvc to "434",
                CardEditViewModel.dateExpSpec to "1226"
            )
        )

        val cardDetailsEntry = viewModel.cardEditState.value.cardDetailsEntry
        assertThat(cardDetailsEntry?.cvc?.first).isEqualTo(IdentifierSpec.CardCvc)
        assertThat(cardDetailsEntry?.cvc?.second?.value).isEqualTo("434")
        assertThat(cardDetailsEntry?.cvc?.second?.isComplete).isTrue()

        assertThat(cardDetailsEntry?.date?.first).isEqualTo(CardEditViewModel.dateExpSpec)
        assertThat(cardDetailsEntry?.date?.second?.value).isEqualTo("1226")
        assertThat(cardDetailsEntry?.cvc?.second?.isComplete).isTrue()

        assertThat(cardDetailsEntry?.postalCode?.first).isEqualTo(IdentifierSpec.PostalCode)
        assertThat(cardDetailsEntry?.postalCode?.second?.value).isEqualTo("94107")
        assertThat(cardDetailsEntry?.postalCode?.second?.isComplete).isTrue()

        assertThat(cardDetailsEntry?.country?.first).isEqualTo(IdentifierSpec.Country)
        assertThat(cardDetailsEntry?.country?.second?.value).isEqualTo("US")
        assertThat(cardDetailsEntry?.country?.second?.isComplete).isTrue()

        assertThat(viewModel.cardEditState.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun `viewmodel should navigate to wallet screen when wallet is successful`() = runTest {
        var destinationScreen: LinkScreen? = null
        val viewModel = viewModel(
            paymentDetailsId = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id,
            navigateAndClearStack = {
                destinationScreen = it
            }
        )

        viewModel.cardBillingAddressElement.setRawValue(
            rawValuesMap = mapOf(
                IdentifierSpec.PostalCode to "94107",
                IdentifierSpec.Country to "US",
            )
        )
        viewModel.cardBillingAddressElement.sectionFieldErrorController()

        viewModel.cvcExpiryElement.setRawValue(
            rawValuesMap = mapOf(
                IdentifierSpec.CardCvc to "434",
                CardEditViewModel.dateExpSpec to "1226"
            )
        )

        viewModel.updateCardClicked()

        assertThat(destinationScreen).isEqualTo(LinkScreen.Wallet)
    }

    @Test
    fun `viewmodel should show error message when card update fails`() = runTest {
        val error = Throwable("oops")
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.updatePaymentDetailsResult = Result.failure(error)

        var destinationScreen: LinkScreen? = null
        val viewModel = viewModel(
            paymentDetailsId = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id,
            linkAccountManager = linkAccountManager,
            navigateAndClearStack = {
                destinationScreen = it
            }
        )

        viewModel.cardBillingAddressElement.setRawValue(
            rawValuesMap = mapOf(
                IdentifierSpec.PostalCode to "94107",
                IdentifierSpec.Country to "US",
            )
        )
        viewModel.cardBillingAddressElement.sectionFieldErrorController()

        viewModel.cvcExpiryElement.setRawValue(
            rawValuesMap = mapOf(
                IdentifierSpec.CardCvc to "434",
                CardEditViewModel.dateExpSpec to "1226"
            )
        )

        viewModel.updateCardClicked()

        assertThat(destinationScreen).isNull()
        assertThat(viewModel.cardEditState.value.errorMessage).isEqualTo(error.stripeErrorMessage())
    }

    private fun viewModel(
        paymentDetailsId: String? = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id,
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        navigateAndClearStack: (LinkScreen) -> Unit = {},
        goBack: () -> Unit = {}
    ): CardEditViewModel {
        return CardEditViewModel(
            savedStateHandle = SavedStateHandle().apply {
                if (paymentDetailsId != null) {
                    set(CardEditViewModel.PAYMENT_DETAILS_ID, paymentDetailsId)
                }
            },
            linkAccountManager = linkAccountManager,
            errorReporter = errorReporter,
            navigateAndClearStack = navigateAndClearStack,
            goBack = goBack
        )
    }
}
