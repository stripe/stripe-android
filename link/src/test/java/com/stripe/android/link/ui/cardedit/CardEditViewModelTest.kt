package com.stripe.android.link.ui.cardedit

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.model.PaymentDetailsFixtures
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CardEditViewModelTest {
    private val linkAccount = mock<LinkAccount>().apply {
        whenever(email).thenReturn("email@stripe.com")
        whenever(clientSecret).thenReturn(CLIENT_SECRET)
    }
    private lateinit var linkAccountManager: LinkAccountManager
    private val navigator = mock<Navigator>()
    private val formControllerSubcomponent = mock<FormControllerSubcomponent>().apply {
        whenever(formController).thenReturn(mock())
    }
    private val formControllerProvider = Provider {
        mock<FormControllerSubcomponent.Builder>().apply {
            whenever(formSpec(anyOrNull())).thenReturn(this)
            whenever(initialValues(anyOrNull())).thenReturn(this)
            whenever(viewOnlyFields(anyOrNull())).thenReturn(this)
            whenever(viewModelScope(anyOrNull())).thenReturn(this)
            whenever(baseFormArgs(anyOrNull())).thenReturn(this)
            whenever(build()).thenReturn(formControllerSubcomponent)
        }
    }
    private val formValues = mapOf(
        IdentifierSpec.CardNumber to FormFieldEntry("•••• 0005", true),
        IdentifierSpec.CardCvc to FormFieldEntry("123", true),
        IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
        IdentifierSpec.CardExpMonth to FormFieldEntry("12", true),
        IdentifierSpec.CardExpYear to FormFieldEntry("2040", true),
        IdentifierSpec.Country to FormFieldEntry("US", true),
        IdentifierSpec.PostalCode to FormFieldEntry("12345", true)
    )
    private val args = mock<LinkActivityContract.Args>()

    @Before
    fun before() {
        linkAccountManager = mock()
    }

    @Test
    fun `On initialization when payment details exist then UI is loaded`() = runTest {
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(PaymentDetailsFixtures.CONSUMER_PAYMENT_DETAILS))

        val viewModel = createViewModel()
        viewModel.initWithPaymentDetailsId(NON_DEFAULT_PAYMENT_DETAILS_ID)

        assertThat(viewModel.formController.value).isNotNull()
    }

    @Test
    fun `On initialization when payment details does not exist then return failure`() = runTest {
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(PaymentDetailsFixtures.CONSUMER_PAYMENT_DETAILS))

        val viewModel = createViewModel()
        viewModel.initWithPaymentDetailsId("UNKNOWN_ID")

        verify(navigator).setResult(any(), argWhere { it is CardEditViewModel.Result.Failure })
    }

    @Test
    fun `On initialization when failed fetching payment details then return failure`() = runTest {
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.failure(RuntimeException("Error")))

        val viewModel = createViewModel()
        viewModel.initWithPaymentDetailsId("any")

        verify(navigator).setResult(any(), argWhere { it is CardEditViewModel.Result.Failure })
    }

    @Test
    fun `updateCard makes api call with correct parameters when setting as default`() = runTest {
        val viewModel = createAndInitViewModel()
        viewModel.setAsDefault(true)
        clearInvocations(linkAccountManager)

        viewModel.updateCard(formValues)

        verify(linkAccountManager).updatePaymentDetails(
            argWhere {
                it is ConsumerPaymentDetailsUpdateParams.Card &&
                    it.toParamMap() == mapOf(
                    "is_default" to true,
                    "exp_month" to "12",
                    "exp_year" to "2040",
                    "billing_address" to mapOf(
                        "country_code" to "US",
                        "postal_code" to "12345"
                    )
                )
            }
        )
    }

    @Test
    fun `updateCard makes api call with correct parameters when not setting as default`() =
        runTest {
            val viewModel = createAndInitViewModel()
            clearInvocations(linkAccountManager)

            viewModel.updateCard(formValues)

            verify(linkAccountManager).updatePaymentDetails(
                argWhere {
                    it is ConsumerPaymentDetailsUpdateParams.Card &&
                        it.toParamMap() == mapOf(
                        "exp_month" to "12",
                        "exp_year" to "2040",
                        "billing_address" to mapOf(
                            "country_code" to "US",
                            "postal_code" to "12345"
                        )
                    )
                }
            )
        }

    @Test
    fun `updateCard makes api call with correct parameters when already is default`() = runTest {
        val viewModel = createAndInitViewModel(DEFAULT_PAYMENT_DETAILS_ID)
        clearInvocations(linkAccountManager)

        viewModel.updateCard(formValues)

        verify(linkAccountManager).updatePaymentDetails(
            argWhere {
                it is ConsumerPaymentDetailsUpdateParams.Card &&
                    it.toParamMap() == mapOf(
                    "exp_month" to "12",
                    "exp_year" to "2040",
                    "billing_address" to mapOf(
                        "country_code" to "US",
                        "postal_code" to "12345"
                    )
                )
            }
        )
    }

    @Test
    fun `dismiss navigates back`() = runTest {
        val viewModel = createAndInitViewModel()
        viewModel.dismiss()
        verify(navigator).setResult(
            eq(CardEditViewModel.Result.KEY),
            argWhere { it is CardEditViewModel.Result.Cancelled }
        )
        verify(navigator).onBack()
    }

    private fun createViewModel() =
        CardEditViewModel(
            linkAccount = linkAccount,
            linkAccountManager = linkAccountManager,
            navigator = navigator,
            logger = Logger.noop(),
            formControllerProvider = formControllerProvider,
            args = args
        )

    private suspend fun createAndInitViewModel(
        paymentDetailsId: String = NON_DEFAULT_PAYMENT_DETAILS_ID
    ): CardEditViewModel {
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(PaymentDetailsFixtures.CONSUMER_PAYMENT_DETAILS))

        val viewModel = createViewModel()
        viewModel.initWithPaymentDetailsId(paymentDetailsId)
        return viewModel
    }

    companion object {
        const val CLIENT_SECRET = "client_secret"
        const val NON_DEFAULT_PAYMENT_DETAILS_ID = "QAAAKIL"
        const val DEFAULT_PAYMENT_DETAILS_ID = "QAAAKJ6"
    }
}
